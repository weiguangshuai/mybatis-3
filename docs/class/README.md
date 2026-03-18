# MyBatis 类图文档

本目录包含 MyBatis 核心模块的 UML 类图，使用 PlantUML 语法编写。

## 类图列表

### 1. [01-Mapper绑定类图.puml](01-Mapper绑定类图.puml)
**Mapper 绑定类图**

展示了 MyBatis 中 Mapper 接口绑定的核心类及其关系：

- **MapperRegistry** - Mapper 注册中心，管理所有 Mapper 接口的注册与获取
- **MapperProxyFactory** - Mapper 代理工厂，负责创建 Mapper 接口的动态代理对象
- **MapperProxy** - Mapper 动态代理，实现 `InvocationHandler` 接口，拦截方法调用
- **MapperMethod** - Mapper 方法执行器，封装 SQL 命令执行逻辑
- **SqlSession** - 核心 API 接口，提供数据库操作方法
- **Configuration** - 全局配置中心

## 类图说明

### Mapper 绑定流程

```
1. Configuration.getMapper(type, sqlSession)
      ↓
2. MapperRegistry.getMapper(type, sqlSession)
      ↓
3. MapperProxyFactory.newInstance(sqlSession)
      ↓
4. 创建 MapperProxy 动态代理对象
      ↓
5. 用户调用 mapper.method()
      ↓
6. MapperProxy.invoke() 拦截方法
      ↓
7. MapperMethod.execute() 执行 SQL
      ↓
8. SqlSession 完成数据库操作
```

### 核心职责

| 类 | 职责 |
|---|---|
| MapperRegistry | 管理 Mapper 接口注册 |
| MapperProxyFactory | 创建动态代理 |
| MapperProxy | 拦截方法调用 |
| MapperMethod | 执行 SQL 命令 |
| SqlSession | 提供数据库操作 API |
| Configuration | 全局配置管理 |

## 使用方法

### 查看类图

可以使用以下工具查看 PlantUML 类图：

1. **VS Code 插件**
   - 安装 "PlantUML" 插件
   - 右键选择 "Preview Current PlantUML File"

2. **IntelliJ IDEA 插件**
   - 安装 "PlantUML integration" 插件

3. **在线查看**
   - 访问 https://www.plantuml.com/plantuml/
