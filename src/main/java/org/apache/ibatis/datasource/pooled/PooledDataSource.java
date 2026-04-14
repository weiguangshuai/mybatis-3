/*
 *    Copyright 2009-2026 the original author or authors.
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
package org.apache.ibatis.datasource.pooled;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * This is a simple, synchronous, thread-safe database connection pool.
 *
 * @author Clinton Begin
 */
public class PooledDataSource implements DataSource {

  /**
   * 日志记录器
   */
  private static final Log log = LogFactory.getLog(PooledDataSource.class);

  /**
   * 连接池状态，包含活跃连接和空闲连接列表
   */
  private final PoolState state = new PoolState(this);

  /**
   * 底层非池化数据源，用于创建实际数据库连接
   */
  private final UnpooledDataSource dataSource;

  // OPTIONAL CONFIGURATION FIELDS

  /**
   * 池中最大活跃连接数
   */
  protected int poolMaximumActiveConnections = 10;

  /**
   * 池中最大空闲连接数
   */
  protected int poolMaximumIdleConnections = 5;

  /**
   * 连接最大检出时间（毫秒），超过此时间可被强制回收
   */
  protected int poolMaximumCheckoutTime = 20000;

  /**
   * 获取连接等待超时时间（毫秒）
   */
  protected int poolTimeToWait = 20000;

  /**
   * 单线程允许的最大坏连接容忍次数
   */
  protected int poolMaximumLocalBadConnectionTolerance = 3;

  /**
   * 连接检测查询语句
   */
  protected String poolPingQuery = "NO PING QUERY SET";

  /**
   * 是否启用连接检测
   */
  protected boolean poolPingEnabled;

  /**
   * 连接未使用超过此时间（毫秒）则执行检测
   */
  protected int poolPingConnectionsNotUsedFor;

  /**
   * 期望的连接类型代码，用于验证连接有效性
   */
  private int expectedConnectionTypeCode;

  /**
   * 线程锁，用于同步连接池操作
   */
  private final Lock lock = new ReentrantLock();

  /**
   * 条件变量，用于等待可用连接
   */
  private final Condition condition = lock.newCondition();

  /**
   * 默认构造函数，使用默认的UnpooledDataSource
   */
  public PooledDataSource() {
    dataSource = new UnpooledDataSource();
  }

  /**
   * 使用指定的UnpooledDataSource初始化连接池
   */
  public PooledDataSource(UnpooledDataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * 使用JDBC驱动参数创建连接池
   *
   * @param driver   JDBC驱动类名
   * @param url      数据库连接URL
   * @param username 用户名
   * @param password 密码
   */
  public PooledDataSource(String driver, String url, String username, String password) {
    dataSource = new UnpooledDataSource(driver, url, username, password);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  /**
   * 使用JDBC驱动参数和属性创建连接池
   *
   * @param driver           JDBC驱动类名
   * @param url              数据库连接URL
   * @param driverProperties 驱动属性
   */
  public PooledDataSource(String driver, String url, Properties driverProperties) {
    dataSource = new UnpooledDataSource(driver, url, driverProperties);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  /**
   * 使用自定义类加载器创建连接池
   *
   * @param driverClassLoader 驱动类加载器
   * @param driver            JDBC驱动类名
   * @param url               数据库连接URL
   * @param username          用户名
   * @param password          密码
   */
  public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
    dataSource = new UnpooledDataSource(driverClassLoader, driver, url, username, password);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  /**
   * 使用自定义类加载器和属性创建连接池
   *
   * @param driverClassLoader 驱动类加载器
   * @param driver            JDBC驱动类名
   * @param url               数据库连接URL
   * @param driverProperties  驱动属性
   */
  public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
    dataSource = new UnpooledDataSource(driverClassLoader, driver, url, driverProperties);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  /**
   * 从连接池获取一个数据库连接
   *
   * @return 包装后的数据库连接代理对象
   * @throws SQLException 获取连接失败时抛出
   */
  @Override
  public Connection getConnection() throws SQLException {
    return popConnection(dataSource.getUsername(), dataSource.getPassword()).getProxyConnection();
  }

  /**
   * 使用指定凭证从连接池获取一个数据库连接
   *
   * @param username 用户名
   * @param password 密码
   * @return 包装后的数据库连接代理对象
   * @throws SQLException 获取连接失败时抛出
   */
  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return popConnection(username, password).getProxyConnection();
  }

  /**
   * 设置登录超时时间
   *
   * @param loginTimeout 登录超时时间（秒）
   */
  @Override
  public void setLoginTimeout(int loginTimeout) {
    DriverManager.setLoginTimeout(loginTimeout);
  }

  /**
   * 获取登录超时时间
   *
   * @return 登录超时时间（秒）
   */
  @Override
  public int getLoginTimeout() {
    return DriverManager.getLoginTimeout();
  }

  /**
   * 设置日志输出Writer
   *
   * @param logWriter 日志输出Writer
   */
  @Override
  public void setLogWriter(PrintWriter logWriter) {
    DriverManager.setLogWriter(logWriter);
  }

  /**
   * 获取日志输出Writer
   *
   * @return 日志输出Writer
   */
  @Override
  public PrintWriter getLogWriter() {
    return DriverManager.getLogWriter();
  }

  /**
   * 设置JDBC驱动类名
   *
   * @param driver JDBC驱动类名
   */
  public void setDriver(String driver) {
    dataSource.setDriver(driver);
    forceCloseAll();
  }

  /**
   * 设置数据库连接URL
   *
   * @param url 数据库连接URL
   */
  public void setUrl(String url) {
    dataSource.setUrl(url);
    forceCloseAll();
  }

  /**
   * 设置数据库用户名
   *
   * @param username 用户名
   */
  public void setUsername(String username) {
    dataSource.setUsername(username);
    forceCloseAll();
  }

  /**
   * 设置数据库密码
   *
   * @param password 密码
   */
  public void setPassword(String password) {
    dataSource.setPassword(password);
    forceCloseAll();
  }

  /**
   * 设置默认自动提交模式
   *
   * @param defaultAutoCommit 默认自动提交模式
   */
  public void setDefaultAutoCommit(boolean defaultAutoCommit) {
    dataSource.setAutoCommit(defaultAutoCommit);
    forceCloseAll();
  }

  /**
   * 设置默认事务隔离级别
   *
   * @param defaultTransactionIsolationLevel 事务隔离级别
   */
  public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
    dataSource.setDefaultTransactionIsolationLevel(defaultTransactionIsolationLevel);
    forceCloseAll();
  }

  /**
   * 设置驱动属性
   *
   * @param driverProps 驱动属性
   */
  public void setDriverProperties(Properties driverProps) {
    dataSource.setDriverProperties(driverProps);
    forceCloseAll();
  }

  /**
   * Sets the default network timeout value to wait for the database operation to complete. See {@link Connection#setNetworkTimeout(java.util.concurrent.Executor, int)}
   *
   * @param milliseconds The time in milliseconds to wait for the database operation to complete.
   * @since 3.5.2
   */
  public void setDefaultNetworkTimeout(Integer milliseconds) {
    dataSource.setDefaultNetworkTimeout(milliseconds);
    forceCloseAll();
  }

  /**
   * The maximum number of active connections.
   *
   * @param poolMaximumActiveConnections The maximum number of active connections
   */
  public void setPoolMaximumActiveConnections(int poolMaximumActiveConnections) {
    this.poolMaximumActiveConnections = poolMaximumActiveConnections;
    forceCloseAll();
  }

  /**
   * The maximum number of idle connections.
   *
   * @param poolMaximumIdleConnections The maximum number of idle connections
   */
  public void setPoolMaximumIdleConnections(int poolMaximumIdleConnections) {
    this.poolMaximumIdleConnections = poolMaximumIdleConnections;
    forceCloseAll();
  }

  /**
   * The maximum number of tolerance for bad connection happens in one thread
   * which are applying for new {@link PooledConnection}.
   *
   * @param poolMaximumLocalBadConnectionTolerance max tolerance for bad connection happens in one thread
   * @since 3.4.5
   */
  public void setPoolMaximumLocalBadConnectionTolerance(
    int poolMaximumLocalBadConnectionTolerance) {
    this.poolMaximumLocalBadConnectionTolerance = poolMaximumLocalBadConnectionTolerance;
  }

  /**
   * The maximum time a connection can be used before it *may* be
   * given away again.
   *
   * @param poolMaximumCheckoutTime The maximum time
   */
  public void setPoolMaximumCheckoutTime(int poolMaximumCheckoutTime) {
    this.poolMaximumCheckoutTime = poolMaximumCheckoutTime;
    forceCloseAll();
  }

  /**
   * The time to wait before retrying to get a connection.
   *
   * @param poolTimeToWait The time to wait
   */
  public void setPoolTimeToWait(int poolTimeToWait) {
    this.poolTimeToWait = poolTimeToWait;
    forceCloseAll();
  }

  /**
   * The query to be used to check a connection.
   *
   * @param poolPingQuery The query
   */
  public void setPoolPingQuery(String poolPingQuery) {
    this.poolPingQuery = poolPingQuery;
    forceCloseAll();
  }

  /**
   * Determines if the ping query should be used.
   *
   * @param poolPingEnabled True if we need to check a connection before using it
   */
  public void setPoolPingEnabled(boolean poolPingEnabled) {
    this.poolPingEnabled = poolPingEnabled;
    forceCloseAll();
  }

  /**
   * If a connection has not been used in this many milliseconds, ping the
   * database to make sure the connection is still good.
   *
   * @param milliseconds the number of milliseconds of inactivity that will trigger a ping
   */
  public void setPoolPingConnectionsNotUsedFor(int milliseconds) {
    this.poolPingConnectionsNotUsedFor = milliseconds;
    forceCloseAll();
  }

  /**
   * 获取JDBC驱动类名
   *
   * @return JDBC驱动类名
   */
  public String getDriver() {
    return dataSource.getDriver();
  }

  /**
   * 获取数据库连接URL
   *
   * @return 数据库连接URL
   */
  public String getUrl() {
    return dataSource.getUrl();
  }

  /**
   * 获取数据库用户名
   *
   * @return 用户名
   */
  public String getUsername() {
    return dataSource.getUsername();
  }

  /**
   * 获取数据库密码
   *
   * @return 密码
   */
  public String getPassword() {
    return dataSource.getPassword();
  }

  /**
   * 获取默认自动提交模式
   *
   * @return 默认自动提交模式
   */
  public boolean isAutoCommit() {
    return dataSource.isAutoCommit();
  }

  /**
   * 获取默认事务隔离级别
   *
   * @return 事务隔离级别
   */
  public Integer getDefaultTransactionIsolationLevel() {
    return dataSource.getDefaultTransactionIsolationLevel();
  }

  /**
   * 获取驱动属性
   *
   * @return 驱动属性
   */
  public Properties getDriverProperties() {
    return dataSource.getDriverProperties();
  }

  /**
   * Gets the default network timeout.
   *
   * @return the default network timeout
   * @since 3.5.2
   */
  public Integer getDefaultNetworkTimeout() {
    return dataSource.getDefaultNetworkTimeout();
  }

  /**
   * 获取池中最大活跃连接数
   *
   * @return 最大活跃连接数
   */
  public int getPoolMaximumActiveConnections() {
    return poolMaximumActiveConnections;
  }

  /**
   * 获取池中最大空闲连接数
   *
   * @return 最大空闲连接数
   */
  public int getPoolMaximumIdleConnections() {
    return poolMaximumIdleConnections;
  }

  /**
   * 获取单线程允许的最大坏连接容忍次数
   *
   * @return 最大坏连接容忍次数
   */
  public int getPoolMaximumLocalBadConnectionTolerance() {
    return poolMaximumLocalBadConnectionTolerance;
  }

  /**
   * 获取连接最大检出时间
   *
   * @return 最大检出时间（毫秒）
   */
  public int getPoolMaximumCheckoutTime() {
    return poolMaximumCheckoutTime;
  }

  /**
   * 获取获取连接等待超时时间
   *
   * @return 等待超时时间（毫秒）
   */
  public int getPoolTimeToWait() {
    return poolTimeToWait;
  }

  /**
   * 获取连接检测查询语句
   *
   * @return 检测查询语句
   */
  public String getPoolPingQuery() {
    return poolPingQuery;
  }

  /**
   * 获取是否启用连接检测
   *
   * @return 是否启用
   */
  public boolean isPoolPingEnabled() {
    return poolPingEnabled;
  }

  /**
   * 获取连接未使用超时时间
   *
   * @return 未使用超时时间（毫秒）
   */
  public int getPoolPingConnectionsNotUsedFor() {
    return poolPingConnectionsNotUsedFor;
  }

  /**
   * 强制关闭连接池中所有活跃和空闲连接
   * 通常在配置变更时调用
   */
  public void forceCloseAll() {
    lock.lock();
    try {
      // 重新计算连接类型代码
      expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
      // 关闭所有活跃连接
      for (int i = state.activeConnections.size(); i > 0; i--) {
        try {
          PooledConnection conn = state.activeConnections.remove(i - 1);
          conn.invalidate();

          Connection realConn = conn.getRealConnection();
          // 回滚未提交的事务
          if (!realConn.getAutoCommit()) {
            realConn.rollback();
          }
          realConn.close();
        } catch (Exception e) {
          // 忽略关闭异常
        }
      }
      // 关闭所有空闲连接
      for (int i = state.idleConnections.size(); i > 0; i--) {
        try {
          PooledConnection conn = state.idleConnections.remove(i - 1);
          conn.invalidate();

          Connection realConn = conn.getRealConnection();
          // 回滚未提交的事务
          if (!realConn.getAutoCommit()) {
            realConn.rollback();
          }
          realConn.close();
        } catch (Exception e) {
          // 忽略关闭异常
        }
      }
    } finally {
      lock.unlock();
    }
    if (log.isDebugEnabled()) {
      log.debug("PooledDataSource forcefully closed/removed all connections.");
    }
  }

  /**
   * 获取连接池状态对象
   *
   * @return 连接池状态
   */
  public PoolState getPoolState() {
    return state;
  }

  /**
   * 根据URL、用户名、密码生成连接类型代码
   *
   * @param url      数据库连接URL
   * @param username 用户名
   * @param password 密码
   * @return 连接类型代码
   */
  private int assembleConnectionTypeCode(String url, String username, String password) {
    return ("" + url + username + password).hashCode();
  }

  /**
   * 将连接归还到连接池
   *
   * @param conn 要归还的池化连接
   * @throws SQLException 处理连接时发生错误
   */
  protected void pushConnection(PooledConnection conn) throws SQLException {

    lock.lock();
    try {
      // 从活跃连接列表中移除
      state.activeConnections.remove(conn);
      if (conn.isValid()) {
        // 检查是否可以将连接放入空闲队列
        if (state.idleConnections.size() < poolMaximumIdleConnections && conn.getConnectionTypeCode() == expectedConnectionTypeCode) {
          // 累加检出时间
          state.accumulatedCheckoutTime += conn.getCheckoutTime();
          // 回滚未提交的事务
          if (!conn.getRealConnection().getAutoCommit()) {
            conn.getRealConnection().rollback();
          }
          // 重新包装连接并放入空闲队列
          PooledConnection newConn = new PooledConnection(conn.getRealConnection(), this);
          state.idleConnections.add(newConn);
          // 保留原有的创建时间和最后使用时间
          newConn.setCreatedTimestamp(conn.getCreatedTimestamp());
          newConn.setLastUsedTimestamp(conn.getLastUsedTimestamp());
          conn.invalidate();
          if (log.isDebugEnabled()) {
            log.debug("Returned connection " + newConn.getRealHashCode() + " to pool.");
          }
          // 唤醒等待线程
          condition.signal();
        } else {
          // 无法放入空闲队列，关闭实际连接
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
        // 连接无效，记录坏连接计数
        if (log.isDebugEnabled()) {
          log.debug("A bad connection (" + conn.getRealHashCode() + ") attempted to return to the pool, discarding connection.");
        }
        state.badConnectionCount++;
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * 从连接池获取一个连接
   *
   * @param username 用户名
   * @param password 密码
   * @return 池化连接对象
   * @throws SQLException 获取连接失败时抛出
   */
  private PooledConnection popConnection(String username, String password) throws SQLException {
    boolean countedWait = false;
    PooledConnection conn = null;
    long t = System.currentTimeMillis();
    int localBadConnectionCount = 0;

    while (conn == null) {
      lock.lock();
      try {
        // 1. 首先尝试从空闲队列获取连接
        if (!state.idleConnections.isEmpty()) {
          // 池中有可用连接
          conn = state.idleConnections.remove(0);
          if (log.isDebugEnabled()) {
            log.debug("Checked out connection " + conn.getRealHashCode() + " from pool.");
          }
        } else {
          // 2. 池中没有可用连接
          if (state.activeConnections.size() < poolMaximumActiveConnections) {
            // 可以创建新连接
            conn = new PooledConnection(dataSource.getConnection(), this);
            if (log.isDebugEnabled()) {
              log.debug("Created connection " + conn.getRealHashCode() + ".");
            }
          } else {
            // 3. 已达到最大连接数，无法创建新连接
            // 检查是否有超期连接可以回收
            PooledConnection oldestActiveConnection = state.activeConnections.get(0);
            long longestCheckoutTime = oldestActiveConnection.getCheckoutTime();
            if (longestCheckoutTime > poolMaximumCheckoutTime) {
              // 可以回收超期连接
              state.claimedOverdueConnectionCount++;
              state.accumulatedCheckoutTimeOfOverdueConnections += longestCheckoutTime;
              state.accumulatedCheckoutTime += longestCheckoutTime;
              state.activeConnections.remove(oldestActiveConnection);
              // 回滚未提交的事务
              if (!oldestActiveConnection.getRealConnection().getAutoCommit()) {
                try {
                  oldestActiveConnection.getRealConnection().rollback();
                } catch (SQLException e) {
                  // 仅记录日志，继续执行
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
              // 4. 必须等待其他线程释放连接
              try {
                if (!countedWait) {
                  state.hadToWaitCount++;
                  countedWait = true;
                }
                if (log.isDebugEnabled()) {
                  log.debug("Waiting as long as " + poolTimeToWait + " milliseconds for connection.");
                }
                long wt = System.currentTimeMillis();
                condition.await(poolTimeToWait, TimeUnit.MILLISECONDS);
                state.accumulatedWaitTime += System.currentTimeMillis() - wt;
              } catch (InterruptedException e) {
                // 设置中断标志并退出
                Thread.currentThread().interrupt();
                break;
              }
            }
          }
        }
        // 5. 验证连接有效性
        if (conn != null) {
          // ping检测连接是否有效
          if (conn.isValid()) {
            // 回滚未提交的事务
            if (!conn.getRealConnection().getAutoCommit()) {
              conn.getRealConnection().rollback();
            }
            // 设置连接类型代码
            conn.setConnectionTypeCode(assembleConnectionTypeCode(dataSource.getUrl(), username, password));
            conn.setCheckoutTimestamp(System.currentTimeMillis());
            conn.setLastUsedTimestamp(System.currentTimeMillis());
            // 加入活跃队列
            state.activeConnections.add(conn);
            state.requestCount++;
            state.accumulatedRequestTime += System.currentTimeMillis() - t;
          } else {
            // 连接无效，记录并重试
            if (log.isDebugEnabled()) {
              log.debug("A bad connection (" + conn.getRealHashCode() + ") was returned from the pool, getting another connection.");
            }
            state.badConnectionCount++;
            localBadConnectionCount++;
            conn = null;
            // 超过容忍阈值则抛出异常
            if (localBadConnectionCount > (poolMaximumIdleConnections + poolMaximumLocalBadConnectionTolerance)) {
              if (log.isDebugEnabled()) {
                log.debug("PooledDataSource: Could not get a good connection to the database.");
              }
              throw new SQLException("PooledDataSource: Could not get a good connection to the database.");
            }
          }
        }
      } finally {
        lock.unlock();
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
   * Method to check to see if a connection is still usable
   *
   * @param conn - the connection to check
   * @return True if the connection is still usable
   */
  protected boolean pingConnection(PooledConnection conn) {
    boolean result = true;

    try {
      // 首先检查底层连接是否已关闭
      result = !conn.getRealConnection().isClosed();
    } catch (SQLException e) {
      if (log.isDebugEnabled()) {
        log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
      }
      result = false;
    }

    // 如果连接有效且启用了ping检测
    if (result && poolPingEnabled && poolPingConnectionsNotUsedFor >= 0
      && conn.getTimeElapsedSinceLastUse() > poolPingConnectionsNotUsedFor) {
      try {
        if (log.isDebugEnabled()) {
          log.debug("Testing connection " + conn.getRealHashCode() + " ...");
        }
        Connection realConn = conn.getRealConnection();
        // 执行ping查询验证连接
        try (Statement statement = realConn.createStatement()) {
          statement.executeQuery(poolPingQuery).close();
        }
        // 回滚可能产生的副作用
        if (!realConn.getAutoCommit()) {
          realConn.rollback();
        }
        result = true;
        if (log.isDebugEnabled()) {
          log.debug("Connection " + conn.getRealHashCode() + " is GOOD!");
        }
      } catch (Exception e) {
        // ping失败，关闭连接并标记为无效
        log.warn("Execution of ping query '" + poolPingQuery + "' failed: " + e.getMessage());
        try {
          conn.getRealConnection().close();
        } catch (Exception e2) {
          // 忽略关闭异常
        }
        result = false;
        if (log.isDebugEnabled()) {
          log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
        }
      }
    }
    return result;
  }

  /**
   * 解包池化连接，获取底层真实连接
   *
   * @param conn 池化连接或普通连接
   * @return 底层真实数据库连接
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
   * 对象销毁前确保关闭所有连接
   */
  @Override
  protected void finalize() throws Throwable {
    forceCloseAll();
    super.finalize();
  }

  /**
   * 不支持此方法
   *
   * @param iface 接口类型
   * @return 总是抛出SQLException
   * @throws SQLException 总是抛出
   */
  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException(getClass().getName() + " is not a wrapper.");
  }

  /**
   * 不支持此方法
   *
   * @param iface 接口类型
   * @return 总是返回false
   */
  @Override
  public boolean isWrapperFor(Class<?> iface) {
    return false;
  }

  /**
   * 获取父日志记录器
   *
   * @return 全局日志记录器
   */
  @Override
  public Logger getParentLogger() {
    return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  }

}
