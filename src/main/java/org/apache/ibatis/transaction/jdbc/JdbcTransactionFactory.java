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
package org.apache.ibatis.transaction.jdbc;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;

/**
 * JdbcTransaction 工厂类，用于创建 JDBC 事务实例。
 *
 * @author Clinton Begin
 * @see JdbcTransaction
 */
public class JdbcTransactionFactory implements TransactionFactory {

  /**
   * 设置配置属性，当前实现为空。
   * 预留用于未来扩展，支持从配置中读取事务相关参数。
   *
   * @param props 配置属性
   */
  @Override
  public void setProperties(Properties props) {
  }

  /**
   * 使用已有的数据库连接创建事务。
   *
   * @param conn 数据库连接，事务将基于此连接创建
   * @return JdbcTransaction 实例
   */
  @Override
  public Transaction newTransaction(Connection conn) {
    return new JdbcTransaction(conn);
  }

  /**
   * 从数据源创建事务，支持设置隔离级别和自动提交模式。
   *
   * @param ds         数据源，用于获取数据库连接
   * @param level      事务隔离级别
   * @param autoCommit 是否自动提交
   * @return JdbcTransaction 实例
   */
  @Override
  public Transaction newTransaction(DataSource ds, TransactionIsolationLevel level, boolean autoCommit) {
    return new JdbcTransaction(ds, level, autoCommit);
  }
}
