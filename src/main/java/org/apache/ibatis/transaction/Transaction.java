/**
 *    Copyright 2009-2016 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
 * 事务接口，封装数据库连接的生命周期管理。
 * 负责连接的创建、准备、提交/回滚和关闭操作。
 *
 * @author Clinton Begin
 */
public interface Transaction {

  /**
   * 获取内部数据库连接。
   * @return 数据库连接对象
   * @throws SQLException 获取连接失败时抛出
   */
  Connection getConnection() throws SQLException;

  /**
   * 提交内部数据库连接的事务。
   * @throws SQLException 提交失败时抛出
   */
  void commit() throws SQLException;

  /**
   * 回滚内部数据库连接的事务。
   * @throws SQLException 回滚失败时抛出
   */
  void rollback() throws SQLException;

  /**
   * 关闭内部数据库连接。
   * @throws SQLException 关闭失败时抛出
   */
  void close() throws SQLException;

  /**
   * 获取事务超时时间。
   * @return 超时时间（毫秒），未设置时返回 null
   * @throws SQLException 获取超时时间失败时抛出
   */
  Integer getTimeout() throws SQLException;

}
