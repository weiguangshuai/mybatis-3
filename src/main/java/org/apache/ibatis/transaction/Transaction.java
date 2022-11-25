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
package org.apache.ibatis.transaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 事务接口，用于封装数据库连接的创建、准备、提交/回滚和关闭等生命周期管理。
 *
 * @author Clinton Begin
 */
public interface Transaction {

  /**
   * 获取底层数据库连接。
   *
   * @return 数据库连接对象
   * @throws SQLException 数据库访问异常
   */
  Connection getConnection() throws SQLException;

  /**
   * 提交事务。
   *
   * @throws SQLException 数据库访问异常
   */
  void commit() throws SQLException;

  /**
   * 回滚事务。
   *
   * @throws SQLException 数据库访问异常
   */
  void rollback() throws SQLException;

  /**
   * 关闭事务，释放底层数据库连接。
   *
   * @throws SQLException 数据库访问异常
   */
  void close() throws SQLException;

  /**
   * 获取事务超时时间（秒）。
   * 如果未设置超时，返回 null。
   *
   * @return 超时秒数，未设置则返回 null
   * @throws SQLException 数据库访问异常
   */
  Integer getTimeout() throws SQLException;

}
