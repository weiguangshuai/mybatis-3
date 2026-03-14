/**
 *    Copyright 2009-2017 the original author or authors.
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
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionException;

/**
 * {@link Transaction} that makes use of the JDBC commit and rollback facilities directly.
 * It relies on the connection retrieved from the dataSource to manage the scope of the transaction.
 * Delays connection retrieval until getConnection() is called.
 * Ignores commit or rollback requests when autocommit is on.
 *
 * @author Clinton Begin
 *
 * @see JdbcTransactionFactory
 */
public class JdbcTransaction implements Transaction {

  private static final Log log = LogFactory.getLog(JdbcTransaction.class);

  /** 数据库连接对象 */
  protected Connection connection;
  /** 数据源，用于获取数据库连接 */
  protected DataSource dataSource;
  /** 事务隔离级别 */
  protected TransactionIsolationLevel level;
  // MEMO: We are aware of the typo. See #941
  /** 是否启用自动提交 */
  protected boolean autoCommmit;

  /**
   * 使用数据源初始化事务。
   *
   * @param ds 数据源
   * @param desiredLevel 事务隔离级别
   * @param desiredAutoCommit 是否自动提交
   */
  public JdbcTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit) {
    dataSource = ds;
    level = desiredLevel;
    autoCommmit = desiredAutoCommit;
  }

  /**
   * 使用已存在的连接初始化事务。
   *
   * @param connection 数据库连接
   */
  public JdbcTransaction(Connection connection) {
    this.connection = connection;
  }

  /**
   * 获取数据库连接，如未初始化则先打开连接。
   *
   * @return 数据库连接
   * @throws SQLException 获取连接失败时抛出
   */
  @Override
  public Connection getConnection() throws SQLException {
    // 延迟获取连接，仅在首次使用时创建
    if (connection == null) {
      openConnection();
    }
    return connection;
  }

  /**
   * 提交事务。只有在连接存在且非自动提交模式下才执行提交操作。
   *
   * @throws SQLException 提交失败时抛出
   */
  @Override
  public void commit() throws SQLException {
    // 非自动提交模式下才需要手动提交
    if (connection != null && !connection.getAutoCommit()) {
      if (log.isDebugEnabled()) {
        log.debug("Committing JDBC Connection [" + connection + "]");
      }
      connection.commit();
    }
  }

  /**
   * 回滚事务。只有在连接存在且非自动提交模式下才执行回滚操作。
   *
   * @throws SQLException 回滚失败时抛出
   */
  @Override
  public void rollback() throws SQLException {
    // 非自动提交模式下才需要手动回滚
    if (connection != null && !connection.getAutoCommit()) {
      if (log.isDebugEnabled()) {
        log.debug("Rolling back JDBC Connection [" + connection + "]");
      }
      connection.rollback();
    }
  }

  /**
   * 关闭事务，释放数据库连接。关闭前会重置自动提交状态。
   *
   * @throws SQLException 关闭连接失败时抛出
   */
  @Override
  public void close() throws SQLException {
    if (connection != null) {
      // 关闭前重置自动提交状态，避免某些数据库报错
      resetAutoCommit();
      if (log.isDebugEnabled()) {
        log.debug("Closing JDBC Connection [" + connection + "]");
      }
      connection.close();
    }
  }

  /**
   * 设置连接是否自动提交。
   *
   * @param desiredAutoCommit 是否自动提交
   */
  protected void setDesiredAutoCommit(boolean desiredAutoCommit) {
    try {
      // 仅在当前值与目标值不同时才修改
      if (connection.getAutoCommit() != desiredAutoCommit) {
        if (log.isDebugEnabled()) {
          log.debug("Setting autocommit to " + desiredAutoCommit + " on JDBC Connection [" + connection + "]");
        }
        connection.setAutoCommit(desiredAutoCommit);
      }
    } catch (SQLException e) {
      // 驱动实现极差时才会失败，这种情况无法恢复
      throw new TransactionException("Error configuring AutoCommit.  "
          + "Your driver may not support getAutoCommit() or setAutoCommit(). "
          + "Requested setting: " + desiredAutoCommit + ".  Cause: " + e, e);
    }
  }

  /**
   * 重置自动提交状态为true。部分数据库要求关闭连接前必须提交或回滚事务，
   * 设置为自动提交可作为变通方案。
   */
  protected void resetAutoCommit() {
    try {
      if (!connection.getAutoCommit()) {
        // MyBatis does not call commit/rollback on a connection if just selects were performed.
        // Some databases start transactions with select statements
        // and they mandate a commit/rollback before closing the connection.
        // A workaround is setting the autocommit to true before closing the connection.
        // Sybase throws an exception here.
        if (log.isDebugEnabled()) {
          log.debug("Resetting autocommit to true on JDBC Connection [" + connection + "]");
        }
        connection.setAutoCommit(true);
      }
    } catch (SQLException e) {
      if (log.isDebugEnabled()) {
        log.debug("Error resetting autocommit to true "
          + "before closing the connection.  Cause: " + e);
      }
    }
  }

  /**
   * 打开数据库连接，设置事务隔离级别和自动提交模式。
   *
   * @throws SQLException 获取连接或设置属性失败时抛出
   */
  protected void openConnection() throws SQLException {
    if (log.isDebugEnabled()) {
      log.debug("Opening JDBC Connection");
    }
    // 从数据源获取连接
    connection = dataSource.getConnection();
    // 设置事务隔离级别
    if (level != null) {
      connection.setTransactionIsolation(level.getLevel());
    }
    // 设置自动提交模式
    setDesiredAutoCommit(autoCommmit);
  }

  /**
   * 获取事务超时时间。JDBC事务不支持超时设置，返回null。
   *
   * @return 超时时间（毫秒），JDBC事务不支持返回null
   * @throws SQLException 获取超时失败时抛出
   */
  @Override
  public Integer getTimeout() throws SQLException {
    return null;
  }
  
}
