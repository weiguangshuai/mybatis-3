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
package org.apache.ibatis.session;

import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.builder.annotation.MethodResolver;
import org.apache.ibatis.builder.xml.XMLStatementBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SoftCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.datasource.jndi.JndiDataSourceFactory;
import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;
import org.apache.ibatis.executor.*;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.executor.loader.cglib.CglibProxyFactory;
import org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.InterceptorChain;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.LanguageDriverRegistry;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * MyBatis 核心配置类 - 框架的中枢，持有所有配置信息、注册表和已解析的映射元素。
 * <p>
 * 在启动阶段由 {@link org.apache.ibatis.builder.xml.XMLConfigBuilder} 解析 mybatis-config.xml 填充，
 * 运行时作为各组件（Executor、StatementHandler、ResultSetHandler 等）的工厂和配置源。
 *
 * @author Clinton Begin
 */
public class Configuration {

  /**
   * 运行环境 - 包含数据源、事务工厂等环境相关配置
   */
  protected Environment environment;

  /**
   * 是否启用安全的 RowBounds（嵌套语句中禁用分页）
   */
  protected boolean safeRowBoundsEnabled;
  /**
   * 是否启用安全的 ResultHandler（嵌套语句中禁用自定义结果处理器）
   */
  protected boolean safeResultHandlerEnabled = true;
  /**
   * 是否将下划线��名自动转换为驼峰命名
   */
  protected boolean mapUnderscoreToCamelCase;
  /**
   * 是否开启激进懒加载（触发任一懒加载属性时加载全部）
   */
  protected boolean aggressiveLazyLoading;
  /**
   * 是否允许单条语句返回多结果集
   */
  protected boolean multipleResultSetsEnabled = true;
  /**
   * 是否使用 JDBC 自动生成主键
   */
  protected boolean useGeneratedKeys;
  /**
   * 是否使用列标签代替列名作为结果集键
   */
  protected boolean useColumnLabel = true;
  /**
   * 是否启用二级缓存（全局开关）
   */
  protected boolean cacheEnabled = true;
  /**
   * 结果为 null 时是否仍调用 setter（用于 Map.put(key, null) 这类场景）
   */
  protected boolean callSettersOnNulls;
  /**
   * 是否使用方法真实参数名（需要编译时保留参数名信息）
   */
  protected boolean useActualParamName = true;
  /**
   * 查询返回空行时是否仍返回一个实例（所有属性为 null）
   */
  protected boolean returnInstanceForEmptyRow;
  /**
   * 是否压缩 SQL 中的多余空白
   */
  protected boolean shrinkWhitespacesInSql;
  /**
   * foreach 标签 nullable 属性的默认值
   */
  protected boolean nullableOnForEach;
  /**
   * 构造器自动映射是否基于参数名（而非位置）
   */
  protected boolean argNameBasedConstructorAutoMapping;

  /**
   * 日志前缀
   */
  protected String logPrefix;
  /**
   * 日志实现类
   */
  protected Class<? extends Log> logImpl;
  /**
   * VFS 实现类，用于查找资源文件
   */
  protected Class<? extends VFS> vfsImpl;
  /**
   * SqlProvider 注解省略 type 时使用的默认 Provider 类型
   */
  protected Class<?> defaultSqlProviderType;
  /**
   * 一级缓存作用域，默认为 SESSION
   */
  protected LocalCacheScope localCacheScope = LocalCacheScope.SESSION;
  /**
   * 参数为 null 时对应的 JDBC 类型
   */
  protected JdbcType jdbcTypeForNull = JdbcType.OTHER;
  /**
   * 触发懒加载的方法名集合，默认为对象基础方法
   */
  protected Set<String> lazyLoadTriggerMethods = new HashSet<>(Arrays.asList("equals", "clone", "hashCode", "toString"));
  /**
   * SQL 默认超时时间（秒）
   */
  protected Integer defaultStatementTimeout;
  /**
   * 默认的 fetchSize
   */
  protected Integer defaultFetchSize;
  /**
   * 默认的 ResultSet 类型（FORWARD_ONLY 等）
   */
  protected ResultSetType defaultResultSetType;
  /**
   * 默认的执行器类型：SIMPLE/REUSE/BATCH
   */
  protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;
  /**
   * 自动映射行为，默认 PARTIAL（部分自动映射，嵌套映射需显式声明）
   */
  protected AutoMappingBehavior autoMappingBehavior = AutoMappingBehavior.PARTIAL;
  /**
   * 自动映射遇到未知列时的处理行为
   */
  protected AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior = AutoMappingUnknownColumnBehavior.NONE;

  /**
   * 全局属性变量，可在配置中通过 ${} 引用
   */
  protected Properties variables = new Properties();
  /**
   * 反射器工厂，提供类元信息缓存
   */
  protected ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
  /**
   * 对象工厂，用于实例化结果对象
   */
  protected ObjectFactory objectFactory = new DefaultObjectFactory();
  /**
   * 对象包装工厂，用于创建 MetaObject
   */
  protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();

  /**
   * 是否启用懒加载
   */
  protected boolean lazyLoadingEnabled = false;
  /**
   * 懒加载代理工厂，默认使用 Javassist
   */
  protected ProxyFactory proxyFactory = new JavassistProxyFactory(); // #224 Using internal Javassist instead of OGNL

  /**
   * 数据库厂商标识，用于区分不同数据库的 SQL
   */
  protected String databaseId;
  /**
   * Configuration factory class.
   * Used to create Configuration for loading deserialized unread properties.
   *
   * @see <a href='https://github.com/mybatis/old-google-code-issues/issues/300'>Issue 300 (google code)</a>
   */
  protected Class<?> configurationFactory;

  /**
   * Mapper 接口注册表，管理 MapperProxyFactory
   */
  protected final MapperRegistry mapperRegistry = new MapperRegistry(this);
  /**
   * 拦截器链，按注册顺序应用于 Executor/StatementHandler/ParameterHandler/ResultSetHandler
   */
  protected final InterceptorChain interceptorChain = new InterceptorChain();
  /**
   * 类型处理器注册表，管理 Java 类型与 JDBC 类型的转换
   */
  protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry(this);
  /**
   * 类型别名注册表
   */
  protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
  /**
   * 脚本语言驱动注册表（解析动态 SQL）
   */
  protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();

  /**
   * 所有已解析的 MappedStatement，以 statementId 为键
   */
  protected final Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>("Mapped Statements collection")
    .conflictMessageProducer((savedValue, targetValue) ->
      ". please check " + savedValue.getResource() + " and " + targetValue.getResource());
  /**
   * 二级缓存集合，以 namespace 为键
   */
  protected final Map<String, Cache> caches = new StrictMap<>("Caches collection");
  /**
   * 所有 ResultMap，以全限定 id 为键
   */
  protected final Map<String, ResultMap> resultMaps = new StrictMap<>("Result Maps collection");
  /**
   * 所有 ParameterMap，以全限定 id 为键
   */
  protected final Map<String, ParameterMap> parameterMaps = new StrictMap<>("Parameter Maps collection");
  /**
   * 所有主键生成器，以 statementId + "!selectKey" 形式为键
   */
  protected final Map<String, KeyGenerator> keyGenerators = new StrictMap<>("Key Generators collection");

  /**
   * 已加载资源集合，防止重复解析同一 Mapper
   */
  protected final Set<String> loadedResources = new HashSet<>();
  /**
   * 复用的 SQL 片段（&lt;sql&gt; 标签）集合，以 fragmentId 为键
   */
  protected final Map<String, XNode> sqlFragments = new StrictMap<>("XML fragments parsed from previous mappers");

  /**
   * 因依赖未就绪而暂缓解析的 Statement 列表
   */
  protected final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList<>();
  /**
   * 因依赖未就绪而暂缓解析的 cache-ref 列表
   */
  protected final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList<>();
  /**
   * 因依赖未就绪而暂缓解析的 ResultMap 列表
   */
  protected final Collection<ResultMapResolver> incompleteResultMaps = new LinkedList<>();
  /**
   * 因依赖未就绪而暂缓解析的注解方法列表
   */
  protected final Collection<MethodResolver> incompleteMethods = new LinkedList<>();

  /*
   * A map holds cache-ref relationship. The key is the namespace that
   * references a cache bound to another namespace and the value is the
   * namespace which the actual cache is bound to.
   */
  protected final Map<String, String> cacheRefMap = new HashMap<>();

  /**
   * 基于指定环境构造 Configuration
   *
   * @param environment 运行环境配置
   */
  public Configuration(Environment environment) {
    this();
    this.environment = environment;
  }

  /**
   * 默认构造函数 - 注册框架内置的类型别名和脚本语言驱动
   */
  public Configuration() {
    // 注册事务工厂别名
    typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
    typeAliasRegistry.registerAlias("MANAGED", ManagedTransactionFactory.class);

    // 注册数据源工厂别名
    typeAliasRegistry.registerAlias("JNDI", JndiDataSourceFactory.class);
    typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
    typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);

    // 注册缓存实现别名（PerpetualCache 为基础实现，其余为装饰器）
    typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class);
    typeAliasRegistry.registerAlias("FIFO", FifoCache.class);
    typeAliasRegistry.registerAlias("LRU", LruCache.class);
    typeAliasRegistry.registerAlias("SOFT", SoftCache.class);
    typeAliasRegistry.registerAlias("WEAK", WeakCache.class);

    // 注册数据库厂商识别器
    typeAliasRegistry.registerAlias("DB_VENDOR", VendorDatabaseIdProvider.class);

    // 注册脚本语言驱动别名
    typeAliasRegistry.registerAlias("XML", XMLLanguageDriver.class);
    typeAliasRegistry.registerAlias("RAW", RawLanguageDriver.class);

    // 注册日志实现别名
    typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);
    typeAliasRegistry.registerAlias("COMMONS_LOGGING", JakartaCommonsLoggingImpl.class);
    typeAliasRegistry.registerAlias("LOG4J", Log4jImpl.class);
    typeAliasRegistry.registerAlias("LOG4J2", Log4j2Impl.class);
    typeAliasRegistry.registerAlias("JDK_LOGGING", Jdk14LoggingImpl.class);
    typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
    typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);

    // 注册懒加载代理工厂别名
    typeAliasRegistry.registerAlias("CGLIB", CglibProxyFactory.class);
    typeAliasRegistry.registerAlias("JAVASSIST", JavassistProxyFactory.class);

    // 设置默认脚本语言驱动为 XML（支持动态 SQL 标签）
    languageRegistry.setDefaultDriverClass(XMLLanguageDriver.class);
    languageRegistry.register(RawLanguageDriver.class);
  }

  public String getLogPrefix() {
    return logPrefix;
  }

  public void setLogPrefix(String logPrefix) {
    this.logPrefix = logPrefix;
  }

  public Class<? extends Log> getLogImpl() {
    return logImpl;
  }

  /**
   * 设置日志实现类，同时触发 LogFactory 切换到该实现
   *
   * @param logImpl 日志实现类
   */
  public void setLogImpl(Class<? extends Log> logImpl) {
    if (logImpl != null) {
      this.logImpl = logImpl;
      LogFactory.useCustomLogging(this.logImpl);
    }
  }

  public Class<? extends VFS> getVfsImpl() {
    return this.vfsImpl;
  }

  /**
   * 设置 VFS 实现类，同时注册到 VFS 静态实现列表
   *
   * @param vfsImpl VFS 实现类
   */
  public void setVfsImpl(Class<? extends VFS> vfsImpl) {
    if (vfsImpl != null) {
      this.vfsImpl = vfsImpl;
      VFS.addImplClass(this.vfsImpl);
    }
  }

  /**
   * Gets an applying type when omit a type on sql provider annotation(e.g. {@link org.apache.ibatis.annotations.SelectProvider}).
   *
   * @return the default type for sql provider annotation
   * @since 3.5.6
   */
  public Class<?> getDefaultSqlProviderType() {
    return defaultSqlProviderType;
  }

  /**
   * Sets an applying type when omit a type on sql provider annotation(e.g. {@link org.apache.ibatis.annotations.SelectProvider}).
   *
   * @param defaultSqlProviderType the default type for sql provider annotation
   * @since 3.5.6
   */
  public void setDefaultSqlProviderType(Class<?> defaultSqlProviderType) {
    this.defaultSqlProviderType = defaultSqlProviderType;
  }

  public boolean isCallSettersOnNulls() {
    return callSettersOnNulls;
  }

  public void setCallSettersOnNulls(boolean callSettersOnNulls) {
    this.callSettersOnNulls = callSettersOnNulls;
  }

  public boolean isUseActualParamName() {
    return useActualParamName;
  }

  public void setUseActualParamName(boolean useActualParamName) {
    this.useActualParamName = useActualParamName;
  }

  public boolean isReturnInstanceForEmptyRow() {
    return returnInstanceForEmptyRow;
  }

  public void setReturnInstanceForEmptyRow(boolean returnEmptyInstance) {
    this.returnInstanceForEmptyRow = returnEmptyInstance;
  }

  public boolean isShrinkWhitespacesInSql() {
    return shrinkWhitespacesInSql;
  }

  public void setShrinkWhitespacesInSql(boolean shrinkWhitespacesInSql) {
    this.shrinkWhitespacesInSql = shrinkWhitespacesInSql;
  }

  /**
   * Sets the default value of 'nullable' attribute on 'foreach' tag.
   *
   * @param nullableOnForEach If nullable, set to {@code true}
   * @since 3.5.9
   */
  public void setNullableOnForEach(boolean nullableOnForEach) {
    this.nullableOnForEach = nullableOnForEach;
  }

  /**
   * Returns the default value of 'nullable' attribute on 'foreach' tag.
   *
   * <p>Default is {@code false}.
   *
   * @return If nullable, set to {@code true}
   * @since 3.5.9
   */
  public boolean isNullableOnForEach() {
    return nullableOnForEach;
  }

  public boolean isArgNameBasedConstructorAutoMapping() {
    return argNameBasedConstructorAutoMapping;
  }

  public void setArgNameBasedConstructorAutoMapping(boolean argNameBasedConstructorAutoMapping) {
    this.argNameBasedConstructorAutoMapping = argNameBasedConstructorAutoMapping;
  }

  public String getDatabaseId() {
    return databaseId;
  }

  public void setDatabaseId(String databaseId) {
    this.databaseId = databaseId;
  }

  public Class<?> getConfigurationFactory() {
    return configurationFactory;
  }

  public void setConfigurationFactory(Class<?> configurationFactory) {
    this.configurationFactory = configurationFactory;
  }

  public boolean isSafeResultHandlerEnabled() {
    return safeResultHandlerEnabled;
  }

  public void setSafeResultHandlerEnabled(boolean safeResultHandlerEnabled) {
    this.safeResultHandlerEnabled = safeResultHandlerEnabled;
  }

  public boolean isSafeRowBoundsEnabled() {
    return safeRowBoundsEnabled;
  }

  public void setSafeRowBoundsEnabled(boolean safeRowBoundsEnabled) {
    this.safeRowBoundsEnabled = safeRowBoundsEnabled;
  }

  public boolean isMapUnderscoreToCamelCase() {
    return mapUnderscoreToCamelCase;
  }

  public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
    this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
  }

  /**
   * 标记资源已加载，防止重复解析
   *
   * @param resource 资源标识
   */
  public void addLoadedResource(String resource) {
    loadedResources.add(resource);
  }

  /**
   * 判断资源是否已被加载
   *
   * @param resource 资源标识
   * @return 已加载返回 true
   */
  public boolean isResourceLoaded(String resource) {
    return loadedResources.contains(resource);
  }

  public Environment getEnvironment() {
    return environment;
  }

  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  public AutoMappingBehavior getAutoMappingBehavior() {
    return autoMappingBehavior;
  }

  public void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior) {
    this.autoMappingBehavior = autoMappingBehavior;
  }

  /**
   * Gets the auto mapping unknown column behavior.
   *
   * @return the auto mapping unknown column behavior
   * @since 3.4.0
   */
  public AutoMappingUnknownColumnBehavior getAutoMappingUnknownColumnBehavior() {
    return autoMappingUnknownColumnBehavior;
  }

  /**
   * Sets the auto mapping unknown column behavior.
   *
   * @param autoMappingUnknownColumnBehavior the new auto mapping unknown column behavior
   * @since 3.4.0
   */
  public void setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior) {
    this.autoMappingUnknownColumnBehavior = autoMappingUnknownColumnBehavior;
  }

  public boolean isLazyLoadingEnabled() {
    return lazyLoadingEnabled;
  }

  public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {
    this.lazyLoadingEnabled = lazyLoadingEnabled;
  }

  public ProxyFactory getProxyFactory() {
    return proxyFactory;
  }

  public void setProxyFactory(ProxyFactory proxyFactory) {
    if (proxyFactory == null) {
      proxyFactory = new JavassistProxyFactory();
    }
    this.proxyFactory = proxyFactory;
  }

  public boolean isAggressiveLazyLoading() {
    return aggressiveLazyLoading;
  }

  public void setAggressiveLazyLoading(boolean aggressiveLazyLoading) {
    this.aggressiveLazyLoading = aggressiveLazyLoading;
  }

  public boolean isMultipleResultSetsEnabled() {
    return multipleResultSetsEnabled;
  }

  public void setMultipleResultSetsEnabled(boolean multipleResultSetsEnabled) {
    this.multipleResultSetsEnabled = multipleResultSetsEnabled;
  }

  public Set<String> getLazyLoadTriggerMethods() {
    return lazyLoadTriggerMethods;
  }

  public void setLazyLoadTriggerMethods(Set<String> lazyLoadTriggerMethods) {
    this.lazyLoadTriggerMethods = lazyLoadTriggerMethods;
  }

  public boolean isUseGeneratedKeys() {
    return useGeneratedKeys;
  }

  public void setUseGeneratedKeys(boolean useGeneratedKeys) {
    this.useGeneratedKeys = useGeneratedKeys;
  }

  public ExecutorType getDefaultExecutorType() {
    return defaultExecutorType;
  }

  public void setDefaultExecutorType(ExecutorType defaultExecutorType) {
    this.defaultExecutorType = defaultExecutorType;
  }

  public boolean isCacheEnabled() {
    return cacheEnabled;
  }

  public void setCacheEnabled(boolean cacheEnabled) {
    this.cacheEnabled = cacheEnabled;
  }

  public Integer getDefaultStatementTimeout() {
    return defaultStatementTimeout;
  }

  public void setDefaultStatementTimeout(Integer defaultStatementTimeout) {
    this.defaultStatementTimeout = defaultStatementTimeout;
  }

  /**
   * Gets the default fetch size.
   *
   * @return the default fetch size
   * @since 3.3.0
   */
  public Integer getDefaultFetchSize() {
    return defaultFetchSize;
  }

  /**
   * Sets the default fetch size.
   *
   * @param defaultFetchSize the new default fetch size
   * @since 3.3.0
   */
  public void setDefaultFetchSize(Integer defaultFetchSize) {
    this.defaultFetchSize = defaultFetchSize;
  }

  /**
   * Gets the default result set type.
   *
   * @return the default result set type
   * @since 3.5.2
   */
  public ResultSetType getDefaultResultSetType() {
    return defaultResultSetType;
  }

  /**
   * Sets the default result set type.
   *
   * @param defaultResultSetType the new default result set type
   * @since 3.5.2
   */
  public void setDefaultResultSetType(ResultSetType defaultResultSetType) {
    this.defaultResultSetType = defaultResultSetType;
  }

  public boolean isUseColumnLabel() {
    return useColumnLabel;
  }

  public void setUseColumnLabel(boolean useColumnLabel) {
    this.useColumnLabel = useColumnLabel;
  }

  public LocalCacheScope getLocalCacheScope() {
    return localCacheScope;
  }

  public void setLocalCacheScope(LocalCacheScope localCacheScope) {
    this.localCacheScope = localCacheScope;
  }

  public JdbcType getJdbcTypeForNull() {
    return jdbcTypeForNull;
  }

  public void setJdbcTypeForNull(JdbcType jdbcTypeForNull) {
    this.jdbcTypeForNull = jdbcTypeForNull;
  }

  public Properties getVariables() {
    return variables;
  }

  public void setVariables(Properties variables) {
    this.variables = variables;
  }

  public TypeHandlerRegistry getTypeHandlerRegistry() {
    return typeHandlerRegistry;
  }

  /**
   * Set a default {@link TypeHandler} class for {@link Enum}.
   * A default {@link TypeHandler} is {@link org.apache.ibatis.type.EnumTypeHandler}.
   *
   * @param typeHandler a type handler class for {@link Enum}
   * @since 3.4.5
   */
  public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
    if (typeHandler != null) {
      getTypeHandlerRegistry().setDefaultEnumTypeHandler(typeHandler);
    }
  }

  public TypeAliasRegistry getTypeAliasRegistry() {
    return typeAliasRegistry;
  }

  /**
   * Gets the mapper registry.
   *
   * @return the mapper registry
   * @since 3.2.2
   */
  public MapperRegistry getMapperRegistry() {
    return mapperRegistry;
  }

  public ReflectorFactory getReflectorFactory() {
    return reflectorFactory;
  }

  public void setReflectorFactory(ReflectorFactory reflectorFactory) {
    this.reflectorFactory = reflectorFactory;
  }

  public ObjectFactory getObjectFactory() {
    return objectFactory;
  }

  public void setObjectFactory(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  public ObjectWrapperFactory getObjectWrapperFactory() {
    return objectWrapperFactory;
  }

  public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
    this.objectWrapperFactory = objectWrapperFactory;
  }

  /**
   * Gets the interceptors.
   *
   * @return the interceptors
   * @since 3.2.2
   */
  public List<Interceptor> getInterceptors() {
    return interceptorChain.getInterceptors();
  }

  public LanguageDriverRegistry getLanguageRegistry() {
    return languageRegistry;
  }

  /**
   * 设置默认脚本语言驱动，null 时回退为 XMLLanguageDriver
   *
   * @param driver 语言驱动类
   */
  public void setDefaultScriptingLanguage(Class<? extends LanguageDriver> driver) {
    if (driver == null) {
      driver = XMLLanguageDriver.class;
    }
    getLanguageRegistry().setDefaultDriverClass(driver);
  }

  public LanguageDriver getDefaultScriptingLanguageInstance() {
    return languageRegistry.getDefaultDriver();
  }

  /**
   * Gets the language driver.
   *
   * @param langClass the lang class
   * @return the language driver
   * @since 3.5.1
   */
  public LanguageDriver getLanguageDriver(Class<? extends LanguageDriver> langClass) {
    if (langClass == null) {
      return languageRegistry.getDefaultDriver();
    }
    languageRegistry.register(langClass);
    return languageRegistry.getDriver(langClass);
  }

  /**
   * Gets the default scripting language instance.
   *
   * @return the default scripting language instance
   * @deprecated Use {@link #getDefaultScriptingLanguageInstance()}
   */
  @Deprecated
  public LanguageDriver getDefaultScriptingLanuageInstance() {
    return getDefaultScriptingLanguageInstance();
  }

  /**
   * 基于当前配置创建一个 MetaObject（反射操作的包装对象）
   *
   * @param object 目标对象
   * @return 对应的 MetaObject
   */
  public MetaObject newMetaObject(Object object) {
    return MetaObject.forObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
  }

  /**
   * 创建参数处理器，会经过拦截器链包装
   *
   * @param mappedStatement 映射语句
   * @param parameterObject 参数对象
   * @param boundSql        绑定的 SQL
   * @return 被插件代理后的参数处理器
   */
  public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
    // 应用拦截器链，插件可在此介入参数设置逻��
    parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
    return parameterHandler;
  }

  /**
   * 创建结果集处理器，会经过拦截器链包装
   *
   * @param executor         执行器
   * @param mappedStatement  映射语句
   * @param rowBounds        分页参数
   * @param parameterHandler 参数处理器
   * @param resultHandler    结果处理器
   * @param boundSql         绑定的 SQL
   * @return 被插件代理后的结果集处理器
   */
  public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler,
                                              ResultHandler resultHandler, BoundSql boundSql) {
    ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
    resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
    return resultSetHandler;
  }

  /**
   * 创建语句处理器（RoutingStatementHandler 会根据 StatementType 路由到具体实现），会经过拦截器链包装
   *
   * @param executor        执行器
   * @param mappedStatement 映射语句
   * @param parameterObject 参数对象
   * @param rowBounds       分页参数
   * @param resultHandler   结果处理器
   * @param boundSql        绑定的 SQL
   * @return 被插件代理后的语句处理器
   */
  public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
    statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
    return statementHandler;
  }

  /**
   * 使用默认执行器类型创建执行器
   *
   * @param transaction 事务对象
   * @return 执行器
   */
  public Executor newExecutor(Transaction transaction) {
    return newExecutor(transaction, defaultExecutorType);
  }

  /**
   * 根据类型创建执行器，若启用二级缓存会外层包装 CachingExecutor，最后经过拦截器链
   *
   * @param transaction  事务对象
   * @param executorType 执行器类型：SIMPLE/REUSE/BATCH
   * @return 被插件代理后的执行器
   */
  public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
    executorType = executorType == null ? defaultExecutorType : executorType;
    Executor executor;
    // 按类型选择基础执行器
    if (ExecutorType.BATCH == executorType) {
      executor = new BatchExecutor(this, transaction);
    } else if (ExecutorType.REUSE == executorType) {
      executor = new ReuseExecutor(this, transaction);
    } else {
      executor = new SimpleExecutor(this, transaction);
    }
    // 启用二级缓存时用 CachingExecutor 装饰
    if (cacheEnabled) {
      executor = new CachingExecutor(executor);
    }
    // 应用所有拦截器
    executor = (Executor) interceptorChain.pluginAll(executor);
    return executor;
  }

  public void addKeyGenerator(String id, KeyGenerator keyGenerator) {
    keyGenerators.put(id, keyGenerator);
  }

  public Collection<String> getKeyGeneratorNames() {
    return keyGenerators.keySet();
  }

  public Collection<KeyGenerator> getKeyGenerators() {
    return keyGenerators.values();
  }

  public KeyGenerator getKeyGenerator(String id) {
    return keyGenerators.get(id);
  }

  public boolean hasKeyGenerator(String id) {
    return keyGenerators.containsKey(id);
  }

  public void addCache(Cache cache) {
    caches.put(cache.getId(), cache);
  }

  public Collection<String> getCacheNames() {
    return caches.keySet();
  }

  public Collection<Cache> getCaches() {
    return caches.values();
  }

  public Cache getCache(String id) {
    return caches.get(id);
  }

  public boolean hasCache(String id) {
    return caches.containsKey(id);
  }

  /**
   * 添加 ResultMap 并检查嵌套鉴别器场景下的关联
   *
   * @param rm 要注册的 ResultMap
   */
  public void addResultMap(ResultMap rm) {
    resultMaps.put(rm.getId(), rm);
    // 检查该 ResultMap 的 discriminator 是否引用了嵌套 ResultMap
    checkLocallyForDiscriminatedNestedResultMaps(rm);
    // 反向检查其他 ResultMap 是否通过 discriminator 引用了当前这个嵌套 ResultMap
    checkGloballyForDiscriminatedNestedResultMaps(rm);
  }

  public Collection<String> getResultMapNames() {
    return resultMaps.keySet();
  }

  public Collection<ResultMap> getResultMaps() {
    return resultMaps.values();
  }

  public ResultMap getResultMap(String id) {
    return resultMaps.get(id);
  }

  public boolean hasResultMap(String id) {
    return resultMaps.containsKey(id);
  }

  public void addParameterMap(ParameterMap pm) {
    parameterMaps.put(pm.getId(), pm);
  }

  public Collection<String> getParameterMapNames() {
    return parameterMaps.keySet();
  }

  public Collection<ParameterMap> getParameterMaps() {
    return parameterMaps.values();
  }

  public ParameterMap getParameterMap(String id) {
    return parameterMaps.get(id);
  }

  public boolean hasParameterMap(String id) {
    return parameterMaps.containsKey(id);
  }

  public void addMappedStatement(MappedStatement ms) {
    mappedStatements.put(ms.getId(), ms);
  }

  public Collection<String> getMappedStatementNames() {
    buildAllStatements();
    return mappedStatements.keySet();
  }

  public Collection<MappedStatement> getMappedStatements() {
    buildAllStatements();
    return mappedStatements.values();
  }

  public Collection<XMLStatementBuilder> getIncompleteStatements() {
    return incompleteStatements;
  }

  public void addIncompleteStatement(XMLStatementBuilder incompleteStatement) {
    incompleteStatements.add(incompleteStatement);
  }

  public Collection<CacheRefResolver> getIncompleteCacheRefs() {
    return incompleteCacheRefs;
  }

  public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef) {
    incompleteCacheRefs.add(incompleteCacheRef);
  }

  public Collection<ResultMapResolver> getIncompleteResultMaps() {
    return incompleteResultMaps;
  }

  public void addIncompleteResultMap(ResultMapResolver resultMapResolver) {
    incompleteResultMaps.add(resultMapResolver);
  }

  public void addIncompleteMethod(MethodResolver builder) {
    incompleteMethods.add(builder);
  }

  public Collection<MethodResolver> getIncompleteMethods() {
    return incompleteMethods;
  }

  /**
   * 根据 id 获取 MappedStatement（首次访问会触发挂起语句的构建）
   *
   * @param id 全限定语句 id
   * @return MappedStatement 实例
   */
  public MappedStatement getMappedStatement(String id) {
    return this.getMappedStatement(id, true);
  }

  /**
   * 根据 id 获取 MappedStatement
   *
   * @param id                           全限定语句 id
   * @param validateIncompleteStatements 是否先构建挂起的语句（确保语句完整性）
   * @return MappedStatement 实例
   */
  public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
    if (validateIncompleteStatements) {
      buildAllStatements();
    }
    return mappedStatements.get(id);
  }

  public Map<String, XNode> getSqlFragments() {
    return sqlFragments;
  }

  /**
   * 注册拦截器到拦截器链
   *
   * @param interceptor 拦截器实例
   */
  public void addInterceptor(Interceptor interceptor) {
    interceptorChain.addInterceptor(interceptor);
  }

  /**
   * 扫描指定包，注册所有 superType 子类/实现的 Mapper 接口
   *
   * @param packageName 包名
   * @param superType   父类型过滤
   */
  public void addMappers(String packageName, Class<?> superType) {
    mapperRegistry.addMappers(packageName, superType);
  }

  /**
   * 扫描指定包下的所有接口并注册为 Mapper
   *
   * @param packageName 包名
   */
  public void addMappers(String packageName) {
    mapperRegistry.addMappers(packageName);
  }

  /**
   * 注册单个 Mapper 接口（会触发 MapperAnnotationBuilder 解析注解与同名 XML）
   *
   * @param type Mapper 接口类型
   * @param <T>  Mapper 接口泛型
   */
  public <T> void addMapper(Class<T> type) {
    mapperRegistry.addMapper(type);
  }

  /**
   * 获取 Mapper 接口的代理实例
   *
   * @param type       Mapper 接口类型
   * @param sqlSession 当前 SqlSession
   * @param <T>        Mapper 接口泛型
   * @return Mapper 代理对象
   */
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    return mapperRegistry.getMapper(type, sqlSession);
  }

  /**
   * 判断指定的 Mapper 接口是否已注册
   *
   * @param type Mapper 接口类型
   * @return 已注册返回 true
   */
  public boolean hasMapper(Class<?> type) {
    return mapperRegistry.hasMapper(type);
  }

  /**
   * 判断是否存在指定 id 的 MappedStatement（首次访问会触发挂起语句的构建）
   *
   * @param statementName 语句 id
   * @return 存在返回 true
   */
  public boolean hasStatement(String statementName) {
    return hasStatement(statementName, true);
  }

  /**
   * 判断是否存在指定 id 的 MappedStatement
   *
   * @param statementName                语句 id
   * @param validateIncompleteStatements 是否先构建挂起的语句
   * @return 存在返回 true
   */
  public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {
    if (validateIncompleteStatements) {
      buildAllStatements();
    }
    return mappedStatements.containsKey(statementName);
  }

  /**
   * 记录 cache-ref 关系（key 引用 value 所属命名空间的缓存）
   *
   * @param namespace           引用方命名空间
   * @param referencedNamespace 被引用的命名空间
   */
  public void addCacheRef(String namespace, String referencedNamespace) {
    cacheRefMap.put(namespace, referencedNamespace);
  }

  /*
   * Parses all the unprocessed statement nodes in the cache. It is recommended
   * to call this method once all the mappers are added as it provides fail-fast
   * statement validation.
   */
  protected void buildAllStatements() {
    // 优先解析挂起的 ResultMap（其他元素可能依赖它）
    parsePendingResultMaps();
    // 解析挂起的 cache-ref（依赖被引用的 cache 已存在）
    if (!incompleteCacheRefs.isEmpty()) {
      synchronized (incompleteCacheRefs) {
        incompleteCacheRefs.removeIf(x -> x.resolveCacheRef() != null);
      }
    }
    // 解析挂起的 XML Statement
    if (!incompleteStatements.isEmpty()) {
      synchronized (incompleteStatements) {
        incompleteStatements.removeIf(x -> {
          x.parseStatementNode();
          return true;
        });
      }
    }
    // 解析挂起的注解方法
    if (!incompleteMethods.isEmpty()) {
      synchronized (incompleteMethods) {
        incompleteMethods.removeIf(x -> {
          x.resolve();
          return true;
        });
      }
    }
  }

  /**
   * 循环解析挂起的 ResultMap，直到无法再推进为止；若仍有无法解析的则抛出异常
   */
  private void parsePendingResultMaps() {
    if (incompleteResultMaps.isEmpty()) {
      return;
    }
    synchronized (incompleteResultMaps) {
      boolean resolved;
      IncompleteElementException ex = null;
      // 循环解析：每轮尝试所有未解析项，只要有进展就继续下一轮
      do {
        resolved = false;
        Iterator<ResultMapResolver> iterator = incompleteResultMaps.iterator();
        while (iterator.hasNext()) {
          try {
            iterator.next().resolve();
            iterator.remove();
            resolved = true;
          } catch (IncompleteElementException e) {
            // 仍有依赖未就绪，留到下一轮
            ex = e;
          }
        }
      } while (resolved);
      // 无��推进且仍有未解析项，抛出最后一次遇到的异常
      if (!incompleteResultMaps.isEmpty() && ex != null) {
        // At least one result map is unresolvable.
        throw ex;
      }
    }
  }

  /**
   * Extracts namespace from fully qualified statement id.
   *
   * @param statementId the statement id
   * @return namespace or null when id does not contain period.
   */
  protected String extractNamespace(String statementId) {
    int lastPeriod = statementId.lastIndexOf('.');
    return lastPeriod > 0 ? statementId.substring(0, lastPeriod) : null;
  }

  /**
   * 若新增的 rm 是嵌套 ResultMap，反向检查已注册的 ResultMap 中有哪些通过 discriminator 引用了它，
   * 将引用方也标记为包含嵌套映射
   */
  // Slow but a one time cost. A better solution is welcome.
  protected void checkGloballyForDiscriminatedNestedResultMaps(ResultMap rm) {
    if (rm.hasNestedResultMaps()) {
      for (Map.Entry<String, ResultMap> entry : resultMaps.entrySet()) {
        Object value = entry.getValue();
        if (value instanceof ResultMap) {
          ResultMap entryResultMap = (ResultMap) value;
          if (!entryResultMap.hasNestedResultMaps() && entryResultMap.getDiscriminator() != null) {
            Collection<String> discriminatedResultMapNames = entryResultMap.getDiscriminator().getDiscriminatorMap().values();
            if (discriminatedResultMapNames.contains(rm.getId())) {
              entryResultMap.forceNestedResultMaps();
            }
          }
        }
      }
    }
  }

  /**
   * 检查新注册的 rm 其 discriminator 引用的 ResultMap 是否为嵌套映射，
   * 若是则将 rm 也标记为包含嵌套映射
   */
  // Slow but a one time cost. A better solution is welcome.
  protected void checkLocallyForDiscriminatedNestedResultMaps(ResultMap rm) {
    if (!rm.hasNestedResultMaps() && rm.getDiscriminator() != null) {
      for (Map.Entry<String, String> entry : rm.getDiscriminator().getDiscriminatorMap().entrySet()) {
        String discriminatedResultMapName = entry.getValue();
        if (hasResultMap(discriminatedResultMapName)) {
          ResultMap discriminatedResultMap = resultMaps.get(discriminatedResultMapName);
          if (discriminatedResultMap.hasNestedResultMaps()) {
            rm.forceNestedResultMaps();
            break;
          }
        }
      }
    }
  }

  /**
   * 严格 Map - 禁止 key 重复 put，支持通过短名（简名）访问带点分隔的全限定 key；
   * 若同一短名对应多个全限定 key，访问短名会抛出 Ambiguity 异常
   */
  protected static class StrictMap<V> extends ConcurrentHashMap<String, V> {

    private static final long serialVersionUID = -4950446264854982944L;
    /**
     * 用于错误提示的集合名称（如 "Mapped Statements collection"）
     */
    private final String name;
    /**
     * 冲突时用于生成错误提示的函数
     */
    private BiFunction<V, V, String> conflictMessageProducer;

    public StrictMap(String name, int initialCapacity, float loadFactor) {
      super(initialCapacity, loadFactor);
      this.name = name;
    }

    public StrictMap(String name, int initialCapacity) {
      super(initialCapacity);
      this.name = name;
    }

    public StrictMap(String name) {
      super();
      this.name = name;
    }

    public StrictMap(String name, Map<String, ? extends V> m) {
      super(m);
      this.name = name;
    }

    /**
     * Assign a function for producing a conflict error message when contains value with the same key.
     * <p>
     * function arguments are 1st is saved value and 2nd is target value.
     *
     * @param conflictMessageProducer A function for producing a conflict error message
     * @return a conflict error message
     * @since 3.5.0
     */
    public StrictMap<V> conflictMessageProducer(BiFunction<V, V, String> conflictMessageProducer) {
      this.conflictMessageProducer = conflictMessageProducer;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V put(String key, V value) {
      // 重复 key 直接抛异常（区别于普通 Map）
      if (containsKey(key)) {
        throw new IllegalArgumentException(name + " already contains value for " + key
          + (conflictMessageProducer == null ? "" : conflictMessageProducer.apply(super.get(key), value)));
      }
      // 带点的 key 额外注册一个短名映射，方便通过简名访问
      if (key.contains(".")) {
        final String shortKey = getShortName(key);
        if (super.get(shortKey) == null) {
          super.put(shortKey, value);
        } else {
          // 短名冲突时存入 Ambiguity 占位，访问时抛出明确错误
          super.put(shortKey, (V) new Ambiguity(shortKey));
        }
      }
      return super.put(key, value);
    }

    @Override
    public boolean containsKey(Object key) {
      if (key == null) {
        return false;
      }

      return super.get(key) != null;
    }

    @Override
    public V get(Object key) {
      V value = super.get(key);
      // 不存在直接抛异常，避免调用方拿到 null 后产生模糊错误
      if (value == null) {
        throw new IllegalArgumentException(name + " does not contain value for " + key);
      }
      // 通过短名访问但存在多个全限定 key 匹配时，提示使用全限定名
      if (value instanceof Ambiguity) {
        throw new IllegalArgumentException(((Ambiguity) value).getSubject() + " is ambiguous in " + name
          + " (try using the full name including the namespace, or rename one of the entries)");
      }
      return value;
    }

    /**
     * 表示短名存在歧义的占位对象
     */
    protected static class Ambiguity {
      /**
       * 产生歧义的短名
       */
      private final String subject;

      public Ambiguity(String subject) {
        this.subject = subject;
      }

      public String getSubject() {
        return subject;
      }
    }

    /**
     * 取全限定 key 的最后一段作为短名
     */
    private String getShortName(String key) {
      final String[] keyParts = key.split("\\.");
      return keyParts[keyParts.length - 1];
    }
  }

}
