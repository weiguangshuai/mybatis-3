# MyBatis 时序图文档

本目录包含 MyBatis 核心模块的 UML 时序图，使用 PlantUML 语法编写。

## 时序图列表

### 1. [01-SqlSession查询流程时序图.puml](01-SqlSession查询流程时序图.puml)
**SqlSession 查询流程时序图**

展示了从用户调用 `selectOne` 或 `selectList` 开始，到最终返回结果对象的完整流程，包括：
- Configuration 获取 MappedStatement
- CachingExecutor 二级缓存处理
- BaseExecutor 一级缓存处理
- SimpleExecutor 实际 SQL 执行
- StatementHandler 参数设置
- ResultSetHandler 结果映射

### 2. [02-Mapper绑定流程时序图.puml](02-Mapper绑定流程时序图.puml)
**Mapper 绑定流程时序图**

展示了 MyBatis 如何将 Mapper 接口绑定到动态代理对象，包括：
- SqlSession 获取 Mapper
- MapperRegistry 创建 MapperProxyFactory
- MapperProxy 动态代理拦截方法调用
- MapperMethod 方法执行器

### 3. [03-缓存机制时序图.puml](03-缓存机制时序图.puml)
**缓存机制时序图**

详细展示了一级缓存（本地缓存）和二级缓存（全局缓存）的工作原理：
- 一级缓存的生命周期（与 SqlSession 相同）
- 二级缓存的跨 SqlSession 共享
- 缓存的查询和更新流程
- 事务提交时的缓存同步

### 4. [04-初始化流程时序图.puml](04-初始化流程时序图.puml)
**初始化流程时序图**

展示了 MyBatis 从配置文件到 SqlSessionFactory 的完整初始化过程：
- SqlSessionFactoryBuilder 构建过程
- XMLConfigBuilder 解析 mybatis-config.xml
- XMLMapperBuilder 解析 Mapper.xml
- MappedStatement、ResultMap 等对象的创建

### 5. [05-XML配置解析时序图.puml](05-XML配置解析时序图.puml)
**XML 配置解析时序图**

详细展示了 MyBatis 如何解析 XML 配置文件：
- XMLConfigBuilder 解析主配置
- XMLMapperBuilder 解析 Mapper.xml
- SQL 语句节点的解析过程
- ResultMap 结果映射的解析
- 动态 SQL 标签的处理

### 6. [06-动态SQL处理时序图.puml](06-动态SQL处理时序图.puml)
**动态SQL处理时序图**

展示了 MyBatis 动态 SQL 的解析和执行过程：
- 动态 SQL 节点的解析（if, foreach, where, set, trim）
- OGNL 表达式求值
- `${}` 变量的替换
- 参数处理和结果映射

### 7. [07-整体架构时序图.puml](07-整体架构时序图.puml)
**整体架构时序图**

以宏观视角展示了 MyBatis 各模块之间的协作关系：
- 客户端层（用户代码层）
- 核心层（SqlSession/Configuration）
- 执行器层（Executor）
- 语句处理层（StatementHandler）
- JDBC 层
- 缓存层

## 核心流程总结

```
用户代码
    ↓
SqlSession.getMapper() → 获取Mapper代理对象
    ↓
MapperProxy.invoke() → 拦截方法调用
    ↓
MapperMethod.execute() → 执行SQL命令
    ↓
SqlSession.select/update/insert/delete
    ↓
CachingExecutor → 二级缓存处理
    ↓
BaseExecutor → 一级缓存处理
    ↓
SimpleExecutor/ReuseExecutor/BatchExecutor → 实际执行
    ↓
StatementHandler → 参数处理 + SQL执行
    ↓
ParameterHandler → 设置SQL参数
    ↓
JDBC PreparedStatement → 执行SQL
    ↓
ResultSetHandler → 结果映射
    ↓
返回结果对象
```

## 使用方法

### 查看时序图

可以使用以下工具查看 PlantUML 时序图：

1. **VS Code 插件**
   - 安装 "PlantUML" 插件
   - 右键选择 "Preview Current PlantUML File"

2. **IntelliJ IDEA 插件**
   - 安装 "PlantUML integration" 插件

3. **在线查看**
   - 访问 https://www.plantuml.com/plantuml/

### 生成图片

使用 PlantUML 命令行工具生成图片：

```bash
# 安装 PlantUML
java -jar plantuml.jar -checkonly

# 生成 PNG 图片
java -jar plantuml.jar -tpng 01-SqlSession查询流程时序图.puml
```

## 时序图说明

每个时序图都包含以下内容：

1. **参与者说明** - 每个参与者下方标注其作用
2. **分支处理** - 使用 `alt/else` 展示不同分支
3. **详细注释** - 每个关键步骤都有中文说明
4. **流程概述** - 时序图结尾有流程总结
