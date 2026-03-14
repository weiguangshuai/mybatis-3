/**
 *    Copyright 2009-2026 the original author or authors.
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
package org.apache.ibatis.datasource.pooled;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 基于同步锁实现的线程安全数据库连接池。
 * <p>
 * 该实现通过维护「空闲连接」与「活跃连接」两个集合来复用 JDBC 连接，
 * 在连接归还时做必要清理（如回滚未提交事务），并可按需执行 ping SQL 校验连接可用性。
 *
 * @author Clinton Begin
 */
public class PooledDataSource implements DataSource {

  private static final Log log = LogFactory.getLog(PooledDataSource.class);

  /**
   * 连接池运行时状态，包含空闲/活跃连接列表及统计指标。
   * <p>
   * 对该对象加锁即可串行化连接池核心操作，保证状态一致性。
   */
  private final PoolState state = new PoolState(this);

  /**
   * 底层非池化数据源，负责真实 JDBC 连接创建。
   */
  private final UnpooledDataSource dataSource;

  /**
   * 池中允许同时借出的最大连接数。
   */
  protected int poolMaximumActiveConnections = 10;
  /**
   * 池中允许缓存的最大空闲连接数。
   */
  protected int poolMaximumIdleConnections = 5;
  /**
   * 单个连接允许被连续借出的最长时长（毫秒），超时后可被强制回收。
   */
  protected int poolMaximumCheckoutTime = 20000;
  /**
   * 无可用连接时，线程每次等待可用连接的最长时长（毫秒）。
   */
  protected int poolTimeToWait = 20000;
  /**
   * 单线程在一次获取连接流程中可容忍的坏连接额外重试次数。
   */
  protected int poolMaximumLocalBadConnectionTolerance = 3;
  /**
   * 连接健康检查使用的 SQL，默认值仅作占位。
   */
  protected String poolPingQuery = "NO PING QUERY SET";
  /**
   * 是否启用连接借出前的健康检查。
   */
  protected boolean poolPingEnabled;
  /**
   * 连接空闲超过该时长（毫秒）后才触发 ping 检查。
   */
  protected int poolPingConnectionsNotUsedFor;

  /**
   * 当前数据源配置（url/username/password）的哈希标识。
   * <p>
   * 连接归还时会用该标识判定其是否仍属于当前池配置。
   */
  private int expectedConnectionTypeCode;

  /**
   * 使用默认配置创建连接池。
   */
  public PooledDataSource() {
    dataSource = new UnpooledDataSource();
  }

  /**
   * 基于已构造的非池化数据源创建连接池。
   *
   * @param dataSource 底层非池化数据源
   */
  public PooledDataSource(UnpooledDataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * 使用驱动、URL 与账号密码创建连接池。
   *
   * @param driver 数据库驱动类名
   * @param url JDBC 连接串
   * @param username 用户名
   * @param password 密码
   */
  public PooledDataSource(String driver, String url, String username, String password) {
    dataSource = new UnpooledDataSource(driver, url, username, password);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  /**
   * 使用驱动、URL 与驱动属性创建连接池。
   *
   * @param driver 数据库驱动类名
   * @param url JDBC 连接串
   * @param driverProperties 驱动属性
   */
  public PooledDataSource(String driver, String url, Properties driverProperties) {
    dataSource = new UnpooledDataSource(driver, url, driverProperties);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  /**
   * 指定驱动类加载器并使用账号密码创建连接池。
   *
   * @param driverClassLoader 驱动类加载器
   * @param driver 数据库驱动类名
   * @param url JDBC 连接串
   * @param username 用户名
   * @param password 密码
   */
  public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
    dataSource = new UnpooledDataSource(driverClassLoader, driver, url, username, password);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  /**
   * 指定驱动类加载器并使用驱动属性创建连接池。
   *
   * @param driverClassLoader 驱动类加载器
   * @param driver 数据库驱动类名
   * @param url JDBC 连接串
   * @param driverProperties 驱动属性
   */
  public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
    dataSource = new UnpooledDataSource(driverClassLoader, driver, url, driverProperties);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  /**
   * 使用数据源默认账号密码获取连接。
   *
   * @return JDBC 代理连接
   * @throws SQLException 获取连接失败
   */
  @Override
  public Connection getConnection() throws SQLException {
    return popConnection(dataSource.getUsername(), dataSource.getPassword()).getProxyConnection();
  }

  /**
   * 使用指定账号密码获取连接。
   *
   * @param username 用户名
   * @param password 密码
   * @return JDBC 代理连接
   * @throws SQLException 获取连接失败
   */
  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return popConnection(username, password).getProxyConnection();
  }

  @Override
  public void setLoginTimeout(int loginTimeout) throws SQLException {
    DriverManager.setLoginTimeout(loginTimeout);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return DriverManager.getLoginTimeout();
  }

  @Override
  public void setLogWriter(PrintWriter logWriter) throws SQLException {
    DriverManager.setLogWriter(logWriter);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return DriverManager.getLogWriter();
  }

  /**
   * 设置 JDBC 驱动类名并重置连接池。
   *
   * @param driver 驱动类名
   */
  public void setDriver(String driver) {
    dataSource.setDriver(driver);
    forceCloseAll();
  }

  /**
   * 设置 JDBC URL 并重置连接池。
   *
   * @param url JDBC 连接串
   */
  public void setUrl(String url) {
    dataSource.setUrl(url);
    forceCloseAll();
  }

  /**
   * 设置用户名并重置连接池。
   *
   * @param username 用户名
   */
  public void setUsername(String username) {
    dataSource.setUsername(username);
    forceCloseAll();
  }

  /**
   * 设置密码并重置连接池。
   *
   * @param password 密码
   */
  public void setPassword(String password) {
    dataSource.setPassword(password);
    forceCloseAll();
  }

  /**
   * 设置默认自动提交行为并重置连接池。
   *
   * @param defaultAutoCommit 默认自动提交开关
   */
  public void setDefaultAutoCommit(boolean defaultAutoCommit) {
    dataSource.setAutoCommit(defaultAutoCommit);
    forceCloseAll();
  }

  /**
   * 设置默认事务隔离级别并重置连接池。
   *
   * @param defaultTransactionIsolationLevel 默认事务隔离级别
   */
  public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
    dataSource.setDefaultTransactionIsolationLevel(defaultTransactionIsolationLevel);
    forceCloseAll();
  }

  /**
   * 设置驱动属性并重置连接池。
   *
   * @param driverProps 驱动参数
   */
  public void setDriverProperties(Properties driverProps) {
    dataSource.setDriverProperties(driverProps);
    forceCloseAll();
  }

  /**
   * 设置最大活跃连接数。
   *
   * @param poolMaximumActiveConnections 最大可并发借出的连接数
   */
  public void setPoolMaximumActiveConnections(int poolMaximumActiveConnections) {
    this.poolMaximumActiveConnections = poolMaximumActiveConnections;
    forceCloseAll();
  }

  /**
   * 设置最大空闲连接数。
   *
   * @param poolMaximumIdleConnections 空闲连接缓存上限
   */
  public void setPoolMaximumIdleConnections(int poolMaximumIdleConnections) {
    this.poolMaximumIdleConnections = poolMaximumIdleConnections;
    forceCloseAll();
  }

  /**
   * 设置单线程获取连接时可容忍的坏连接次数。
   *
   * @param poolMaximumLocalBadConnectionTolerance
   *        单线程在一次获取流程中的坏连接容忍上限
   *
   * @since 3.4.5
   */
  public void setPoolMaximumLocalBadConnectionTolerance(
      int poolMaximumLocalBadConnectionTolerance) {
    this.poolMaximumLocalBadConnectionTolerance = poolMaximumLocalBadConnectionTolerance;
  }

  /**
   * 设置连接最长借出时间。
   *
   * @param poolMaximumCheckoutTime 连接可连续占用的最长时间（毫秒）
   */
  public void setPoolMaximumCheckoutTime(int poolMaximumCheckoutTime) {
    this.poolMaximumCheckoutTime = poolMaximumCheckoutTime;
    forceCloseAll();
  }

  /**
   * 设置获取连接失败后的等待时长。
   *
   * @param poolTimeToWait 每次等待重试的时间（毫秒）
   */
  public void setPoolTimeToWait(int poolTimeToWait) {
    this.poolTimeToWait = poolTimeToWait;
    forceCloseAll();
  }

  /**
   * 设置连接健康检查 SQL。
   *
   * @param poolPingQuery 用于验证连接可用性的 SQL
   */
  public void setPoolPingQuery(String poolPingQuery) {
    this.poolPingQuery = poolPingQuery;
    forceCloseAll();
  }

  /**
   * 设置是否启用 ping 检查。
   *
   * @param poolPingEnabled 为 true 时，在满足空闲阈值后会执行 ping SQL
   */
  public void setPoolPingEnabled(boolean poolPingEnabled) {
    this.poolPingEnabled = poolPingEnabled;
    forceCloseAll();
  }

  /**
   * 设置触发 ping 检查的最小空闲时长。
   *
   * @param milliseconds 连接空闲达到该毫秒数后触发 ping
   */
  public void setPoolPingConnectionsNotUsedFor(int milliseconds) {
    this.poolPingConnectionsNotUsedFor = milliseconds;
    forceCloseAll();
  }

  public String getDriver() {
    return dataSource.getDriver();
  }

  public String getUrl() {
    return dataSource.getUrl();
  }

  public String getUsername() {
    return dataSource.getUsername();
  }

  public String getPassword() {
    return dataSource.getPassword();
  }

  public boolean isAutoCommit() {
    return dataSource.isAutoCommit();
  }

  public Integer getDefaultTransactionIsolationLevel() {
    return dataSource.getDefaultTransactionIsolationLevel();
  }

  public Properties getDriverProperties() {
    return dataSource.getDriverProperties();
  }

  public int getPoolMaximumActiveConnections() {
    return poolMaximumActiveConnections;
  }

  public int getPoolMaximumIdleConnections() {
    return poolMaximumIdleConnections;
  }

  public int getPoolMaximumLocalBadConnectionTolerance() {
    return poolMaximumLocalBadConnectionTolerance;
  }

  public int getPoolMaximumCheckoutTime() {
    return poolMaximumCheckoutTime;
  }

  public int getPoolTimeToWait() {
    return poolTimeToWait;
  }

  public String getPoolPingQuery() {
    return poolPingQuery;
  }

  public boolean isPoolPingEnabled() {
    return poolPingEnabled;
  }

  public int getPoolPingConnectionsNotUsedFor() {
    return poolPingConnectionsNotUsedFor;
  }

  /**
   * 强制关闭连接池中的所有连接（包括活跃与空闲连接）。
   * <p>
   * 常用于数据源关键配置变更后，避免旧配置连接继续被复用。
   */
  public void forceCloseAll() {
    synchronized (state) {
      expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
      for (int i = state.activeConnections.size(); i > 0; i--) {
        try {
          PooledConnection conn = state.activeConnections.remove(i - 1);
          conn.invalidate();

          Connection realConn = conn.getRealConnection();
          if (!realConn.getAutoCommit()) {
            realConn.rollback();
          }
          realConn.close();
        } catch (Exception e) {
          // 关闭过程中的异常仅用于兜底清理，不影响后续连接继续回收。
        }
      }
      for (int i = state.idleConnections.size(); i > 0; i--) {
        try {
          PooledConnection conn = state.idleConnections.remove(i - 1);
          conn.invalidate();

          Connection realConn = conn.getRealConnection();
          if (!realConn.getAutoCommit()) {
            realConn.rollback();
          }
          realConn.close();
        } catch (Exception e) {
          // 关闭过程中的异常仅用于兜底清理，不影响后续连接继续回收。
        }
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("PooledDataSource forcefully closed/removed all connections.");
    }
  }

  /**
   * 返回连接池当前状态对象（主要用于监控与调试）。
   *
   * @return 连接池状态
   */
  public PoolState getPoolState() {
    return state;
  }

  /**
   * 根据 URL、用户名与密码计算连接类型标识。
   *
   * @param url JDBC 连接串
   * @param username 用户名
   * @param password 密码
   * @return 连接配置哈希值
   */
  private int assembleConnectionTypeCode(String url, String username, String password) {
    return ("" + url + username + password).hashCode();
  }

  /**
   * 归还连接到连接池。
   * <p>
   * 若连接有效且池中空闲位未满，则回收到空闲队列；否则直接关闭物理连接。
   *
   * @param conn 待归还的池化连接
   * @throws SQLException 归还过程中的数据库异常
   */
  protected void pushConnection(PooledConnection conn) throws SQLException {

    synchronized (state) {
      state.activeConnections.remove(conn);
      if (conn.isValid()) {
        if (state.idleConnections.size() < poolMaximumIdleConnections && conn.getConnectionTypeCode() == expectedConnectionTypeCode) {
          // 连接可复用：回滚未提交事务后放回空闲队列，并复制时间戳统计信息。
          state.accumulatedCheckoutTime += conn.getCheckoutTime();
          if (!conn.getRealConnection().getAutoCommit()) {
            conn.getRealConnection().rollback();
          }
          PooledConnection newConn = new PooledConnection(conn.getRealConnection(), this);
          state.idleConnections.add(newConn);
          newConn.setCreatedTimestamp(conn.getCreatedTimestamp());
          newConn.setLastUsedTimestamp(conn.getLastUsedTimestamp());
          conn.invalidate();
          if (log.isDebugEnabled()) {
            log.debug("Returned connection " + newConn.getRealHashCode() + " to pool.");
          }
          state.notifyAll();
        } else {
          // 连接不可复用（池已满或配置不匹配），直接关闭物理连接避免污染连接池。
          state.accumulatedCheckoutTime += conn.getCheckoutTime();
          if (!conn.getRealConnection().getAutoCommit()) {
            conn.getRealConnection().rollback();
          }
          conn.getRealConnection().close();
          if (log.isDebugEnabled()) {
            log.debug("Closed connection " + conn.getRealHashCode() + ".");
          }
          conn.invalidate();
        }
      } else {
        // 无效连接直接丢弃，仅记录坏连接计数。
        if (log.isDebugEnabled()) {
          log.debug("A bad connection (" + conn.getRealHashCode() + ") attempted to return to the pool, discarding connection.");
        }
        state.badConnectionCount++;
      }
    }
  }

  /**
   * 从连接池获取可用连接。
   * <p>
   * 获取顺序为：优先取空闲连接，其次新建连接，最后在超时条件下抢占最老活跃连接。
   *
   * @param username 用户名
   * @param password 密码
   * @return 可用的池化连接
   * @throws SQLException 无法获取可用连接时抛出
   */
  private PooledConnection popConnection(String username, String password) throws SQLException {
    boolean countedWait = false;
    PooledConnection conn = null;
    long t = System.currentTimeMillis();
    int localBadConnectionCount = 0;

    while (conn == null) {
      synchronized (state) {
        if (!state.idleConnections.isEmpty()) {
          // 连接池存在空闲连接，直接取出复用。
          conn = state.idleConnections.remove(0);
          if (log.isDebugEnabled()) {
            log.debug("Checked out connection " + conn.getRealHashCode() + " from pool.");
          }
        } else {
          // 没有空闲连接时，优先尝试新建连接。
          if (state.activeConnections.size() < poolMaximumActiveConnections) {
            // 活跃连接数未达上限，允许创建新连接。
            conn = new PooledConnection(dataSource.getConnection(), this);
            if (log.isDebugEnabled()) {
              log.debug("Created connection " + conn.getRealHashCode() + ".");
            }
          } else {
            // 活跃连接已达上限，只能尝试抢占超时连接或等待。
            PooledConnection oldestActiveConnection = state.activeConnections.get(0);
            long longestCheckoutTime = oldestActiveConnection.getCheckoutTime();
            if (longestCheckoutTime > poolMaximumCheckoutTime) {
              // 最老活跃连接已超时，回收其物理连接并转交给当前请求。
              state.claimedOverdueConnectionCount++;
              state.accumulatedCheckoutTimeOfOverdueConnections += longestCheckoutTime;
              state.accumulatedCheckoutTime += longestCheckoutTime;
              state.activeConnections.remove(oldestActiveConnection);
              if (!oldestActiveConnection.getRealConnection().getAutoCommit()) {
                try {
                  oldestActiveConnection.getRealConnection().rollback();
                } catch (SQLException e) {
                  // 回滚失败通常意味着该连接已损坏：记录日志并继续后续流程，
                  // 让当前线程有机会在本轮或下一轮竞争到可用连接。
                  log.debug("Bad connection. Could not roll back");
                }
              }
              conn = new PooledConnection(oldestActiveConnection.getRealConnection(), this);
              conn.setCreatedTimestamp(oldestActiveConnection.getCreatedTimestamp());
              conn.setLastUsedTimestamp(oldestActiveConnection.getLastUsedTimestamp());
              oldestActiveConnection.invalidate();
              if (log.isDebugEnabled()) {
                log.debug("Claimed overdue connection " + conn.getRealHashCode() + ".");
              }
            } else {
              // 没有可抢占连接，只能等待其他线程归还连接。
              try {
                if (!countedWait) {
                  state.hadToWaitCount++;
                  countedWait = true;
                }
                if (log.isDebugEnabled()) {
                  log.debug("Waiting as long as " + poolTimeToWait + " milliseconds for connection.");
                }
                long wt = System.currentTimeMillis();
                state.wait(poolTimeToWait);
                state.accumulatedWaitTime += System.currentTimeMillis() - wt;
              } catch (InterruptedException e) {
                break;
              }
            }
          }
        }
        if (conn != null) {
          // 借出前进行最终有效性检查（可触发 ping）。
          if (conn.isValid()) {
            if (!conn.getRealConnection().getAutoCommit()) {
              conn.getRealConnection().rollback();
            }
            conn.setConnectionTypeCode(assembleConnectionTypeCode(dataSource.getUrl(), username, password));
            conn.setCheckoutTimestamp(System.currentTimeMillis());
            conn.setLastUsedTimestamp(System.currentTimeMillis());
            state.activeConnections.add(conn);
            state.requestCount++;
            state.accumulatedRequestTime += System.currentTimeMillis() - t;
          } else {
            if (log.isDebugEnabled()) {
              log.debug("A bad connection (" + conn.getRealHashCode() + ") was returned from the pool, getting another connection.");
            }
            state.badConnectionCount++;
            localBadConnectionCount++;
            conn = null;
            // 单线程连续拿到坏连接超过阈值时快速失败，避免无限循环等待。
            if (localBadConnectionCount > (poolMaximumIdleConnections + poolMaximumLocalBadConnectionTolerance)) {
              if (log.isDebugEnabled()) {
                log.debug("PooledDataSource: Could not get a good connection to the database.");
              }
              throw new SQLException("PooledDataSource: Could not get a good connection to the database.");
            }
          }
        }
      }

    }

    if (conn == null) {
      if (log.isDebugEnabled()) {
        log.debug("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
      }
      throw new SQLException("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
    }

    return conn;
  }

  /**
   * 检查连接是否仍可用。
   *
   * @param conn 待检查的池化连接
   * @return true 表示连接可用
   */
  protected boolean pingConnection(PooledConnection conn) {
    boolean result = true;

    try {
      result = !conn.getRealConnection().isClosed();
    } catch (SQLException e) {
      if (log.isDebugEnabled()) {
        log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
      }
      result = false;
    }

    if (result) {
      if (poolPingEnabled) {
        if (poolPingConnectionsNotUsedFor >= 0 && conn.getTimeElapsedSinceLastUse() > poolPingConnectionsNotUsedFor) {
          try {
            if (log.isDebugEnabled()) {
              log.debug("Testing connection " + conn.getRealHashCode() + " ...");
            }
            Connection realConn = conn.getRealConnection();
            Statement statement = realConn.createStatement();
            ResultSet rs = statement.executeQuery(poolPingQuery);
            rs.close();
            statement.close();
            // 与归还连接保持一致：若为手动事务模式，清理潜在未提交状态。
            if (!realConn.getAutoCommit()) {
              realConn.rollback();
            }
            result = true;
            if (log.isDebugEnabled()) {
              log.debug("Connection " + conn.getRealHashCode() + " is GOOD!");
            }
          } catch (Exception e) {
            log.warn("Execution of ping query '" + poolPingQuery + "' failed: " + e.getMessage());
            try {
              conn.getRealConnection().close();
            } catch (Exception e2) {
              // 忽略关闭失败，保持原始异常处理流程。
            }
            result = false;
            if (log.isDebugEnabled()) {
              log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * 从池化代理连接中提取底层真实连接。
   *
   * @param conn 可能是代理的连接对象
   * @return 若为池化代理则返回真实连接，否则返回原对象
   */
  public static Connection unwrapConnection(Connection conn) {
    if (Proxy.isProxyClass(conn.getClass())) {
      InvocationHandler handler = Proxy.getInvocationHandler(conn);
      if (handler instanceof PooledConnection) {
        return ((PooledConnection) handler).getRealConnection();
      }
    }
    return conn;
  }

  /**
   * 终结器兜底关闭连接池，避免资源泄漏。
   *
   * @throws Throwable 由父类终结逻辑抛出的异常
   */
  protected void finalize() throws Throwable {
    forceCloseAll();
    super.finalize();
  }

  /**
   * 本数据源不支持 JDBC unwrap。
   *
   * @param iface 目标接口类型
   * @param <T> 目标类型
   * @return 不会返回
   * @throws SQLException 始终抛出
   */
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException(getClass().getName() + " is not a wrapper.");
  }

  /**
   * 当前实现不作为任何接口的 JDBC Wrapper。
   *
   * @param iface 目标接口类型
   * @return 始终为 false
   * @throws SQLException 保持与接口签名一致
   */
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  /**
   * 返回 JDBC 4.1 要求的父级日志器。
   *
   * @return 全局日志器
   */
  public Logger getParentLogger() {
    return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); // 需要 JDK 1.6+
  }

}
