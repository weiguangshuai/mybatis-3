# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

MyBatis 是一个 Java SQL 映射框架，使得面向对象的应用程序可以更方便地使用关系型数据库。MyBatis 通过 XML 描述符或注解将对象与存储过程或 SQL 语句关联起来。

- 版本：3.4.7-SNAPSHOT
- 官方网站：http://mybatis.github.io/mybatis-3

## 构建与测试命令

```bash
# 完整编译并运行测试
./mvn -P release clean test

# 运行单个测试类
./mvn -P release -Dtest=org.apache.ibatis.reflection.ReflectorTest test

# 跳过测试快速打包
./mvn -P release -DskipTests package

# 生成站点文档
./mvn -P release site

# 排除慢速测试分组
./mvn -P release -Dmaven.surefile.excludeGroups= test
```

注意：Windows 下使用 `mvn` 代替 `./mvn`。

## 项目结构

```
src/main/java/org/apache/ibatis/        # 核心源码
src/test/java/                          # 测试代码
src/site/                               # Maven site/xdoc 文档源码
docs/                                   # 补充文档
pom.xml                                 # Maven 构建配置
```

## 核心包结构

- `annotations/` - MyBatis 注解定义
- `binding/` - Mapper 绑定机制
- `builder/` - XML/注解构建器
- `cache/` - 一级/二级缓存实现
- `cursor/` - 游标结果处理
- `datasource/` - 数据源连接池
- `executor/` - SQL 执行器
- `mapping/` - SQL 映射配置
- `session/` - SqlSession 会话管理
- `transaction/` - 事务管理
- `type/` - JDBC 类型转换处理器

## 开发规范

- 遵循现有 Java 风格：2 空格缩进、同一行大括号、方法保持简洁
- 包名：`org.apache.ibatis.*`
- 类名：`UpperCamelCase`，方法/字段：`lowerCamelCase`
- 新增源码文件需保留 Apache 2.0 License 头

## 测试规范

- 主要使用 JUnit 4（`org.junit.Test`）
- 部分场景使用 AssertJ、Mockito
- 慢速测试通过 `org.apache.ibatis.test.SlowTests` 分组标记

## 提交规范

- 提交信息简短、祈使语气（如：`Fix ...`、`Update ...`）
- 相关改动请关联 Issue（如 `#1234`、`gh-1234`）
- PR 至少应包含：问题背景、解决思路、影响范围、测试证明
