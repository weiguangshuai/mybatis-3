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
 *
 * @see JdbcTransaction
 */
public class JdbcTransactionFactory implements TransactionFactory {

  /** 是否跳过关闭连接时设置自动提交的操作 */
  private boolean skipSetAutoCommitOnClose;

  /**
   * 从配置属性中读取事务工厂的设置参数。
   *
   * @param props 配置属性集合
   */
  @Override
  public void setProperties(Properties props) {
    // 空属性直接返回，避免后续空指针
    if (props == null) {
      return;
    }
    String value = props.getProperty("skipSetAutoCommitOnClose");
    // 解析 skipSetAutoCommitOnClose 配置项
    if (value != null) {
      skipSetAutoCommitOnClose = Boolean.parseBoolean(value);
    }
  }

  /**
   * 使用已存在的数据库连接创建事务实例。
   *
   * @param conn 已有 JDBC 连接
   * @return JdbcTransaction 事务对象
   */
  @Override
  public Transaction newTransaction(Connection conn) {
    return new JdbcTransaction(conn);
  }

  /**
   * 从数据源创建事务实例，支持设置事务隔离级别和自动提交模式。
   *
   * @param ds           数据源
   * @param level        事务隔离级别
   * @param autoCommit   是否自动提交
   * @return JdbcTransaction 事务对象
   */
  @Override
  public Transaction newTransaction(DataSource ds, TransactionIsolationLevel level, boolean autoCommit) {
    return new JdbcTransaction(ds, level, autoCommit, skipSetAutoCommitOnClose);
  }
}
