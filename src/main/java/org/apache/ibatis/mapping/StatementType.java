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
package org.apache.ibatis.mapping;

/**
 * SQL 语句类型枚举，定义 MyBatis 支持的三种 SQL 执行方式
 *
 * @author Clinton Begin
 */
public enum StatementType {
  /** 简单 SQL 语句，直接拼接参数 */
  STATEMENT,
  /** 预编译 SQL 语句，使用参数占位符 */
  PREPARED,
  /** 存储过程调用 */
  CALLABLE
}
