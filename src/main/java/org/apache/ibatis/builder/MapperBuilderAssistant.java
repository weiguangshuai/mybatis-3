/*
 *    Copyright 2009-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.CacheBuilder;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * Mapper构建助手类，负责解析和构建Mapper中的SQL语句、结果映射、缓存等配置。
 *
 * @author Clinton Begin
 */
public class MapperBuilderAssistant extends BaseBuilder {

  /** 当前Mapper的命名空间 */
  private String currentNamespace;
  /** 映射文件资源路径，用于错误定位 */
  private final String resource;
  /** 当前Mapper对应的二级缓存 */
  private Cache currentCache;
  /** 是否存在未解析的缓存引用，用于处理循环依赖问题 */
  private boolean unresolvedCacheRef; // issue #676

  /**
   * 构造MapperBuilderAssistant。
   *
   * @param configuration MyBatis全局配置对象
   * @param resource 映射文件资源路径
   */
  public MapperBuilderAssistant(Configuration configuration, String resource) {
    super(configuration);
    ErrorContext.instance().resource(resource);
    this.resource = resource;
  }

  /**
   * 获取当前Mapper的命名空间。
   *
   * @return 当前命名空间
   */
  public String getCurrentNamespace() {
    return currentNamespace;
  }

  /**
   * 设置当前Mapper的命名空间，并进行有效性校验。
   *
   * @param currentNamespace 命名空间值
   * @throws BuilderException 如果命名空间为空或与已有命名空间不匹配
   */
  public void setCurrentNamespace(String currentNamespace) {
    if (currentNamespace == null) {
      throw new BuilderException("The mapper element requires a namespace attribute to be specified.");
    }

    if (this.currentNamespace != null && !this.currentNamespace.equals(currentNamespace)) {
      throw new BuilderException("Wrong namespace. Expected '"
          + this.currentNamespace + "' but found '" + currentNamespace + "'.");
    }

    this.currentNamespace = currentNamespace;
  }

  /**
   * 为指定名称应用当前命名空间前缀。
   *
   * @param base 基础名称
   * @param isReference 是否为引用类型（引用其他命名空间的元素）
   * @return 带有命名空间前缀的名称
   * @throws BuilderException 名称中包含非法点号
   */
  public String applyCurrentNamespace(String base, boolean isReference) {
    if (base == null) {
      return null;
    }
    if (isReference) {
      // 引用类型：检查是否已包含命名空间前缀
      if (base.contains(".")) {
        return base;
      }
    } else {
      // 非引用类型：检查是否已应用当前命名空间
      if (base.startsWith(currentNamespace + ".")) {
        return base;
      }
      // 非引用类型不允许使用点号
      if (base.contains(".")) {
        throw new BuilderException("Dots are not allowed in element names, please remove it from " + base);
      }
    }
    return currentNamespace + "." + base;
  }

  /**
   * 引用指定命名空间的缓存。
   *
   * @param namespace 要引用的缓存所属Mapper的命名空间
   * @return 被引用的缓存对象
   * @throws BuilderException 命名空间为空或缓存不存在
   */
  public Cache useCacheRef(String namespace) {
    if (namespace == null) {
      throw new BuilderException("cache-ref element requires a namespace attribute.");
    }
    try {
      unresolvedCacheRef = true;
      Cache cache = configuration.getCache(namespace);
      if (cache == null) {
        throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
      }
      currentCache = cache;
      unresolvedCacheRef = false;
      return cache;
    } catch (IllegalArgumentException e) {
      throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.", e);
    }
  }

  /**
   * 创建新的二级缓存并设置为当前缓存。
   *
   * @param typeCache 缓存实现类
   * @param evictionClass 缓存淘汰策略类
   * @param flushInterval 缓存刷新间隔（毫秒）
   * @param size 缓存最大容量
   * @param readWrite 是否支持读写
   * @param blocking 是否使用阻塞缓存
   * @param props 缓存额外配置属性
   * @return 新创建的缓存对象
   */
  public Cache useNewCache(Class<? extends Cache> typeCache,
      Class<? extends Cache> evictionClass,
      Long flushInterval,
      Integer size,
      boolean readWrite,
      boolean blocking,
      Properties props) {
    Cache cache = new CacheBuilder(currentNamespace)
        .implementation(valueOrDefault(typeCache, PerpetualCache.class))
        .addDecorator(valueOrDefault(evictionClass, LruCache.class))
        .clearInterval(flushInterval)
        .size(size)
        .readWrite(readWrite)
        .blocking(blocking)
        .properties(props)
        .build();
    configuration.addCache(cache);
    currentCache = cache;
    return cache;
  }

  /**
   * 添加参数映射集合。
   *
   * @param id 参数映射ID
   * @param parameterClass 参数类型
   * @param parameterMappings 参数映射列表
   * @return 构建好的ParameterMap对象
   */
  public ParameterMap addParameterMap(String id, Class<?> parameterClass, List<ParameterMapping> parameterMappings) {
    id = applyCurrentNamespace(id, false);
    ParameterMap parameterMap = new ParameterMap.Builder(configuration, id, parameterClass, parameterMappings).build();
    configuration.addParameterMap(parameterMap);
    return parameterMap;
  }

  /**
   * 构建参数映射对象。
   *
   * @param parameterType 参数类型
   * @param property 参数属性名
   * @param javaType Java类型
   * @param jdbcType JDBC类型
   * @param resultMap 引用结果映射ID
   * @param parameterMode 参数模式（IN/OUT/INOUT）
   * @param typeHandler 类型处理器
   * @param numericScale 数值精度
   * @return 构建好的ParameterMapping对象
   */
  public ParameterMapping buildParameterMapping(
      Class<?> parameterType,
      String property,
      Class<?> javaType,
      JdbcType jdbcType,
      String resultMap,
      ParameterMode parameterMode,
      Class<? extends TypeHandler<?>> typeHandler,
      Integer numericScale) {
    resultMap = applyCurrentNamespace(resultMap, true);

    // 解析Java类型，若未指定则根据属性推断
    Class<?> javaTypeClass = resolveParameterJavaType(parameterType, property, javaType, jdbcType);
    TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);

    return new ParameterMapping.Builder(configuration, property, javaTypeClass)
        .jdbcType(jdbcType)
        .resultMapId(resultMap)
        .mode(parameterMode)
        .numericScale(numericScale)
        .typeHandler(typeHandlerInstance)
        .build();
  }

  /**
   * 添加结果映射集合。
   *
   * @param id 结果映射ID
   * @param type 实体类型
   * @param extend 父结果映射ID
   * @param discriminator 鉴别器
   * @param resultMappings 结果映射列表
   * @param autoMapping 是否自动映射
   * @return 构建好的ResultMap对象
   * @throws IncompleteElementException 父结果映射不存在
   */
  public ResultMap addResultMap(
      String id,
      Class<?> type,
      String extend,
      Discriminator discriminator,
      List<ResultMapping> resultMappings,
      Boolean autoMapping) {
    id = applyCurrentNamespace(id, false);
    extend = applyCurrentNamespace(extend, true);

    // 处理继承：合并父ResultMap的映射
    if (extend != null) {
      if (!configuration.hasResultMap(extend)) {
        throw new IncompleteElementException("Could not find a parent resultmap with id '" + extend + "'");
      }
      ResultMap resultMap = configuration.getResultMap(extend);
      List<ResultMapping> extendedResultMappings = new ArrayList<>(resultMap.getResultMappings());
      extendedResultMappings.removeAll(resultMappings);
      // 若当前ResultMap声明了构造函数，则移除父类的构造函数映射
      boolean declaresConstructor = false;
      for (ResultMapping resultMapping : resultMappings) {
        if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
          declaresConstructor = true;
          break;
        }
      }
      if (declaresConstructor) {
        extendedResultMappings.removeIf(resultMapping -> resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR));
      }
      resultMappings.addAll(extendedResultMappings);
    }
    ResultMap resultMap = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping)
        .discriminator(discriminator)
        .build();
    configuration.addResultMap(resultMap);
    return resultMap;
  }

  /**
   * 构建鉴别器对象。
   *
   * @param resultType 实体类型
   * @param column 鉴别器列名
   * @param javaType Java类型
   * @param jdbcType JDBC类型
   * @param typeHandler 类型处理器
   * @param discriminatorMap 鉴别器值到结果映射的映射
   * @return 构建好的Discriminator对象
   */
  public Discriminator buildDiscriminator(
      Class<?> resultType,
      String column,
      Class<?> javaType,
      JdbcType jdbcType,
      Class<? extends TypeHandler<?>> typeHandler,
      Map<String, String> discriminatorMap) {
    ResultMapping resultMapping = buildResultMapping(
        resultType,
        null,
        column,
        javaType,
        jdbcType,
        null,
        null,
        null,
        null,
        typeHandler,
        new ArrayList<>(),
        null,
        null,
        false);
    // 为鉴别器映射中的应用命名空间
    Map<String, String> namespaceDiscriminatorMap = new HashMap<>();
    for (Map.Entry<String, String> e : discriminatorMap.entrySet()) {
      String resultMap = e.getValue();
      resultMap = applyCurrentNamespace(resultMap, true);
      namespaceDiscriminatorMap.put(e.getKey(), resultMap);
    }
    return new Discriminator.Builder(configuration, resultMapping, namespaceDiscriminatorMap).build();
  }

  /**
   * 添加MappedStatement（SQL语句映射）。
   *
   * @param id 语句ID
   * @param sqlSource SQL来源
   * @param statementType 语句类型（PREPARED/STATEMENT/CALLABLE）
   * @param sqlCommandType SQL命令类型（SELECT/INSERT/UPDATE/DELETE）
   * @param fetchSize 抓取数量
   * @param timeout 超时时间
   * @param parameterMap 参数映射ID
   * @param parameterType 参数类型
   * @param resultMap 结果映射ID
   * @param resultType 结果类型
   * @param resultSetType 结果集类型
   * @param flushCache 是否刷新缓存
   * @param useCache 是否使用缓存
   * @param resultOrdered 结果是否有序
   * @param keyGenerator 主键生成器
   * @param keyProperty 主键属性
   * @param keyColumn 主键列
   * @param databaseId 数据库ID
   * @param lang 语言驱动
   * @param resultSets 结果集
   * @return 构建好的MappedStatement对象
   * @throws IncompleteElementException 存在未解析的缓存引用
   */
  public MappedStatement addMappedStatement(
      String id,
      SqlSource sqlSource,
      StatementType statementType,
      SqlCommandType sqlCommandType,
      Integer fetchSize,
      Integer timeout,
      String parameterMap,
      Class<?> parameterType,
      String resultMap,
      Class<?> resultType,
      ResultSetType resultSetType,
      boolean flushCache,
      boolean useCache,
      boolean resultOrdered,
      KeyGenerator keyGenerator,
      String keyProperty,
      String keyColumn,
      String databaseId,
      LanguageDriver lang,
      String resultSets) {

    if (unresolvedCacheRef) {
      throw new IncompleteElementException("Cache-ref not yet resolved");
    }

    id = applyCurrentNamespace(id, false);

    MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType)
        .resource(resource)
        .fetchSize(fetchSize)
        .timeout(timeout)
        .statementType(statementType)
        .keyGenerator(keyGenerator)
        .keyProperty(keyProperty)
        .keyColumn(keyColumn)
        .databaseId(databaseId)
        .lang(lang)
        .resultOrdered(resultOrdered)
        .resultSets(resultSets)
        .resultMaps(getStatementResultMaps(resultMap, resultType, id))
        .resultSetType(resultSetType)
        .flushCacheRequired(flushCache)
        .useCache(useCache)
        .cache(currentCache);

    // 处理参数映射
    ParameterMap statementParameterMap = getStatementParameterMap(parameterMap, parameterType, id);
    if (statementParameterMap != null) {
      statementBuilder.parameterMap(statementParameterMap);
    }

    MappedStatement statement = statementBuilder.build();
    configuration.addMappedStatement(statement);
    return statement;
  }

  /**
   * Backward compatibility signature 'addMappedStatement'.
   *
   * @param id
   *          the id
   * @param sqlSource
   *          the sql source
   * @param statementType
   *          the statement type
   * @param sqlCommandType
   *          the sql command type
   * @param fetchSize
   *          the fetch size
   * @param timeout
   *          the timeout
   * @param parameterMap
   *          the parameter map
   * @param parameterType
   *          the parameter type
   * @param resultMap
   *          the result map
   * @param resultType
   *          the result type
   * @param resultSetType
   *          the result set type
   * @param flushCache
   *          the flush cache
   * @param useCache
   *          the use cache
   * @param resultOrdered
   *          the result ordered
   * @param keyGenerator
   *          the key generator
   * @param keyProperty
   *          the key property
   * @param keyColumn
   *          the key column
   * @param databaseId
   *          the database id
   * @param lang
   *          the lang
   * @return the mapped statement
   */
  public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
      SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap, Class<?> parameterType,
      String resultMap, Class<?> resultType, ResultSetType resultSetType, boolean flushCache, boolean useCache,
      boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId,
      LanguageDriver lang) {
    return addMappedStatement(
      id, sqlSource, statementType, sqlCommandType, fetchSize, timeout,
      parameterMap, parameterType, resultMap, resultType, resultSetType,
      flushCache, useCache, resultOrdered, keyGenerator, keyProperty,
      keyColumn, databaseId, lang, null);
  }

  /**
   * 获取值，若为空则返回默认值。
   *
   * @param value 原值
   * @param defaultValue 默认值
   * @param <T> 类型参数
   * @return 值或默认值
   */
  private <T> T valueOrDefault(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  /**
   * 获取语句参数映射，不存在时创建内联参数映射。
   *
   * @param parameterMapName 参数映射名称
   * @param parameterTypeClass 参数类型
   * @param statementId 语句ID
   * @return ParameterMap对象
   * @throws IncompleteElementException 指定的参数映射不存在
   */
  private ParameterMap getStatementParameterMap(
      String parameterMapName,
      Class<?> parameterTypeClass,
      String statementId) {
    parameterMapName = applyCurrentNamespace(parameterMapName, true);
    ParameterMap parameterMap = null;
    if (parameterMapName != null) {
      // 使用已定义的参数映射
      try {
        parameterMap = configuration.getParameterMap(parameterMapName);
      } catch (IllegalArgumentException e) {
        throw new IncompleteElementException("Could not find parameter map " + parameterMapName, e);
      }
    } else if (parameterTypeClass != null) {
      // 创建内联参数映射
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      parameterMap = new ParameterMap.Builder(
          configuration,
          statementId + "-Inline",
          parameterTypeClass,
          parameterMappings).build();
    }
    return parameterMap;
  }

  /**
   * 获取语句的结果映射列表，不存在时创建内联结果映射。
   *
   * @param resultMap 结果映射ID（多个用逗号分隔）
   * @param resultType 结果类型
   * @param statementId 语句ID
   * @return ResultMap列表
   * @throws IncompleteElementException 指定的结果映射不存在
   */
  private List<ResultMap> getStatementResultMaps(
      String resultMap,
      Class<?> resultType,
      String statementId) {
    resultMap = applyCurrentNamespace(resultMap, true);

    List<ResultMap> resultMaps = new ArrayList<>();
    if (resultMap != null) {
      // 解析多个结果映射（逗号分隔）
      String[] resultMapNames = resultMap.split(",");
      for (String resultMapName : resultMapNames) {
        try {
          resultMaps.add(configuration.getResultMap(resultMapName.trim()));
        } catch (IllegalArgumentException e) {
          throw new IncompleteElementException("Could not find result map '" + resultMapName + "' referenced from '" + statementId + "'", e);
        }
      }
    } else if (resultType != null) {
      // 创建内联结果映射
      ResultMap inlineResultMap = new ResultMap.Builder(
          configuration,
          statementId + "-Inline",
          resultType,
          new ArrayList<>(),
          null).build();
      resultMaps.add(inlineResultMap);
    }
    return resultMaps;
  }

  /**
   * 构建结果映射对象。
   *
   * @param resultType 实体类型
   * @param property 属性名
   * @param column 列名
   * @param javaType Java类型
   * @param jdbcType JDBC类型
   * @param nestedSelect 嵌套查询SQL
   * @param nestedResultMap 嵌套结果映射ID
   * @param notNullColumn 非空列（多个用逗号分隔）
   * @param columnPrefix 列前缀
   * @param typeHandler 类型处理器
   * @param flags 标志列表
   * @param resultSet 结果集名称
   * @param foreignColumn 外键列
   * @param lazy 是否延迟加载
   * @return 构建好的ResultMapping对象
   */
  public ResultMapping buildResultMapping(
      Class<?> resultType,
      String property,
      String column,
      Class<?> javaType,
      JdbcType jdbcType,
      String nestedSelect,
      String nestedResultMap,
      String notNullColumn,
      String columnPrefix,
      Class<? extends TypeHandler<?>> typeHandler,
      List<ResultFlag> flags,
      String resultSet,
      String foreignColumn,
      boolean lazy) {
    Class<?> javaTypeClass = resolveResultJavaType(resultType, property, javaType);
    TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);
    // 处理复合列名（嵌套查询或外键列时需要）
    List<ResultMapping> composites;
    if ((nestedSelect == null || nestedSelect.isEmpty()) && (foreignColumn == null || foreignColumn.isEmpty())) {
      composites = Collections.emptyList();
    } else {
      composites = parseCompositeColumnName(column);
    }
    return new ResultMapping.Builder(configuration, property, column, javaTypeClass)
        .jdbcType(jdbcType)
        .nestedQueryId(applyCurrentNamespace(nestedSelect, true))
        .nestedResultMapId(applyCurrentNamespace(nestedResultMap, true))
        .resultSet(resultSet)
        .typeHandler(typeHandlerInstance)
        .flags(flags == null ? new ArrayList<>() : flags)
        .composites(composites)
        .notNullColumns(parseMultipleColumnNames(notNullColumn))
        .columnPrefix(columnPrefix)
        .foreignColumn(foreignColumn)
        .lazy(lazy)
        .build();
  }

  /**
   * Backward compatibility signature 'buildResultMapping'.
   *
   * @param resultType
   *          the result type
   * @param property
   *          the property
   * @param column
   *          the column
   * @param javaType
   *          the java type
   * @param jdbcType
   *          the jdbc type
   * @param nestedSelect
   *          the nested select
   * @param nestedResultMap
   *          the nested result map
   * @param notNullColumn
   *          the not null column
   * @param columnPrefix
   *          the column prefix
   * @param typeHandler
   *          the type handler
   * @param flags
   *          the flags
   * @return the result mapping
   */
  public ResultMapping buildResultMapping(Class<?> resultType, String property, String column, Class<?> javaType,
      JdbcType jdbcType, String nestedSelect, String nestedResultMap, String notNullColumn, String columnPrefix,
      Class<? extends TypeHandler<?>> typeHandler, List<ResultFlag> flags) {
    return buildResultMapping(
      resultType, property, column, javaType, jdbcType, nestedSelect,
      nestedResultMap, notNullColumn, columnPrefix, typeHandler, flags, null, null, configuration.isLazyLoadingEnabled());
  }

  /**
   * Gets the language driver.
   *
   * @param langClass
   *          the lang class
   * @return the language driver
   * @deprecated Use {@link Configuration#getLanguageDriver(Class)}
   */
  @Deprecated
  public LanguageDriver getLanguageDriver(Class<? extends LanguageDriver> langClass) {
    return configuration.getLanguageDriver(langClass);
  }

  /**
   * 解析多个列名（逗号分隔）。
   *
   * @param columnName 列名（可能包含逗号、空格、大括号）
   * @return 列名集合
   */
  private Set<String> parseMultipleColumnNames(String columnName) {
    Set<String> columns = new HashSet<>();
    if (columnName != null) {
      if (columnName.indexOf(',') > -1) {
        StringTokenizer parser = new StringTokenizer(columnName, "{}, ", false);
        while (parser.hasMoreTokens()) {
          String column = parser.nextToken();
          columns.add(column);
        }
      } else {
        columns.add(columnName);
      }
    }
    return columns;
  }

  /**
   * 解析复合列名（属性=列名 格式）。
   *
   * @param columnName 复合列名（如 "property=column" 或 "property,column"）
   * @return 复合ResultMapping列表
   */
  private List<ResultMapping> parseCompositeColumnName(String columnName) {
    List<ResultMapping> composites = new ArrayList<>();
    if (columnName != null && (columnName.indexOf('=') > -1 || columnName.indexOf(',') > -1)) {
      StringTokenizer parser = new StringTokenizer(columnName, "{}=, ", false);
      while (parser.hasMoreTokens()) {
        String property = parser.nextToken();
        String column = parser.nextToken();
        ResultMapping complexResultMapping = new ResultMapping.Builder(
            configuration, property, column, configuration.getTypeHandlerRegistry().getUnknownTypeHandler()).build();
        composites.add(complexResultMapping);
      }
    }
    return composites;
  }

  /**
   * 解析结果映射的Java类型，未指定时从实体类属性推断。
   *
   * @param resultType 实体类型
   * @param property 属性名
   * @param javaType 指定的Java类型
   * @return 解析后的Java类型
   */
  private Class<?> resolveResultJavaType(Class<?> resultType, String property, Class<?> javaType) {
    if (javaType == null && property != null) {
      try {
        // 通过反射获取setter方法参数类型
        MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
        javaType = metaResultType.getSetterType(property);
      } catch (Exception e) {
        // 忽略异常，后续null检查会处理
      }
    }
    if (javaType == null) {
      javaType = Object.class;
    }
    return javaType;
  }

  /**
   * 解析参数映射的Java类型，未指定时根据参数类型和属性推断。
   *
   * @param resultType 参数类型
   * @param property 属性名
   * @param javaType 指定的Java类型
   * @param jdbcType JDBC类型
   * @return 解析后的Java类型
   */
  private Class<?> resolveParameterJavaType(Class<?> resultType, String property, Class<?> javaType, JdbcType jdbcType) {
    if (javaType == null) {
      if (JdbcType.CURSOR.equals(jdbcType)) {
        // 游标类型返回ResultSet
        javaType = java.sql.ResultSet.class;
      } else if (Map.class.isAssignableFrom(resultType)) {
        // Map类型默认使用Object
        javaType = Object.class;
      } else {
        // 通过反射获取getter方法返回类型
        MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
        javaType = metaResultType.getGetterType(property);
      }
    }
    if (javaType == null) {
      javaType = Object.class;
    }
    return javaType;
  }

}
