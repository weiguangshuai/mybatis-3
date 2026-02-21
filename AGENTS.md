# 仓库指南

## 项目结构与模块组织
- 核心代码位于 `src/main/java/org/apache/ibatis/**`。
- 测试代码位于 `src/test/java/**`，测试相关的 mapper XML、SQL 文件通常与测试类同目录管理。
- 文档源码在 `src/site`（Maven site/xdoc），补充文档在 `docs/`。
- 构建与 CI 相关文件在 `.mvn/`、`travis/` 及根目录（`pom.xml`、`mvnw`、`mvnw.cmd`）。
- 不要提交生成产物，如 `target/`、本地 Derby 文件或本地 Maven 缓存目录。

## 构建、测试与开发命令
- `./mvn -P release clean test`（Windows 用 `mvn -P release clean test`）：完整编译并运行测试。
- `./mvn -P release -Dtest=org.apache.ibatis.reflection.ReflectorTest test`：只运行单个测试类。
- `./mvn -P release -DskipTests package`：跳过测试快速打包。
- `./mvn -P release site`：基于 `src/site` 生成站点文档。
- `./mvn -P release -Dmaven.surefire.excludeGroups= test`：按需包含慢速测试分组。
- 注意：构建时如果找不到本地仓库，使用maven自带命令去查找，不要把本地仓库指定到当前项目空间，我有自己的全局本地仓库

## 代码风格与命名规范
- 遵循现有 Java 风格：2 空格缩进、同一行大括号、方法保持简洁。
- 包名使用 `org.apache.ibatis.*`；类名 `UpperCamelCase`，方法/字段 `lowerCamelCase`。
- 新增源码文件需保留 Apache 2.0 License 头（见 `CONTRIBUTING.md`）。
- 主代码优先保持向后兼容；构建流程会做 Java 兼容性检查。

## 测试规范
- 主要使用 JUnit 4（`org.junit.Test`），部分场景会使用 AssertJ、Mockito。
- 行为变更必须补充/更新测试，修复缺陷时优先增加回归测试。
- 测试命名建议体现行为，如 `shouldXxx...`、`testXxx...`。
- 慢速测试通过 `org.apache.ibatis.test.SlowTests` 分组，默认执行可能排除该组。

## 提交与 Pull Request 规范
- 提交信息保持简短、祈使语气（如：`Fix ...`、`Update ...`、`Upgrade ...`）。
- 相关改动请关联 Issue（如 `#1234`、`gh-1234`、`Fixes #1234`）。
- PR 至少应包含：问题背景、解决思路、影响范围、测试证明。
- 若涉及行为或公开 API 变化，请同步更新文档/配置并在 PR 说明中标注。
