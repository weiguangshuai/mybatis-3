# MyBatis 类图文档

本目录存放 MyBatis 核心模块的 PlantUML 类图，便于结合源码理解关键对象关系与调用链。

## 类图列表

### 1. [01-Mapper绑定类图.puml](01-Mapper绑定类图.puml)
说明 Mapper 注册、代理创建与 `MapperMethod` 执行之间的关系，适合阅读 `org.apache.ibatis.binding` 包时对照查看。

核心关注点：
- `MapperRegistry` 如何注册并返回 Mapper 代理
- `MapperProxyFactory` / `MapperProxy` 如何拦截接口调用
- `MapperMethod` 如何把接口方法映射到 `SqlSession`

### 2. [02-SqlSession查询流程类图.puml](02-SqlSession查询流程类图.puml)
说明 `SqlSession` 默认查询链路，覆盖 `selectList()` 到 `Executor`、`StatementHandler`、`ParameterHandler`、`ResultSetHandler` 的核心协作关系。

核心关注点：
- `DefaultSqlSession` 如何定位 `MappedStatement`
- `CachingExecutor`、`BaseExecutor`、`SimpleExecutor` 的职责分层
- `RoutingStatementHandler` 如何落到默认的 `PreparedStatementHandler`
- `DefaultParameterHandler` 与 `DefaultResultSetHandler` 如何完成参数绑定和结果映射

### 3. [03-缓存机制类图.puml](03-缓存机制类图.puml)
说明 MyBatis 一级缓存、二级缓存、装饰器链与事务缓冲之间的关系，适合阅读 `org.apache.ibatis.cache`、`org.apache.ibatis.executor`、`org.apache.ibatis.mapping.CacheBuilder` 时对照查看。

核心关注点：
- `BaseExecutor` 的本地缓存与 `LocalCacheScope`
- `CachingExecutor`、`TransactionalCacheManager`、`TransactionalCache` 如何管理二级缓存
- `CacheBuilder` 如何构建 `PerpetualCache` 与装饰器链
- `MappedStatement.cache`、`useCache`、`flushCacheRequired` 如何决定缓存行为

### 4. [04-初始化相关类图.puml](04-初始化相关类图.puml)
说明 MyBatis 从 `SqlSessionFactoryBuilder` 启动，到 `Configuration`、`Environment`、`DefaultSqlSessionFactory` 完成装配的主路径。

核心关注点：
- `SqlSessionFactoryBuilder` 如何驱动 `XMLConfigBuilder.parse()`
- `XMLConfigBuilder` 如何装配 `Configuration` 与 `Environment`
- `DefaultSqlSessionFactory` 为什么是初始化阶段的最终产物

### 5. [05-XML配置解析类图.puml](05-XML配置解析类图.puml)
说明 `mybatis-config.xml` 与 Mapper XML 的解析协作关系，适合对照 `org.apache.ibatis.builder.xml`、`org.apache.ibatis.parsing` 阅读。

核心关注点：
- `XPathParser` / `XNode` 如何提供统一的 XML 读取能力
- `XMLConfigBuilder` 如何把 mapper 注册继续分发给 `XMLMapperBuilder`
- `XMLStatementBuilder` 如何展开 `<include>`、解析 `selectKey`、注册 `MappedStatement`

### 6. [06-动态SQL处理类图.puml](06-动态SQL处理类图.puml)
说明动态 SQL 从 `XMLLanguageDriver`、`XMLScriptBuilder`、`SqlNode` 树到 `BoundSql` 的生成过程，适合阅读 `org.apache.ibatis.scripting.xmltags`、`org.apache.ibatis.builder.SqlSourceBuilder`。

核心关注点：
- `XMLScriptBuilder` 如何把 XML 标签转成 `SqlNode` 树
- `DynamicSqlSource` 与 `RawSqlSource` 的分工差异
- `SqlSourceBuilder` 如何把 `#{}` 解析成 `?` 并生成参数映射

## 阅读建议

1. 先看类之间的依赖方向，再回到对应源码包核对方法实现。
2. 初始化主线优先阅读 `org.apache.ibatis.session.SqlSessionFactoryBuilder`、`org.apache.ibatis.builder.xml.XMLConfigBuilder`、`org.apache.ibatis.mapping.Environment`。
3. 查询链路优先阅读 `org.apache.ibatis.session.defaults`、`org.apache.ibatis.executor`、`org.apache.ibatis.executor.statement`。
4. 缓存机制优先阅读 `org.apache.ibatis.cache`、`org.apache.ibatis.mapping.CacheBuilder`、`org.apache.ibatis.executor.CachingExecutor`。
5. 动态 SQL 优先阅读 `org.apache.ibatis.scripting.xmltags`、`org.apache.ibatis.scripting.defaults.RawSqlSource`、`org.apache.ibatis.builder.SqlSourceBuilder`。
6. 使用 VS Code PlantUML、IntelliJ PlantUML integration，或在线工具 https://www.plantuml.com/plantuml/ 预览图形。
