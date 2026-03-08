/**
 *    Copyright 2009-2017 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
 * Mapper 构建助手。
 * 在解析 mapper XML/注解期间负责拼装并注册缓存、参数映射、结果映射和语句定义，
 * 同时统一处理命名空间规则，确保各类配置 ID 的唯一性与可引用性。
 *
 * @author Clinton Begin
 */
public class MapperBuilderAssistant extends BaseBuilder {

  /**
   * 当前正在解析的 mapper 命名空间。
   */
  private String currentNamespace;
  /**
   * 当前资源标识（通常是 mapper 文件路径），用于错误上下文定位。
   */
  private final String resource;
  /**
   * 当前命名空间绑定的二级缓存实例。
   */
  private Cache currentCache;
  /**
   * 标记 cache-ref 是否仍未解析完成。
   * 为 true 时禁止创建 MappedStatement，避免语句绑定到错误缓存。
   */
  private boolean unresolvedCacheRef; // issue #676

  /**
   * 创建 Mapper 构建助手。
   *
   * @param configuration 全局配置
   * @param resource 当前解析资源标识
   */
  public MapperBuilderAssistant(Configuration configuration, String resource) {
    super(configuration);
    // 设置错误上下文资源，便于后续异常输出精确定位到当前 mapper 文件。
    ErrorContext.instance().resource(resource);
    this.resource = resource;
  }

  /**
   * 获取当前命名空间。
   *
   * @return 当前命名空间
   */
  public String getCurrentNamespace() {
    return currentNamespace;
  }

  /**
   * 设置当前命名空间。
   * 命名空间一旦确定后不可切换到其他值，以防同一资源解析过程发生串扰。
   *
   * @param currentNamespace 命名空间
   * @throws BuilderException 命名空间为空或与已设置值不一致时抛出
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
   * 将给定标识应用当前命名空间。
   *
   * @param base 原始标识
   * @param isReference 是否作为引用（引用允许使用跨命名空间全限定名）
   * @return 规范化后的标识
   * @throws BuilderException 非引用场景下包含点号时抛出
   */
  public String applyCurrentNamespace(String base, boolean isReference) {
    if (base == null) {
      return null;
    }
    if (isReference) {
      // 引用场景：若已是全限定名，直接返回。
      if (base.contains(".")) {
        return base;
      }
    } else {
      // 声明场景：若已带当前命名空间前缀，直接返回。
      if (base.startsWith(currentNamespace + ".")) {
        return base;
      }
      if (base.contains(".")) {
        // 声明 ID 不允许携带其他命名空间，避免污染当前 mapper 的 ID 空间。
        throw new BuilderException("Dots are not allowed in element names, please remove it from " + base);
      }
    }
    // 未限定时统一补齐当前命名空间前缀。
    return currentNamespace + "." + base;
  }

  /**
   * 引用其他命名空间的缓存定义（cache-ref）。
   *
   * @param namespace 被引用缓存所在命名空间
   * @return 引用到的缓存实例
   * @throws BuilderException 传入命名空间为空时抛出
   * @throws IncompleteElementException 目标缓存尚未解析时抛出
   */
  public Cache useCacheRef(String namespace) {
    if (namespace == null) {
      throw new BuilderException("cache-ref element requires a namespace attribute.");
    }
    try {
      // 先标记为未解析状态；解析成功后再恢复，防止中途失败造成脏状态。
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
   * 按当前命名空间创建并注册新缓存。
   *
   * @param typeClass 缓存实现类型
   * @param evictionClass 淘汰策略装饰器类型
   * @param flushInterval 刷新间隔（毫秒）
   * @param size 容量上限
   * @param readWrite 是否读写缓存（序列化）
   * @param blocking 是否启用阻塞缓存
   * @param props 额外属性
   * @return 创建后的缓存实例
   */
  public Cache useNewCache(Class<? extends Cache> typeClass,
      Class<? extends Cache> evictionClass,
      Long flushInterval,
      Integer size,
      boolean readWrite,
      boolean blocking,
      Properties props) {
    // 若调用方未指定实现/淘汰策略，则使用框架默认实现。
    Cache cache = new CacheBuilder(currentNamespace)
        .implementation(valueOrDefault(typeClass, PerpetualCache.class))
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
   * 创建并注册参数映射定义。
   *
   * @param id 参数映射 ID
   * @param parameterClass 参数对象类型
   * @param parameterMappings 参数字段映射集合
   * @return 构建后的参数映射
   */
  public ParameterMap addParameterMap(String id, Class<?> parameterClass, List<ParameterMapping> parameterMappings) {
    // 参数映射 ID 属于声明场景，必须归入当前命名空间。
    id = applyCurrentNamespace(id, false);
    ParameterMap parameterMap = new ParameterMap.Builder(configuration, id, parameterClass, parameterMappings).build();
    configuration.addParameterMap(parameterMap);
    return parameterMap;
  }

  /**
   * 构建单个参数字段映射。
   *
   * @param parameterType 参数对象类型
   * @param property 参数属性名
   * @param javaType Java 类型
   * @param jdbcType JDBC 类型
   * @param resultMap 结果映射引用 ID
   * @param parameterMode 参数模式
   * @param typeHandler 类型处理器类型
   * @param numericScale 数值精度
   * @return 参数字段映射
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
    // resultMap 作为引用处理，支持跨命名空间全限定 ID。
    resultMap = applyCurrentNamespace(resultMap, true);

    // Class parameterType = parameterMapBuilder.type();
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
   * 创建并注册结果映射定义，支持通过 extend 继承父结果映射。
   *
   * @param id 结果映射 ID
   * @param type 结果对象类型
   * @param extend 父结果映射 ID
   * @param discriminator 鉴别器定义
   * @param resultMappings 当前结果映射条目
   * @param autoMapping 是否启用自动映射
   * @return 构建后的结果映射
   * @throws IncompleteElementException 父结果映射不存在时抛出
   */
  public ResultMap addResultMap(
      String id,
      Class<?> type,
      String extend,
      Discriminator discriminator,
      List<ResultMapping> resultMappings,
      Boolean autoMapping) {
    // 当前定义 ID 必须归入本命名空间；extend 作为引用可跨命名空间。
    id = applyCurrentNamespace(id, false);
    extend = applyCurrentNamespace(extend, true);

    if (extend != null) {
      if (!configuration.hasResultMap(extend)) {
        throw new IncompleteElementException("Could not find a parent resultmap with id '" + extend + "'");
      }
      ResultMap resultMap = configuration.getResultMap(extend);
      List<ResultMapping> extendedResultMappings = new ArrayList<ResultMapping>(resultMap.getResultMappings());
      // 子映射优先：先剔除父映射中与子映射完全重合的条目。
      extendedResultMappings.removeAll(resultMappings);
      // Remove parent constructor if this resultMap declares a constructor.
      boolean declaresConstructor = false;
      for (ResultMapping resultMapping : resultMappings) {
        if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
          declaresConstructor = true;
          break;
        }
      }
      if (declaresConstructor) {
        Iterator<ResultMapping> extendedResultMappingsIter = extendedResultMappings.iterator();
        while (extendedResultMappingsIter.hasNext()) {
          // 子映射已声明构造器时，移除父映射构造器映射，避免构造参数冲突。
          if (extendedResultMappingsIter.next().getFlags().contains(ResultFlag.CONSTRUCTOR)) {
            extendedResultMappingsIter.remove();
          }
        }
      }
      // 将父映射剩余条目追加到子映射，形成最终结果映射集合。
      resultMappings.addAll(extendedResultMappings);
    }
    ResultMap resultMap = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping)
        .discriminator(discriminator)
        .build();
    configuration.addResultMap(resultMap);
    return resultMap;
  }

  /**
   * 构建鉴别器定义。
   *
   * @param resultType 结果类型
   * @param column 鉴别列
   * @param javaType Java 类型
   * @param jdbcType JDBC 类型
   * @param typeHandler 类型处理器类型
   * @param discriminatorMap 鉴别值到结果映射 ID 的映射
   * @return 鉴别器实例
   */
  public Discriminator buildDiscriminator(
      Class<?> resultType,
      String column,
      Class<?> javaType,
      JdbcType jdbcType,
      Class<? extends TypeHandler<?>> typeHandler,
      Map<String, String> discriminatorMap) {
    // 鉴别列本质是一个 ResultMapping。
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
        new ArrayList<ResultFlag>(),
        null,
        null,
        false);
    Map<String, String> namespaceDiscriminatorMap = new HashMap<String, String>();
    for (Map.Entry<String, String> e : discriminatorMap.entrySet()) {
      String resultMap = e.getValue();
      // 鉴别目标结果映射作为引用处理。
      resultMap = applyCurrentNamespace(resultMap, true);
      namespaceDiscriminatorMap.put(e.getKey(), resultMap);
    }
    return new Discriminator.Builder(configuration, resultMapping, namespaceDiscriminatorMap).build();
  }

  /**
   * 创建并注册 MappedStatement。
   *
   * @param id 语句 ID
   * @param sqlSource SQL 源
   * @param statementType Statement 类型
   * @param sqlCommandType SQL 命令类型
   * @param fetchSize 获取行数提示
   * @param timeout 超时时间
   * @param parameterMap 参数映射 ID
   * @param parameterType 参数类型
   * @param resultMap 结果映射 ID（支持逗号分隔）
   * @param resultType 结果类型
   * @param resultSetType ResultSet 类型
   * @param flushCache 是否刷新缓存
   * @param useCache 是否使用缓存
   * @param resultOrdered 是否结果有序
   * @param keyGenerator 主键生成器
   * @param keyProperty 主键属性
   * @param keyColumn 主键列
   * @param databaseId 数据库方言 ID
   * @param lang 脚本语言驱动
   * @param resultSets 多结果集名称
   * @return 构建后的 MappedStatement
   * @throws IncompleteElementException cache-ref 未解析时抛出
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
      // 若 cache-ref 仍未解析，禁止继续注册语句，避免缓存绑定错误。
      throw new IncompleteElementException("Cache-ref not yet resolved");
    }

    id = applyCurrentNamespace(id, false);
    boolean isSelect = sqlCommandType == SqlCommandType.SELECT;

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
        .flushCacheRequired(valueOrDefault(flushCache, !isSelect))
        .useCache(valueOrDefault(useCache, isSelect))
        .cache(currentCache);

    ParameterMap statementParameterMap = getStatementParameterMap(parameterMap, parameterType, id);
    if (statementParameterMap != null) {
      // 仅在解析得到参数映射时进行绑定。
      statementBuilder.parameterMap(statementParameterMap);
    }

    MappedStatement statement = statementBuilder.build();
    configuration.addMappedStatement(statement);
    return statement;
  }

  /**
   * 返回传入值或默认值。
   *
   * @param value 原始值
   * @param defaultValue 默认值
   * @param <T> 值类型
   * @return value 非空时返回 value，否则返回 defaultValue
   */
  private <T> T valueOrDefault(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  /**
   * 解析语句参数映射。
   * 优先按名称引用已有 ParameterMap；若未指定名称但指定了参数类型，则构建内联 ParameterMap。
   *
   * @param parameterMapName 参数映射名称
   * @param parameterTypeClass 参数类型
   * @param statementId 语句 ID
   * @return 解析后的参数映射，可能为 null
   * @throws IncompleteElementException 指定名称但找不到映射时抛出
   */
  private ParameterMap getStatementParameterMap(
      String parameterMapName,
      Class<?> parameterTypeClass,
      String statementId) {
    parameterMapName = applyCurrentNamespace(parameterMapName, true);
    ParameterMap parameterMap = null;
    if (parameterMapName != null) {
      try {
        parameterMap = configuration.getParameterMap(parameterMapName);
      } catch (IllegalArgumentException e) {
        throw new IncompleteElementException("Could not find parameter map " + parameterMapName, e);
      }
    } else if (parameterTypeClass != null) {
      // 未显式声明 parameterMap 时，基于参数类型创建空的内联 ParameterMap。
      List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
      parameterMap = new ParameterMap.Builder(
          configuration,
          statementId + "-Inline",
          parameterTypeClass,
          parameterMappings).build();
    }
    return parameterMap;
  }

  /**
   * 解析语句结果映射集合。
   * 支持逗号分隔的多个 resultMap；若未指定 resultMap 但指定 resultType，则构建内联 ResultMap。
   *
   * @param resultMap 结果映射名称（可多个）
   * @param resultType 结果类型
   * @param statementId 语句 ID
   * @return 结果映射列表
   * @throws IncompleteElementException 指定的结果映射不存在时抛出
   */
  private List<ResultMap> getStatementResultMaps(
      String resultMap,
      Class<?> resultType,
      String statementId) {
    resultMap = applyCurrentNamespace(resultMap, true);

    List<ResultMap> resultMaps = new ArrayList<ResultMap>();
    if (resultMap != null) {
      String[] resultMapNames = resultMap.split(",");
      for (String resultMapName : resultMapNames) {
        try {
          // 允许多个 resultMap，以逗号拆分逐个加载。
          resultMaps.add(configuration.getResultMap(resultMapName.trim()));
        } catch (IllegalArgumentException e) {
          throw new IncompleteElementException("Could not find result map " + resultMapName, e);
        }
      }
    } else if (resultType != null) {
      // 仅有 resultType 时构造空的内联 ResultMap，由自动映射补齐字段。
      ResultMap inlineResultMap = new ResultMap.Builder(
          configuration,
          statementId + "-Inline",
          resultType,
          new ArrayList<ResultMapping>(),
          null).build();
      resultMaps.add(inlineResultMap);
    }
    return resultMaps;
  }

  /**
   * 构建结果字段映射。
   *
   * @param resultType 结果对象类型
   * @param property 属性名
   * @param column 列名
   * @param javaType Java 类型
   * @param jdbcType JDBC 类型
   * @param nestedSelect 嵌套查询 ID
   * @param nestedResultMap 嵌套结果映射 ID
   * @param notNullColumn 非空触发列
   * @param columnPrefix 列前缀
   * @param typeHandler 类型处理器类型
   * @param flags 映射标记
   * @param resultSet 结果集名称
   * @param foreignColumn 外键列
   * @param lazy 是否延迟加载
   * @return 结果字段映射
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
    // 当未显式指定 javaType 时，尝试基于属性签名推断。
    Class<?> javaTypeClass = resolveResultJavaType(resultType, property, javaType);
    TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);
    // 复合列（如 {prop1=col1,prop2=col2}）会被拆分为多个子映射。
    List<ResultMapping> composites = parseCompositeColumnName(column);
    return new ResultMapping.Builder(configuration, property, column, javaTypeClass)
        .jdbcType(jdbcType)
        .nestedQueryId(applyCurrentNamespace(nestedSelect, true))
        .nestedResultMapId(applyCurrentNamespace(nestedResultMap, true))
        .resultSet(resultSet)
        .typeHandler(typeHandlerInstance)
        .flags(flags == null ? new ArrayList<ResultFlag>() : flags)
        .composites(composites)
        .notNullColumns(parseMultipleColumnNames(notNullColumn))
        .columnPrefix(columnPrefix)
        .foreignColumn(foreignColumn)
        .lazy(lazy)
        .build();
  }

  /**
   * 解析逗号分隔或大括号包裹的列名集合。
   *
   * @param columnName 列名配置
   * @return 列名集合；当入参为空时返回空集合
   */
  private Set<String> parseMultipleColumnNames(String columnName) {
    Set<String> columns = new HashSet<String>();
    if (columnName != null) {
      if (columnName.indexOf(',') > -1) {
        // 兼容 "{a,b}"、"a,b"、"a, b" 等多种写法。
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
   * 解析复合列配置（形如 "{prop1=col1,prop2=col2}"）。
   *
   * @param columnName 复合列配置字符串
   * @return 复合结果映射列表；非复合格式时返回空列表
   */
  private List<ResultMapping> parseCompositeColumnName(String columnName) {
    List<ResultMapping> composites = new ArrayList<ResultMapping>();
    if (columnName != null && (columnName.indexOf('=') > -1 || columnName.indexOf(',') > -1)) {
      StringTokenizer parser = new StringTokenizer(columnName, "{}=, ", false);
      while (parser.hasMoreTokens()) {
        String property = parser.nextToken();
        String column = parser.nextToken();
        // 复合列子项使用 UnknownTypeHandler，后续执行阶段再按实际值解析类型。
        ResultMapping complexResultMapping = new ResultMapping.Builder(
            configuration, property, column, configuration.getTypeHandlerRegistry().getUnknownTypeHandler()).build();
        composites.add(complexResultMapping);
      }
    }
    return composites;
  }

  /**
   * 解析结果映射的 Java 类型。
   * 优先使用显式 javaType；否则基于 resultType 的 setter 参数推断；最终兜底为 Object。
   *
   * @param resultType 结果对象类型
   * @param property 属性名
   * @param javaType 显式 Java 类型
   * @return 解析后的 Java 类型
   */
  private Class<?> resolveResultJavaType(Class<?> resultType, String property, Class<?> javaType) {
    if (javaType == null && property != null) {
      try {
        MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
        javaType = metaResultType.getSetterType(property);
      } catch (Exception e) {
        // 忽略反射解析异常，后续统一走 Object.class 兜底。
      }
    }
    if (javaType == null) {
      javaType = Object.class;
    }
    return javaType;
  }

  /**
   * 解析参数映射的 Java 类型。
   * 优先使用显式 javaType；其次按 JDBC 类型和参数对象元信息推断；最终兜底为 Object。
   *
   * @param resultType 参数对象类型
   * @param property 属性名
   * @param javaType 显式 Java 类型
   * @param jdbcType JDBC 类型
   * @return 解析后的 Java 类型
   */
  private Class<?> resolveParameterJavaType(Class<?> resultType, String property, Class<?> javaType, JdbcType jdbcType) {
    if (javaType == null) {
      if (JdbcType.CURSOR.equals(jdbcType)) {
        // CURSOR 参数固定映射为 ResultSet。
        javaType = java.sql.ResultSet.class;
      } else if (Map.class.isAssignableFrom(resultType)) {
        // Map 参数无法静态推断属性类型，统一按 Object 处理。
        javaType = Object.class;
      } else {
        MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
        javaType = metaResultType.getGetterType(property);
      }
    }
    if (javaType == null) {
      javaType = Object.class;
    }
    return javaType;
  }

  /**
   * 兼容旧版本签名的结果映射构建方法。
   * 新增参数使用默认值：foreignColumn/resultSet 为空，lazy 使用全局懒加载配置。
   *
   * @param resultType 结果对象类型
   * @param property 属性名
   * @param column 列名
   * @param javaType Java 类型
   * @param jdbcType JDBC 类型
   * @param nestedSelect 嵌套查询 ID
   * @param nestedResultMap 嵌套结果映射 ID
   * @param notNullColumn 非空触发列
   * @param columnPrefix 列前缀
   * @param typeHandler 类型处理器类型
   * @param flags 映射标记
   * @return 结果字段映射
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
      List<ResultFlag> flags) {
      return buildResultMapping(
        resultType, property, column, javaType, jdbcType, nestedSelect,
        nestedResultMap, notNullColumn, columnPrefix, typeHandler, flags, null, null, configuration.isLazyLoadingEnabled());
  }

  /**
   * 获取脚本语言驱动实例。
   * 传入具体驱动类时会先注册，再返回对应驱动；未传入时返回默认驱动。
   *
   * @param langClass 语言驱动类，可为 null
   * @return 语言驱动实例
   */
  public LanguageDriver getLanguageDriver(Class<?> langClass) {
    if (langClass != null) {
      configuration.getLanguageRegistry().register(langClass);
    } else {
      langClass = configuration.getLanguageRegistry().getDefaultDriverClass();
    }
    return configuration.getLanguageRegistry().getDriver(langClass);
  }

  /**
   * 兼容旧版本签名的语句构建方法。
   * 该签名不支持 resultSets 参数，内部转调新签名并传入 null。
   *
   * @param id 语句 ID
   * @param sqlSource SQL 源
   * @param statementType Statement 类型
   * @param sqlCommandType SQL 命令类型
   * @param fetchSize 获取行数提示
   * @param timeout 超时时间
   * @param parameterMap 参数映射 ID
   * @param parameterType 参数类型
   * @param resultMap 结果映射 ID
   * @param resultType 结果类型
   * @param resultSetType ResultSet 类型
   * @param flushCache 是否刷新缓存
   * @param useCache 是否使用缓存
   * @param resultOrdered 是否结果有序
   * @param keyGenerator 主键生成器
   * @param keyProperty 主键属性
   * @param keyColumn 主键列
   * @param databaseId 数据库方言 ID
   * @param lang 脚本语言驱动
   * @return 构建后的 MappedStatement
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
    LanguageDriver lang) {
    return addMappedStatement(
      id, sqlSource, statementType, sqlCommandType, fetchSize, timeout,
      parameterMap, parameterType, resultMap, resultType, resultSetType,
      flushCache, useCache, resultOrdered, keyGenerator, keyProperty,
      keyColumn, databaseId, lang, null);
  }

}
