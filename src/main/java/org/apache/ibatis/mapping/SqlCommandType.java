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
 * SQL 命令类型枚举。
 * 表示 MyBatis 中可执行的 SQL 操作类型，用于区分不同的 Mapper 方法对应的 SQL 类别。
 *
 * @author Clinton Begin
 */
public enum SqlCommandType {
  /** 未知或未匹配的 SQL 类型 */
  UNKNOWN,
  /** 插入操作 */
  INSERT,
  /** 更新操作 */
  UPDATE,
  /** 删除操作 */
  DELETE,
  /** 查询操作 */
  SELECT,
  /** 刷新操作（如刷新语句缓存） */
  FLUSH
}
