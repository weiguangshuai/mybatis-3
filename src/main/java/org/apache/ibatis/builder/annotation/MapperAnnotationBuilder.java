/*
 *    Copyright 2009-2022 the original author or authors.
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
package org.apache.ibatis.builder.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.CacheNamespaceRef;
import org.apache.ibatis.annotations.Case;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Options.FlushCachePolicy;
import org.apache.ibatis.annotations.Property;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.TypeDiscriminator;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.FetchType;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.UnknownTypeHandler;

/**
 * 基于注解的 Mapper 接口解析器，负责将 Mapper 接口中的注解转换为 MyBatis 运行时映射配置。
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class MapperAnnotationBuilder {

  /** 语句注解类型集合，包含 Select、Update、Insert、Delete 及其 Provider 注解 */
  private static final Set<Class<? extends Annotation>> statementAnnotationTypes = Stream
      .of(Select.class, Update.class, Insert.class, Delete.class, SelectProvider.class, UpdateProvider.class,
          InsertProvider.class, DeleteProvider.class)
      .collect(Collectors.toSet());

  /** MyBatis 全局配置对象 */
  private final Configuration configuration;
  /** Mapper 构建助手，提供构建映射的辅助方法 */
  private final MapperBuilderAssistant assistant;
  /** Mapper 接口类型 */
  private final Class<?> type;

  /**
   * 构造 Mapper 注解解析器。
   *
   * @param configuration MyBatis 全局配置
   * @param type Mapper 接口类型
   */
  public MapperAnnotationBuilder(Configuration configuration, Class<?> type) {
    String resource = type.getName().replace('.', '/') + ".java (best guess)";
    this.assistant = new MapperBuilderAssistant(configuration, resource);
    this.configuration = configuration;
    this.type = type;
  }

  /**
   * 解析 Mapper 接口中的注解，构建映射配置。
   * 解析缓存注解、缓存引用、ResultMap 以及 SQL 语句注解。
   */
  public void parse() {
    String resource = type.toString();
    // 避免重复解析同一 Mapper 接口
    if (!configuration.isResourceLoaded(resource)) {
      // 尝试加载同名的 XML mapper 文件
      loadXmlResource();
      configuration.addLoadedResource(resource);
      assistant.setCurrentNamespace(type.getName());
      // 解析 @CacheNamespace 注解
      parseCache();
      // 解析 @CacheNamespaceRef 注解
      parseCacheRef();
      // 遍历所有方法，解析语句注解
      for (Method method : type.getMethods()) {
        if (!canHaveStatement(method)) {
          continue;
        }
        // 对于 Select 查询，若未指定 ResultMap 则自动生成
        if (getAnnotationWrapper(method, false, Select.class, SelectProvider.class).isPresent()
            && method.getAnnotation(ResultMap.class) == null) {
          parseResultMap(method);
        }
        try {
          parseStatement(method);
        } catch (IncompleteElementException e) {
          // 解析失败时加入待处理队列
          configuration.addIncompleteMethod(new MethodResolver(this, method));
        }
      }
    }
    // 处理之前因依赖未解析而延迟的方法
    parsePendingMethods();
  }

  /**
   * 判断方法是否可以包含 SQL 语句注解。
   * 排除桥接方法（泛型擦除产生）和接口默认方法。
   *
   * @param method 待检查的方法
   * @return 若可包含语句注解返回 true
   */
  private boolean canHaveStatement(Method method) {
    // issue #237: 排除桥接方法和默认方法
    return !method.isBridge() && !method.isDefault();
  }

  /**
   * 处理之前因依赖未解析而延迟的方法。
   * 尝试重新解析未完成的 method，解析成功后从待处理集合中移除。
   */
  private void parsePendingMethods() {
    Collection<MethodResolver> incompleteMethods = configuration.getIncompleteMethods();
    synchronized (incompleteMethods) {
      Iterator<MethodResolver> iter = incompleteMethods.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolve();
          iter.remove();
        } catch (IncompleteElementException e) {
          // 仍缺少必要资源，留待后续处理
        }
      }
    }
  }

  /**
   * 尝试加载与 Mapper 接口同名的 XML 映射文件。
   * XML 配置优先级高于注解，若存在 XML 则以 XML 为准。
   */
  private void loadXmlResource() {
    // 防止 Spring 环境下重复加载，通过 namespace 标识检查
    if (!configuration.isResourceLoaded("namespace:" + type.getName())) {
      String xmlResource = type.getName().replace('.', '/') + ".xml";
      // 先尝试从当前模块资源路径加载
      InputStream inputStream = type.getResourceAsStream("/" + xmlResource);
      if (inputStream == null) {
        // 从类路径根目录搜索 XML 文件
        try {
          inputStream = Resources.getResourceAsStream(type.getClassLoader(), xmlResource);
        } catch (IOException e2) {
          // XML 映射文件为可选资源
        }
      }
      // 存在 XML 文件则解析
      if (inputStream != null) {
        XMLMapperBuilder xmlParser = new XMLMapperBuilder(inputStream, assistant.getConfiguration(), xmlResource, configuration.getSqlFragments(), type.getName());
        xmlParser.parse();
      }
    }
  }

  /**
   * 解析 @CacheNamespace 注解，配置二级缓存。
   */
  private void parseCache() {
    CacheNamespace cacheDomain = type.getAnnotation(CacheNamespace.class);
    if (cacheDomain != null) {
      Integer size = cacheDomain.size() == 0 ? null : cacheDomain.size();
      Long flushInterval = cacheDomain.flushInterval() == 0 ? null : cacheDomain.flushInterval();
      Properties props = convertToProperties(cacheDomain.properties());
      assistant.useNewCache(cacheDomain.implementation(), cacheDomain.eviction(), flushInterval, size, cacheDomain.readWrite(), cacheDomain.blocking(), props);
    }
  }

  /**
   * 将注解中的属性数组转换为 Properties 对象。
   *
   * @param properties 缓存属性数组
   * @return 转换后的 Properties，若无属性则返回 null
   */
  private Properties convertToProperties(Property[] properties) {
    if (properties.length == 0) {
      return null;
    }
    Properties props = new Properties();
    for (Property property : properties) {
      // 支持占位符解析
      props.setProperty(property.name(),
          PropertyParser.parse(property.value(), configuration.getVariables()));
    }
    return props;
  }

  /**
   * 解析 @CacheNamespaceRef 注解，引用其他命名空间的缓存。
   */
  private void parseCacheRef() {
    CacheNamespaceRef cacheDomainRef = type.getAnnotation(CacheNamespaceRef.class);
    if (cacheDomainRef != null) {
      Class<?> refType = cacheDomainRef.value();
      String refName = cacheDomainRef.name();
      // value 和 name 必须且只能指定其一
      if (refType == void.class && refName.isEmpty()) {
        throw new BuilderException("Should be specified either value() or name() attribute in the @CacheNamespaceRef");
      }
      if (refType != void.class && !refName.isEmpty()) {
        throw new BuilderException("Cannot use both value() and name() attribute in the @CacheNamespaceRef");
      }
      String namespace = (refType != void.class) ? refType.getName() : refName;
      try {
        assistant.useCacheRef(namespace);
      } catch (IncompleteElementException e) {
        // 被引用的缓存尚未解析完成，加入待处理队列
        configuration.addIncompleteCacheRef(new CacheRefResolver(assistant, namespace));
      }
    }
  }

  /**
   * 解析方法上的 Result 注解，生成 ResultMap。
   *
   * @param method Mapper 方法
   * @return 生成的 ResultMap ID
   */
  private String parseResultMap(Method method) {
    Class<?> returnType = getReturnType(method);
    Arg[] args = method.getAnnotationsByType(Arg.class);
    Result[] results = method.getAnnotationsByType(Result.class);
    TypeDiscriminator typeDiscriminator = method.getAnnotation(TypeDiscriminator.class);
    String resultMapId = generateResultMapName(method);
    applyResultMap(resultMapId, returnType, args, results, typeDiscriminator);
    return resultMapId;
  }

  /**
   * 根据方法生成唯一的 ResultMap ID。
   * 若方法上有 @Results 且指定了 id，则使用该 id；否则使用 "接口名.方法名-参数类型" 格式。
   *
   * @param method Mapper 方法
   * @return 生成的 ResultMap ID
   */
  private String generateResultMapName(Method method) {
    Results results = method.getAnnotation(Results.class);
    if (results != null && !results.id().isEmpty()) {
      return type.getName() + "." + results.id();
    }
    // 根据方法参数类型生成后缀，以区分重载方法
    StringBuilder suffix = new StringBuilder();
    for (Class<?> c : method.getParameterTypes()) {
      suffix.append("-");
      suffix.append(c.getSimpleName());
    }
    if (suffix.length() < 1) {
      suffix.append("-void");
    }
    return type.getName() + "." + method.getName() + suffix;
  }

  /**
   * 构建 ResultMap 并注册到 Configuration。
   *
   * @param resultMapId ResultMap 标识
   * @param returnType 返回类型
   * @param args 构造器参数注解
   * @param results 结果字段注解
   * @param discriminator 类型辨别器注解
   */
  private void applyResultMap(String resultMapId, Class<?> returnType, Arg[] args, Result[] results, TypeDiscriminator discriminator) {
    List<ResultMapping> resultMappings = new ArrayList<>();
    applyConstructorArgs(args, returnType, resultMappings);
    applyResults(results, returnType, resultMappings);
    Discriminator disc = applyDiscriminator(resultMapId, returnType, discriminator);
    // TODO add AutoMappingBehaviour
    assistant.addResultMap(resultMapId, returnType, null, disc, resultMappings, null);
    createDiscriminatorResultMaps(resultMapId, returnType, discriminator);
  }

  /**
   * 为 TypeDiscriminator 的每个 Case 创建子 ResultMap。
   *
   * @param resultMapId 父 ResultMap ID
   * @param resultType  result 类型
   * @param discriminator 类型辨别器注解
   */
  private void createDiscriminatorResultMaps(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
    if (discriminator != null) {
      for (Case c : discriminator.cases()) {
        String caseResultMapId = resultMapId + "-" + c.value();
        List<ResultMapping> resultMappings = new ArrayList<>();
        // issue #136
        applyConstructorArgs(c.constructArgs(), resultType, resultMappings);
        applyResults(c.results(), resultType, resultMappings);
        // TODO add AutoMappingBehaviour
        assistant.addResultMap(caseResultMapId, c.type(), resultMapId, null, resultMappings, null);
      }
    }
  }

  /**
   * 构建 Discriminator 对象，用于处理继承映射。
   *
   * @param resultMapId ResultMap ID
   * @param resultType  result 类型
   * @param discriminator 类型辨别器注解
   * @return 构建的 Discriminator，若无辨别器则返回 null
   */
  private Discriminator applyDiscriminator(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
    if (discriminator != null) {
      String column = discriminator.column();
      Class<?> javaType = discriminator.javaType() == void.class ? String.class : discriminator.javaType();
      JdbcType jdbcType = discriminator.jdbcType() == JdbcType.UNDEFINED ? null : discriminator.jdbcType();
      @SuppressWarnings("unchecked")
      Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>)
              (discriminator.typeHandler() == UnknownTypeHandler.class ? null : discriminator.typeHandler());
      Case[] cases = discriminator.cases();
      // 构建辨别值到子 ResultMap 的映射
      Map<String, String> discriminatorMap = new HashMap<>();
      for (Case c : cases) {
        String value = c.value();
        String caseResultMapId = resultMapId + "-" + value;
        discriminatorMap.put(value, caseResultMapId);
      }
      return assistant.buildDiscriminator(resultType, column, javaType, jdbcType, typeHandler, discriminatorMap);
    }
    return null;
  }

  /**
   * 解析方法上的 SQL 语句注解，构建 MappedStatement。
   * 支持 Select、Update、Insert、Delete 及其 Provider 注解。
   *
   * @param method Mapper 方法
   */
  void parseStatement(Method method) {
    final Class<?> parameterTypeClass = getParameterType(method);
    final LanguageDriver languageDriver = getLanguageDriver(method);

    getAnnotationWrapper(method, true, statementAnnotationTypes).ifPresent(statementAnnotation -> {
      final SqlSource sqlSource = buildSqlSource(statementAnnotation.getAnnotation(), parameterTypeClass, languageDriver, method);
      final SqlCommandType sqlCommandType = statementAnnotation.getSqlCommandType();
      final Options options = getAnnotationWrapper(method, false, Options.class).map(x -> (Options)x.getAnnotation()).orElse(null);
      final String mappedStatementId = type.getName() + "." + method.getName();

      // 处理主键生成策略
      final KeyGenerator keyGenerator;
      String keyProperty = null;
      String keyColumn = null;
      if (SqlCommandType.INSERT.equals(sqlCommandType) || SqlCommandType.UPDATE.equals(sqlCommandType)) {
        // 优先检查 @SelectKey 注解，优先级高于配置
        SelectKey selectKey = getAnnotationWrapper(method, false, SelectKey.class).map(x -> (SelectKey)x.getAnnotation()).orElse(null);
        if (selectKey != null) {
          keyGenerator = handleSelectKeyAnnotation(selectKey, mappedStatementId, getParameterType(method), languageDriver);
          keyProperty = selectKey.keyProperty();
        } else if (options == null) {
          // 未配置时使用全局配置
          keyGenerator = configuration.isUseGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
        } else {
          // 使用 @Options 注解配置
          keyGenerator = options.useGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
          keyProperty = options.keyProperty();
          keyColumn = options.keyColumn();
        }
      } else {
        keyGenerator = NoKeyGenerator.INSTANCE;
      }

      // 解析执行选项
      Integer fetchSize = null;
      Integer timeout = null;
      StatementType statementType = StatementType.PREPARED;
      ResultSetType resultSetType = configuration.getDefaultResultSetType();
      boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
      boolean flushCache = !isSelect;
      boolean useCache = isSelect;
      if (options != null) {
        if (FlushCachePolicy.TRUE.equals(options.flushCache())) {
          flushCache = true;
        } else if (FlushCachePolicy.FALSE.equals(options.flushCache())) {
          flushCache = false;
        }
        useCache = options.useCache();
        fetchSize = options.fetchSize() > -1 || options.fetchSize() == Integer.MIN_VALUE ? options.fetchSize() : null; //issue #348
        timeout = options.timeout() > -1 ? options.timeout() : null;
        statementType = options.statementType();
        if (options.resultSetType() != ResultSetType.DEFAULT) {
          resultSetType = options.resultSetType();
        }
      }

      // 解析 ResultMap
      String resultMapId = null;
      if (isSelect) {
        ResultMap resultMapAnnotation = method.getAnnotation(ResultMap.class);
        if (resultMapAnnotation != null) {
          resultMapId = String.join(",", resultMapAnnotation.value());
        } else {
          resultMapId = generateResultMapName(method);
        }
      }

      assistant.addMappedStatement(
          mappedStatementId,
          sqlSource,
          statementType,
          sqlCommandType,
          fetchSize,
          timeout,
          // ParameterMapID
          null,
          parameterTypeClass,
          resultMapId,
          getReturnType(method),
          resultSetType,
          flushCache,
          useCache,
          // TODO gcode issue #577
          false,
          keyGenerator,
          keyProperty,
          keyColumn,
          statementAnnotation.getDatabaseId(),
          languageDriver,
          // ResultSets
          options != null ? nullOrEmpty(options.resultSets()) : null);
    });
  }

  /**
   * 获取方法使用的语言驱动。
   * 若方法上有 @Lang 注解则使用指定驱动，否则使用全局默认驱动。
   *
   * @param method Mapper 方法
   * @return 语言驱动实例
   */
  private LanguageDriver getLanguageDriver(Method method) {
    Lang lang = method.getAnnotation(Lang.class);
    Class<? extends LanguageDriver> langClass = null;
    if (lang != null) {
      langClass = lang.value();
    }
    return configuration.getLanguageDriver(langClass);
  }

  /**
   * 获取方法的有效参数类型。
   * 排除 RowBounds 和 ResultHandler 类型的参数。
   * 若有多个有效参数则返回 ParamMap 类型。
   *
   * @param method Mapper 方法
   * @return 参数类型，若无有效参数则返回 null
   */
  private Class<?> getParameterType(Method method) {
    Class<?> parameterType = null;
    Class<?>[] parameterTypes = method.getParameterTypes();
    for (Class<?> currentParameterType : parameterTypes) {
      // 跳过 RowBounds 和 ResultHandler 类型的参数
      if (!RowBounds.class.isAssignableFrom(currentParameterType) && !ResultHandler.class.isAssignableFrom(currentParameterType)) {
        if (parameterType == null) {
          parameterType = currentParameterType;
        } else {
          // 多个参数时使用 ParamMap 封装
          parameterType = ParamMap.class;
        }
      }
    }
    return parameterType;
  }

  /**
   * 解析方法的返回类型。
   * 处理泛型参数、Collection、Map、Optional、数组等复杂类型。
   *
   * @param method Mapper 方法
   * @return 解析后的返回类型
   */
  private Class<?> getReturnType(Method method) {
    Class<?> returnType = method.getReturnType();
    Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, type);
    if (resolvedReturnType instanceof Class) {
      returnType = (Class<?>) resolvedReturnType;
      // 数组类型取其元素类型
      if (returnType.isArray()) {
        returnType = returnType.getComponentType();
      }
      // gcode issue #508: 处理返回 void 但指定了 @ResultType 的情况
      if (void.class.equals(returnType)) {
        ResultType rt = method.getAnnotation(ResultType.class);
        if (rt != null) {
          returnType = rt.value();
        }
      }
    } else if (resolvedReturnType instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) resolvedReturnType;
      Class<?> rawType = (Class<?>) parameterizedType.getRawType();
      // 处理 Collection 和 Cursor 类型
      if (Collection.class.isAssignableFrom(rawType) || Cursor.class.isAssignableFrom(rawType)) {
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          Type returnTypeParameter = actualTypeArguments[0];
          if (returnTypeParameter instanceof Class<?>) {
            returnType = (Class<?>) returnTypeParameter;
          } else if (returnTypeParameter instanceof ParameterizedType) {
            // (gcode issue #443) actual type can be a also a parameterized type
            returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
          } else if (returnTypeParameter instanceof GenericArrayType) {
            Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter).getGenericComponentType();
            // (gcode issue #525) support List<byte[]>
            returnType = Array.newInstance(componentType, 0).getClass();
          }
        }
      // 处理 Map 类型，需配合 @MapKey 注解使用
      } else if (method.isAnnotationPresent(MapKey.class) && Map.class.isAssignableFrom(rawType)) {
        // (gcode issue 504) Do not look into Maps if there is not MapKey annotation
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments != null && actualTypeArguments.length == 2) {
          Type returnTypeParameter = actualTypeArguments[1];
          if (returnTypeParameter instanceof Class<?>) {
            returnType = (Class<?>) returnTypeParameter;
          } else if (returnTypeParameter instanceof ParameterizedType) {
            // (gcode issue 443) actual type can be a also a parameterized type
            returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
          }
        }
      // 处理 Optional 类型
      } else if (Optional.class.equals(rawType)) {
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        Type returnTypeParameter = actualTypeArguments[0];
        if (returnTypeParameter instanceof Class<?>) {
          returnType = (Class<?>) returnTypeParameter;
        }
      }
    }

    return returnType;
  }

  /**
   * 处理方法上的 @Results 注解，构建结果映射。
   *
   * @param results 结果注解数组
   * @param resultType result 类型
   * @param resultMappings 结果映射集合
   */
  private void applyResults(Result[] results, Class<?> resultType, List<ResultMapping> resultMappings) {
    for (Result result : results) {
      List<ResultFlag> flags = new ArrayList<>();
      if (result.id()) {
        flags.add(ResultFlag.ID);
      }
      @SuppressWarnings("unchecked")
      Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>)
              ((result.typeHandler() == UnknownTypeHandler.class) ? null : result.typeHandler());
      boolean hasNestedResultMap = hasNestedResultMap(result);
      ResultMapping resultMapping = assistant.buildResultMapping(
          resultType,
          nullOrEmpty(result.property()),
          nullOrEmpty(result.column()),
          result.javaType() == void.class ? null : result.javaType(),
          result.jdbcType() == JdbcType.UNDEFINED ? null : result.jdbcType(),
          hasNestedSelect(result) ? nestedSelectId(result) : null,
          hasNestedResultMap ? nestedResultMapId(result) : null,
          null,
          hasNestedResultMap ? findColumnPrefix(result) : null,
          typeHandler,
          flags,
          null,
          null,
          isLazy(result));
      resultMappings.add(resultMapping);
    }
  }

  /**
   * 获取列前缀，优先使用 @One 的配置。
   *
   * @param result 结果注解
   * @return 列前缀，若无则返回空字符串
   */
  private String findColumnPrefix(Result result) {
    String columnPrefix = result.one().columnPrefix();
    if (columnPrefix.length() < 1) {
      columnPrefix = result.many().columnPrefix();
    }
    return columnPrefix;
  }

  /**
   * 获取嵌套 ResultMap 的 ID。
   *
   * @param result 结果注解
   * @return 完整的 ResultMap ID
   */
  private String nestedResultMapId(Result result) {
    String resultMapId = result.one().resultMap();
    if (resultMapId.length() < 1) {
      resultMapId = result.many().resultMap();
    }
    // 补全 namespace 前缀
    if (!resultMapId.contains(".")) {
      resultMapId = type.getName() + "." + resultMapId;
    }
    return resultMapId;
  }

  /**
   * 判断是否存在嵌套 ResultMap 映射。
   * @One 和 @Many 不能同时使用。
   *
   * @param result 结果注解
   * @return 存在嵌套映射返回 true
   */
  private boolean hasNestedResultMap(Result result) {
    // @One 和 @Many 互斥，不能同时使用
    if (result.one().resultMap().length() > 0 && result.many().resultMap().length() > 0) {
      throw new BuilderException("Cannot use both @One and @Many annotations in the same @Result");
    }
    return result.one().resultMap().length() > 0 || result.many().resultMap().length() > 0;
  }

  /**
   * 获取嵌套查询的 select 语句 ID。
   *
   * @param result 结果注解
   * @return 完整的 select 语句 ID
   */
  private String nestedSelectId(Result result) {
    String nestedSelect = result.one().select();
    if (nestedSelect.length() < 1) {
      nestedSelect = result.many().select();
    }
    // 补全 namespace 前缀
    if (!nestedSelect.contains(".")) {
      nestedSelect = type.getName() + "." + nestedSelect;
    }
    return nestedSelect;
  }

  /**
   * 判断是否启用延迟加载。
   *
   * @param result 结果注解
   * @return 是否延迟加载
   */
  private boolean isLazy(Result result) {
    boolean isLazy = configuration.isLazyLoadingEnabled();
    // @One 关联的延迟加载配置
    if (result.one().select().length() > 0 && FetchType.DEFAULT != result.one().fetchType()) {
      isLazy = result.one().fetchType() == FetchType.LAZY;
    // @Many 关联的延迟加载配置
    } else if (result.many().select().length() > 0 && FetchType.DEFAULT != result.many().fetchType()) {
      isLazy = result.many().fetchType() == FetchType.LAZY;
    }
    return isLazy;
  }

  /**
   * 判断是否存在嵌套查询。
   * @One 和 @Many 不能同时使用。
   *
   * @param result 结果注解
   * @return 存在嵌套查询返回 true
   */
  private boolean hasNestedSelect(Result result) {
    // @One 和 @Many 互斥，不能同时使用
    if (result.one().select().length() > 0 && result.many().select().length() > 0) {
      throw new BuilderException("Cannot use both @One and @Many annotations in the same @Result");
    }
    return result.one().select().length() > 0 || result.many().select().length() > 0;
  }

  /**
   * 处理构造器参数注解 @Arg，构建构造器结果映射。
   *
   * @param args 构造器参数注解数组
   * @param resultType result 类型
   * @param resultMappings 结果映射集合
   */
  private void applyConstructorArgs(Arg[] args, Class<?> resultType, List<ResultMapping> resultMappings) {
    for (Arg arg : args) {
      List<ResultFlag> flags = new ArrayList<>();
      flags.add(ResultFlag.CONSTRUCTOR);
      if (arg.id()) {
        flags.add(ResultFlag.ID);
      }
      @SuppressWarnings("unchecked")
      Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>)
              (arg.typeHandler() == UnknownTypeHandler.class ? null : arg.typeHandler());
      ResultMapping resultMapping = assistant.buildResultMapping(
          resultType,
          nullOrEmpty(arg.name()),
          nullOrEmpty(arg.column()),
          arg.javaType() == void.class ? null : arg.javaType(),
          arg.jdbcType() == JdbcType.UNDEFINED ? null : arg.jdbcType(),
          nullOrEmpty(arg.select()),
          nullOrEmpty(arg.resultMap()),
          null,
          nullOrEmpty(arg.columnPrefix()),
          typeHandler,
          flags,
          null,
          null,
          false);
      resultMappings.add(resultMapping);
    }
  }

  /**
   * 判断字符串是否为空，若为空则返回 null。
   *
   * @param value 待检查的字符串
   * @return 非空字符串或 null
   */
  private String nullOrEmpty(String value) {
    return value == null || value.trim().length() == 0 ? null : value;
  }

  /**
   * 处理 @SelectKey 注解，创建用于获取主键的 SelectKey 语句。
   *
   * @param selectKeyAnnotation @SelectKey 注解
   * @param baseStatementId 基础语句 ID
   * @param parameterTypeClass 参数类型
   * @param languageDriver 语言驱动
   * @return 主键生成器
   */
  private KeyGenerator handleSelectKeyAnnotation(SelectKey selectKeyAnnotation, String baseStatementId, Class<?> parameterTypeClass, LanguageDriver languageDriver) {
    String id = baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
    Class<?> resultTypeClass = selectKeyAnnotation.resultType();
    StatementType statementType = selectKeyAnnotation.statementType();
    String keyProperty = selectKeyAnnotation.keyProperty();
    String keyColumn = selectKeyAnnotation.keyColumn();
    boolean executeBefore = selectKeyAnnotation.before();

    // SelectKey 语句使用默认配置
    boolean useCache = false;
    KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
    Integer fetchSize = null;
    Integer timeout = null;
    boolean flushCache = false;
    String parameterMap = null;
    String resultMap = null;
    ResultSetType resultSetTypeEnum = null;
    String databaseId = selectKeyAnnotation.databaseId().isEmpty() ? null : selectKeyAnnotation.databaseId();

    SqlSource sqlSource = buildSqlSource(selectKeyAnnotation, parameterTypeClass, languageDriver, null);
    SqlCommandType sqlCommandType = SqlCommandType.SELECT;

    // 注册 SelectKey 语句
    assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum,
        flushCache, useCache, false,
        keyGenerator, keyProperty, keyColumn, databaseId, languageDriver, null);

    id = assistant.applyCurrentNamespace(id, false);

    // 创建并注册主键生成器
    MappedStatement keyStatement = configuration.getMappedStatement(id, false);
    SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement, executeBefore);
    configuration.addKeyGenerator(id, answer);
    return answer;
  }

  /**
   * 根据注解类型构建 SqlSource。
   * 直接注解使用语言驱动解析，Provider 注解使用 ProviderSqlSource 包装。
   *
   * @param annotation 语句注解
   * @param parameterType 参数类型
   * @param languageDriver 语言驱动
   * @param method Mapper 方法
   * @return SqlSource 实例
   */
  private SqlSource buildSqlSource(Annotation annotation, Class<?> parameterType, LanguageDriver languageDriver,
      Method method) {
    if (annotation instanceof Select) {
      return buildSqlSourceFromStrings(((Select) annotation).value(), parameterType, languageDriver);
    } else if (annotation instanceof Update) {
      return buildSqlSourceFromStrings(((Update) annotation).value(), parameterType, languageDriver);
    } else if (annotation instanceof Insert) {
      return buildSqlSourceFromStrings(((Insert) annotation).value(), parameterType, languageDriver);
    } else if (annotation instanceof Delete) {
      return buildSqlSourceFromStrings(((Delete) annotation).value(), parameterType, languageDriver);
    } else if (annotation instanceof SelectKey) {
      return buildSqlSourceFromStrings(((SelectKey) annotation).statement(), parameterType, languageDriver);
    }
    // Provider 注解使用 ProviderSqlSource 处理
    return new ProviderSqlSource(assistant.getConfiguration(), annotation, type, method);
  }

  /**
   * 将字符串数组拼接后通过语言驱动创建 SqlSource。
   *
   * @param strings SQL 语句片段数组
   * @param parameterTypeClass 参数类型
   * @param languageDriver 语言驱动
   * @return SqlSource 实例
   */
  private SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass,
      LanguageDriver languageDriver) {
    return languageDriver.createSqlSource(configuration, String.join(" ", strings).trim(), parameterTypeClass);
  }

  /**
   * 获取方法上指定类型的注解包装器。
   * 支持多数据库注解匹配，根据当前数据库 ID 选择对应的注解。
   *
   * @param method Mapper 方法
   * @param errorIfNoMatch 若无匹配是否抛出异常
   * @param targetTypes 目标注解类型
   * @return 注解包装器
   */
  @SafeVarargs
  private final Optional<AnnotationWrapper> getAnnotationWrapper(Method method, boolean errorIfNoMatch,
      Class<? extends Annotation>... targetTypes) {
    return getAnnotationWrapper(method, errorIfNoMatch, Arrays.asList(targetTypes));
  }

  /**
   * 内部方法，按集合类型查找注解包装器。
   */
  private Optional<AnnotationWrapper> getAnnotationWrapper(Method method, boolean errorIfNoMatch,
      Collection<Class<? extends Annotation>> targetTypes) {
    String databaseId = configuration.getDatabaseId();
    // 按 databaseId 收集注解，检测冲突
    Map<String, AnnotationWrapper> statementAnnotations = targetTypes.stream()
        .flatMap(x -> Arrays.stream(method.getAnnotationsByType(x))).map(AnnotationWrapper::new)
        .collect(Collectors.toMap(AnnotationWrapper::getDatabaseId, x -> x, (existing, duplicate) -> {
          throw new BuilderException(String.format("Detected conflicting annotations '%s' and '%s' on '%s'.",
              existing.getAnnotation(), duplicate.getAnnotation(),
              method.getDeclaringClass().getName() + "." + method.getName()));
        }));
    AnnotationWrapper annotationWrapper = null;
    // 优先匹配当前数据库的注解
    if (databaseId != null) {
      annotationWrapper = statementAnnotations.get(databaseId);
    }
    // 降级使用默认注解（空 databaseId）
    if (annotationWrapper == null) {
      annotationWrapper = statementAnnotations.get("");
    }
    // 无匹配且要求严格匹配时抛出异常
    if (errorIfNoMatch && annotationWrapper == null && !statementAnnotations.isEmpty()) {
      throw new BuilderException(
          String.format(
              "Could not find a statement annotation that correspond a current database or default statement on method '%s.%s'. Current database id is [%s].",
              method.getDeclaringClass().getName(), method.getName(), databaseId));
    }
    return Optional.ofNullable(annotationWrapper);
  }

  /**
   * 注解包装器，内部类。
   * 封装语句注解并解析其数据库 ID 和 SQL 命令类型。
   */
  private class AnnotationWrapper {
    /** 原始注解对象 */
    private final Annotation annotation;
    /** 注解指定的数据库 ID */
    private final String databaseId;
    /** SQL 命令类型 */
    private final SqlCommandType sqlCommandType;

    /**
     * 构造注解包装器。
     *
     * @param annotation 语句注解
     */
    AnnotationWrapper(Annotation annotation) {
      super();
      this.annotation = annotation;
      if (annotation instanceof Select) {
        databaseId = ((Select) annotation).databaseId();
        sqlCommandType = SqlCommandType.SELECT;
      } else if (annotation instanceof Update) {
        databaseId = ((Update) annotation).databaseId();
        sqlCommandType = SqlCommandType.UPDATE;
      } else if (annotation instanceof Insert) {
        databaseId = ((Insert) annotation).databaseId();
        sqlCommandType = SqlCommandType.INSERT;
      } else if (annotation instanceof Delete) {
        databaseId = ((Delete) annotation).databaseId();
        sqlCommandType = SqlCommandType.DELETE;
      } else if (annotation instanceof SelectProvider) {
        databaseId = ((SelectProvider) annotation).databaseId();
        sqlCommandType = SqlCommandType.SELECT;
      } else if (annotation instanceof UpdateProvider) {
        databaseId = ((UpdateProvider) annotation).databaseId();
        sqlCommandType = SqlCommandType.UPDATE;
      } else if (annotation instanceof InsertProvider) {
        databaseId = ((InsertProvider) annotation).databaseId();
        sqlCommandType = SqlCommandType.INSERT;
      } else if (annotation instanceof DeleteProvider) {
        databaseId = ((DeleteProvider) annotation).databaseId();
        sqlCommandType = SqlCommandType.DELETE;
      } else {
        sqlCommandType = SqlCommandType.UNKNOWN;
        if (annotation instanceof Options) {
          databaseId = ((Options) annotation).databaseId();
        } else if (annotation instanceof SelectKey) {
          databaseId = ((SelectKey) annotation).databaseId();
        } else {
          databaseId = "";
        }
      }
    }

    /**
     * 获取原始注解对象。
     *
     * @return 注解
     */
    Annotation getAnnotation() {
      return annotation;
    }

    /**
     * 获取 SQL 命令类型。
     *
     * @return 命令类型
     */
    SqlCommandType getSqlCommandType() {
      return sqlCommandType;
    }

    /**
     * 获取数据库 ID。
     *
     * @return 数据库 ID
     */
    String getDatabaseId() {
      return databaseId;
    }
  }
}
