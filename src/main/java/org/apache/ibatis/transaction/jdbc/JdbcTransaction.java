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

  /** 日志对象 */
  private static final Log log = LogFactory.getLog(JdbcTransaction.class);

  /** 数据库连接对象 */
  protected Connection connection;
  /** 数据源，用于获取连接 */
  protected DataSource dataSource;
  /** 事务隔离级别 */
  protected TransactionIsolationLevel level;
  /** 是否自动提交 */
  protected boolean autoCommit;
  /** 是否跳过关闭时的自动提交设置 */
  protected boolean skipSetAutoCommitOnClose;

  /**
   * 构造 JdbcTransaction。
   *
   * @param ds 数据源
   * @param desiredLevel 期望的事务隔离级别
   * @param desiredAutoCommit 期望的自动提交状态
   */
  public JdbcTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit) {
    this(ds, desiredLevel, desiredAutoCommit, false);
  }

  /**
   * 构造 JdbcTransaction。
   *
   * @param ds 数据源
   * @param desiredLevel 期望的事务隔离级别
   * @param desiredAutoCommit 期望的自动提交状态
   * @param skipSetAutoCommitOnClose 是否跳过关闭时的自动提交设置
   */
  public JdbcTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit, boolean skipSetAutoCommitOnClose) {
    dataSource = ds;
    level = desiredLevel;
    autoCommit = desiredAutoCommit;
    this.skipSetAutoCommitOnClose = skipSetAutoCommitOnClose;
  }

  /**
   * 使用已有的连接构造 JdbcTransaction。
   *
   * @param connection 已存在的数据库连接
   */
  public JdbcTransaction(Connection connection) {
    this.connection = connection;
  }

  /**
   * 获取数据库连接。如未连接则先从数据源获取连接。
   *
   * @return 数据库连接
   * @throws SQLException 获取连接失败时抛出
   */
  @Override
  public Connection getConnection() throws SQLException {
    // 延迟获取连接，仅在首次调用时创建
    if (connection == null) {
      openConnection();
    }
    return connection;
  }

  /**
   * 提交事务。仅在连接存在且非自动提交模式下执行提交操作。
   *
   * @throws SQLException 提交失败时抛出
   */
  @Override
  public void commit() throws SQLException {
    // 自动提交模式下无需手动提交
    if (connection != null && !connection.getAutoCommit()) {
      if (log.isDebugEnabled()) {
        log.debug("Committing JDBC Connection [" + connection + "]");
      }
      connection.commit();
    }
  }

  /**
   * 回滚事务。仅在连接存在且非自动提交模式下执行回滚操作。
   *
   * @throws SQLException 回滚失败时抛出
   */
  @Override
  public void rollback() throws SQLException {
    // 自动提交模式下无需手动回滚
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
      // 关闭前重置自动提交状态，避免影响后续使用该连接的代码
      resetAutoCommit();
      if (log.isDebugEnabled()) {
        log.debug("Closing JDBC Connection [" + connection + "]");
      }
      connection.close();
    }
  }

  /**
   * 设置目标自动提交状态。
   *
   * @param desiredAutoCommit 期望的自动提交状态
   */
  protected void setDesiredAutoCommit(boolean desiredAutoCommit) {
    try {
      // 仅在状态不一致时才设置，减少不必要的数据库调用
      if (connection.getAutoCommit() != desiredAutoCommit) {
        if (log.isDebugEnabled()) {
          log.debug("Setting autocommit to " + desiredAutoCommit + " on JDBC Connection [" + connection + "]");
        }
        connection.setAutoCommit(desiredAutoCommit);
      }
    } catch (SQLException e) {
      // 驱动实现问题导致失败，无法恢复
      throw new TransactionException("Error configuring AutoCommit.  "
          + "Your driver may not support getAutoCommit() or setAutoCommit(). "
          + "Requested setting: " + desiredAutoCommit + ".  Cause: " + e, e);
    }
  }

  /**
   * 重置自动提交状态为 true。部分数据库在执行 SELECT 语句后会开始事务，
   * 关闭连接前需要提交或回滚，否则可能导致连接泄漏。
   */
  protected void resetAutoCommit() {
    try {
      // 如果非手动设置且当前非自动提交，则重置为自动提交
      if (!skipSetAutoCommitOnClose && !connection.getAutoCommit()) {
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
      // 重置失败仅记录日志，不影响连接关闭
      if (log.isDebugEnabled()) {
        log.debug("Error resetting autocommit to true "
            + "before closing the connection.  Cause: " + e);
      }
    }
  }

  /**
   * 从数据源获取数据库连接，并设置事务隔离级别和自动提交状态。
   *
   * @throws SQLException 获取连接或设置属性失败时抛出
   */
  protected void openConnection() throws SQLException {
    if (log.isDebugEnabled()) {
      log.debug("Opening JDBC Connection");
    }
    // 从数据源获取新连接
    connection = dataSource.getConnection();
    // 设置事务隔离级别
    if (level != null) {
      connection.setTransactionIsolation(level.getLevel());
    }
    // 设置自动提交状态
    setDesiredAutoCommit(autoCommit);
  }

  /**
   * 获取事务超时时间。JDBC 事务本身不支持超时设置，返回 null。
   *
   * @return 超时时间（毫秒），当前始终返回 null
   * @throws SQLException 获取超时失败时抛出
   */
  @Override
  public Integer getTimeout() throws SQLException {
    return null;
  }

}
