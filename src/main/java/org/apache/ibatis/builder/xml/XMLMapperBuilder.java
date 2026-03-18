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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * MyBatis Mapper XML 配置构建器，负责解析 Mapper XML 文件并构建对应的映射配置。
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLMapperBuilder extends BaseBuilder {

  /** XPath 解析器，用于解析 XML 文件 */
  private final XPathParser parser;
  /** Mapper 构建辅助器，负责构建映射语句和结果映射 */
  private final MapperBuilderAssistant builderAssistant;
  /** SQL 片段映射，存储 <sql> 标签定义的可复用 SQL 片段 */
  private final Map<String, XNode> sqlFragments;
  /** 当前 Mapper XML 文件的资源路径 */
  private final String resource;

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(reader, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  /**
   * 使用 InputStream 构造 XMLMapperBuilder。
   *
   * @param inputStream Mapper XML 文件输入流
   * @param configuration MyBatis 配置对象
   * @param resource 资源路径
   * @param sqlFragments SQL 片段映射
   * @param namespace 命名空间
   */
  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(inputStream, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  /**
   * 使用 InputStream 构造 XMLMapperBuilder（无命名空间）。
   *
   * @param inputStream Mapper XML 文件输入流
   * @param configuration MyBatis 配置对象
   * @param resource 资源路径
   * @param sqlFragments SQL 片段映射
   */
  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  /**
   * 私有构造函数，初始化所有字段。
   */
  private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    super(configuration);
    this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
    this.parser = parser;
    this.sqlFragments = sqlFragments;
    this.resource = resource;
  }

  /**
   * 解析 Mapper XML 文件，将映射配置注册到 Configuration 中。
   * 如果该资源尚未加载，则解析根元素并绑定 Mapper 接口。
   * 解析完成后处理待解析的 ResultMap、缓存引用和语句。
   */
  public void parse() {
    // 仅解析未加载过的资源，避免重复解析
    if (!configuration.isResourceLoaded(resource)) {
      configurationElement(parser.evalNode("/mapper"));
      configuration.addLoadedResource(resource);
      // 将命名空间与 Mapper 接口绑定
      bindMapperForNamespace();
    }

    // 处理之前因依赖未满足而暂缓解析的元素
    parsePendingResultMaps();
    parsePendingCacheRefs();
    parsePendingStatements();
  }

  /**
   * 获取指定 ID 的 SQL 片段。
   *
   * @param refid SQL 片段引用 ID
   * @return SQL 片段节点，不存在则返回 null
   */
  public XNode getSqlFragment(String refid) {
    return sqlFragments.get(refid);
  }

  /**
   * 解析 Mapper XML 的根元素，处理所有子元素配置。
   *
   * @param context /mapper 根节点
   */
  private void configurationElement(XNode context) {
    try {
      // 解析并验证命名空间
      String namespace = context.getStringAttribute("namespace");
      if (namespace == null || namespace.isEmpty()) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      builderAssistant.setCurrentNamespace(namespace);

      // 依次解析各配置元素
      cacheRefElement(context.evalNode("cache-ref"));          // 缓存引用
      cacheElement(context.evalNode("cache"));                  // 缓存配置
      parameterMapElement(context.evalNodes("/mapper/parameterMap")); // 参数映射
      resultMapElements(context.evalNodes("/mapper/resultMap"));      // 结果映射
      sqlElement(context.evalNodes("/mapper/sql"));              // SQL 片段
      buildStatementFromContext(context.evalNodes("select|insert|update|delete")); // SQL 语句
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }

  /**
   * 根据数据库 ID 构建 SQL 语句。
   * 先处理特定数据库的语句，再处理无数据库标识的默认语句。
   *
   * @param list SQL 语句节点列表
   */
  private void buildStatementFromContext(List<XNode> list) {
    // 根据配置选择特定数据库的语句
    if (configuration.getDatabaseId() != null) {
      buildStatementFromContext(list, configuration.getDatabaseId());
    }
    // 处理无 databaseId 属性的默认语句
    buildStatementFromContext(list, null);
  }

  /**
   * 遍历节点列表构建 SQL 语句。
   *
   * @param list SQL 语句节点列表
   * @param requiredDatabaseId 所需的数据库标识
   */
  private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
      try {
        statementParser.parseStatementNode();
      } catch (IncompleteElementException e) {
        configuration.addIncompleteStatement(statementParser);
      }
    }
  }

  /**
   * 处理之前因依赖未满足而暂缓解析的 ResultMap。
   */
  private void parsePendingResultMaps() {
    Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
    synchronized (incompleteResultMaps) {
      Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolve();
          iter.remove();
        } catch (IncompleteElementException e) {
          // 依赖仍未满足，跳过继续等待
        }
      }
    }
  }

  /**
   * 处理之前因依赖未满足而暂缓解析的缓存引用。
   */
  private void parsePendingCacheRefs() {
    Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
    synchronized (incompleteCacheRefs) {
      Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolveCacheRef();
          iter.remove();
        } catch (IncompleteElementException e) {
          // 依赖仍未满足，跳过继续等待
        }
      }
    }
  }

  /**
   * 处理之前因依赖未满足而暂缓解析的 SQL 语句。
   */
  private void parsePendingStatements() {
    Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
    synchronized (incompleteStatements) {
      Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().parseStatementNode();
          iter.remove();
        } catch (IncompleteElementException e) {
          // 依赖仍未满足，跳过继续等待
        }
      }
    }
  }

  /**
   * 解析 <cache-ref> 元素，建立当前命名空间对其他命名空间缓存的引用。
   *
   * @param context cache-ref 节点
   */
  private void cacheRefElement(XNode context) {
    if (context != null) {
      // 注册缓存引用关系
      configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
      CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
      try {
        cacheRefResolver.resolveCacheRef();
      } catch (IncompleteElementException e) {
        // 被引用的缓存尚未解析完成，添加到待处理队列
        configuration.addIncompleteCacheRef(cacheRefResolver);
      }
    }
  }

  /**
   * 解析 <cache> 元素，配置当前命名空间的缓存策略。
   *
   * @param context cache 节点
   */
  private void cacheElement(XNode context) {
    if (context != null) {
      // 解析缓存类型（默认 PerpetualCache）
      String type = context.getStringAttribute("type", "PERPETUAL");
      Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
      // 解析缓存淘汰策略（默认 LRU）
      String eviction = context.getStringAttribute("eviction", "LRU");
      Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
      // 解析其他缓存属性
      Long flushInterval = context.getLongAttribute("flushInterval");
      Integer size = context.getIntAttribute("size");
      boolean readWrite = !context.getBooleanAttribute("readOnly", false);
      boolean blocking = context.getBooleanAttribute("blocking", false);
      Properties props = context.getChildrenAsProperties();
      // 创建并注册缓存实例
      builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    }
  }

  /**
   * 解析 <parameterMap> 元素列表。
   *
   * @param list parameterMap 节点列表
   */
  private void parameterMapElement(List<XNode> list) {
    for (XNode parameterMapNode : list) {
      // 解析 parameterMap 的 id 和类型
      String id = parameterMapNode.getStringAttribute("id");
      String type = parameterMapNode.getStringAttribute("type");
      Class<?> parameterClass = resolveClass(type);
      // 解析其中的每个 parameter 元素
      List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      for (XNode parameterNode : parameterNodes) {
        // 解析各属性
        String property = parameterNode.getStringAttribute("property");
        String javaType = parameterNode.getStringAttribute("javaType");
        String jdbcType = parameterNode.getStringAttribute("jdbcType");
        String resultMap = parameterNode.getStringAttribute("resultMap");
        String mode = parameterNode.getStringAttribute("mode");
        String typeHandler = parameterNode.getStringAttribute("typeHandler");
        Integer numericScale = parameterNode.getIntAttribute("numericScale");
        // 转换枚举类型
        ParameterMode modeEnum = resolveParameterMode(mode);
        Class<?> javaTypeClass = resolveClass(javaType);
        JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
        Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
        // 构建参数映射并添加到列表
        ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property, javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
        parameterMappings.add(parameterMapping);
      }
      // 注册参数映射
      builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
    }
  }

  /**
   * 解析 <resultMap> 元素列表。
   *
   * @param list resultMap 节点列表
   */
  private void resultMapElements(List<XNode> list) {
    for (XNode resultMapNode : list) {
      try {
        resultMapElement(resultMapNode);
      } catch (IncompleteElementException e) {
        // 解析失败，添加到待处理队列稍后重试
      }
    }
  }

  /**
   * 解析单个 resultMap 节点（无额外映射和闭包类型）。
   *
   * @param resultMapNode resultMap 节点
   * @return 解析后的 ResultMap 对象
   */
  private ResultMap resultMapElement(XNode resultMapNode) {
    return resultMapElement(resultMapNode, Collections.emptyList(), null);
  }

  /**
   * 解析 resultMap 节点，支持继承和额外映射。
   *
   * @param resultMapNode resultMap 节点
   * @param additionalResultMappings 额外的结果映射
   * @param enclosingType 闭包类型（用于嵌套映射）
   * @return 解析后的 ResultMap 对象
   */
  private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings, Class<?> enclosingType) {
    ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());

    // 解析 resultMap 的类型属性，支持多种别名
    String type = resultMapNode.getStringAttribute("type",
        resultMapNode.getStringAttribute("ofType",
            resultMapNode.getStringAttribute("resultType",
                resultMapNode.getStringAttribute("javaType"))));
    Class<?> typeClass = resolveClass(type);
    // 如果未指定类型，尝试从闭包类型继承
    if (typeClass == null) {
      typeClass = inheritEnclosingType(resultMapNode, enclosingType);
    }

    // 解析子元素：constructor、discriminator、result/id
    Discriminator discriminator = null;
    List<ResultMapping> resultMappings = new ArrayList<>(additionalResultMappings);
    List<XNode> resultChildren = resultMapNode.getChildren();
    for (XNode resultChild : resultChildren) {
      if ("constructor".equals(resultChild.getName())) {
        // 构造函数映射
        processConstructorElement(resultChild, typeClass, resultMappings);
      } else if ("discriminator".equals(resultChild.getName())) {
        // 鉴别器映射
        discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
      } else {
        // 普通属性映射
        List<ResultFlag> flags = new ArrayList<>();
        if ("id".equals(resultChild.getName())) {
          flags.add(ResultFlag.ID);
        }
        resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
      }
    }

    // 解析 resultMap 的 id、extends 和 autoMapping 属性
    String id = resultMapNode.getStringAttribute("id",
            resultMapNode.getValueBasedIdentifier());
    String extend = resultMapNode.getStringAttribute("extends");
    Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");

    // 创建 ResultMap 解析器并解析
    ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
    try {
      return resultMapResolver.resolve();
    } catch (IncompleteElementException e) {
      // 依赖未满足，添加到待处理队列
      configuration.addIncompleteResultMap(resultMapResolver);
      throw e;
    }
  }

  /**
   * 从闭包类型继承 resultMap 的类型。
   *
   * @param resultMapNode resultMap 节点
   * @param enclosingType 闭包类型
   * @return 继承的类型，未继承则返回 null
   */
  protected Class<?> inheritEnclosingType(XNode resultMapNode, Class<?> enclosingType) {
    // association 节点：根据属性类型推断 resultMap 类型
    if ("association".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      String property = resultMapNode.getStringAttribute("property");
      if (property != null && enclosingType != null) {
        MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
        return metaResultType.getSetterType(property);
      }
    } else if ("case".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      // case 节点：直接使用闭包类型
      return enclosingType;
    }
    return null;
  }

  /**
   * 解析 <constructor> 元素，构建构造函数参数映射。
   *
   * @param resultChild constructor 节点
   * @param resultType 结果类型
   * @param resultMappings 结果映射列表
   */
  private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) {
    List<XNode> argChildren = resultChild.getChildren();
    for (XNode argChild : argChildren) {
      List<ResultFlag> flags = new ArrayList<>();
      flags.add(ResultFlag.CONSTRUCTOR);
      // idArg 标记为 ID 标识
      if ("idArg".equals(argChild.getName())) {
        flags.add(ResultFlag.ID);
      }
      resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
    }
  }

  /**
   * 解析 <discriminator> 元素，构建鉴别器映射。
   *
   * @param context discriminator 节点
   * @param resultType 结果类型
   * @param resultMappings 结果映射列表
   * @return 解析后的 Discriminator 对象
   */
  private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType, List<ResultMapping> resultMappings) {
    // 解析鉴别器的基本属性
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String typeHandler = context.getStringAttribute("typeHandler");
    Class<?> javaTypeClass = resolveClass(javaType);
    Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);

    // 解析每个 case 子元素，建立值到 resultMap 的映射
    Map<String, String> discriminatorMap = new HashMap<>();
    for (XNode caseChild : context.getChildren()) {
      String value = caseChild.getStringAttribute("value");
      String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings, resultType));
      discriminatorMap.put(value, resultMap);
    }
    return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
  }

  /**
   * 解析 <sql> 元素，根据数据库 ID 筛选后存储到 sqlFragments。
   *
   * @param list sql 节点列表
   */
  private void sqlElement(List<XNode> list) {
    // 根据配置选择特定数据库的 SQL 片段
    if (configuration.getDatabaseId() != null) {
      sqlElement(list, configuration.getDatabaseId());
    }
    // 处理无 databaseId 属性的默认 SQL 片段
    sqlElement(list, null);
  }

  /**
   * 遍历 SQL 节点列表，筛选匹配的片段并存储。
   *
   * @param list sql 节点列表
   * @param requiredDatabaseId 所需的数据库标识
   */
  private void sqlElement(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      String databaseId = context.getStringAttribute("databaseId");
      String id = context.getStringAttribute("id");
      // 应用当前命名空间
      id = builderAssistant.applyCurrentNamespace(id, false);
      // 检查是否匹配当前数据库 ID
      if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
        sqlFragments.put(id, context);
      }
    }
  }

  /**
   * 判断 SQL 片段的 databaseId 是否匹配当前所需。
   *
   * @param id SQL 片段 ID
   * @param databaseId 片段的 databaseId 属性
   * @param requiredDatabaseId 所需的 databaseId
   * @return 是否匹配
   */
  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    // 需要特定数据库：严格匹配
    if (requiredDatabaseId != null) {
      return requiredDatabaseId.equals(databaseId);
    }
    // 需要默认片段但已有特定数据库的片段：跳过
    if (databaseId != null) {
      return false;
    }
    // 无 databaseId 且尚未存在该 ID 的片段：匹配
    if (!this.sqlFragments.containsKey(id)) {
      return true;
    }
    // 已存在该 ID 的片段：如果之前的片段没有 databaseId 则跳过当前
    XNode context = this.sqlFragments.get(id);
    return context.getStringAttribute("databaseId") == null;
  }

  /**
   * 从节点上下文构建 ResultMapping。
   *
   * @param context result/id/association/collection 等节点
   * @param resultType 结果类型
   * @param flags 结果映射标志
   * @return 解析后的 ResultMapping 对象
   */
  private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) {
    // 构造函数使用 name 属性，普通属性使用 property
    String property;
    if (flags.contains(ResultFlag.CONSTRUCTOR)) {
      property = context.getStringAttribute("name");
    } else {
      property = context.getStringAttribute("property");
    }
    // 解析各属性
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String nestedSelect = context.getStringAttribute("select");
    String nestedResultMap = context.getStringAttribute("resultMap", () ->
        processNestedResultMappings(context, Collections.emptyList(), resultType));
    String notNullColumn = context.getStringAttribute("notNullColumn");
    String columnPrefix = context.getStringAttribute("columnPrefix");
    String typeHandler = context.getStringAttribute("typeHandler");
    String resultSet = context.getStringAttribute("resultSet");
    String foreignColumn = context.getStringAttribute("foreignColumn");
    // 延迟加载策略：默认跟随全局配置
    boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));

    // 解析类型别名
    Class<?> javaTypeClass = resolveClass(javaType);
    Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);

    return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
  }

  /**
   * 处理嵌套的结果映射（association/collection/case）。
   *
   * @param context 当前节点
   * @param resultMappings 结果映射列表
   * @param enclosingType 闭包类型
   * @return 嵌套 resultMap 的 ID，不存在则返回 null
   */
  private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings, Class<?> enclosingType) {
    // 仅处理使用嵌套 resultMap 的 association/collection/case
    if (Arrays.asList("association", "collection", "case").contains(context.getName())
        && context.getStringAttribute("select") == null) {
      // 验证集合类型是否明确
      validateCollection(context, enclosingType);
      ResultMap resultMap = resultMapElement(context, resultMappings, enclosingType);
      return resultMap.getId();
    }
    return null;
  }

  /**
   * 验证 collection 元素的类型是否明确，避免歧义。
   *
   * @param context collection 节点
   * @param enclosingType 闭包类型
   */
  protected void validateCollection(XNode context, Class<?> enclosingType) {
    // 未指定 javaType 和 resultMap 时，需要能从闭包类型推断出集合元素类型
    if ("collection".equals(context.getName()) && context.getStringAttribute("resultMap") == null
        && context.getStringAttribute("javaType") == null) {
      MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
      String property = context.getStringAttribute("property");
      if (!metaResultType.hasSetter(property)) {
        throw new BuilderException(
            "Ambiguous collection type for property '" + property + "'. You must specify 'javaType' or 'resultMap'.");
      }
    }
  }

  /**
   * 将命名空间与对应的 Mapper 接口绑定。
   */
  private void bindMapperForNamespace() {
    String namespace = builderAssistant.getCurrentNamespace();
    if (namespace != null) {
      // 尝试根据命名空间加载对应的 Mapper 接口
      Class<?> boundType = null;
      try {
        boundType = Resources.classForName(namespace);
      } catch (ClassNotFoundException e) {
        // 命名空间可能不对应接口，跳过绑定
      }
      // 注册 Mapper 接口
      if (boundType != null && !configuration.hasMapper(boundType)) {
        // 标记命名空间资源已加载，防止重复加载
        configuration.addLoadedResource("namespace:" + namespace);
        configuration.addMapper(boundType);
      }
    }
  }

}
