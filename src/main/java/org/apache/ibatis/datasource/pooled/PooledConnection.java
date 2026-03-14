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
package org.apache.ibatis.datasource.pooled;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * @author Clinton Begin
 */
/**
 * 池化连接的代理实现类。
 * <p>
 * 该类通过动态代理包装真实的数据库连接，当调用 {@code close()} 方法时，
 * 不会真正关闭连接，而是将连接归还到连接池中，以便重复使用。
 * </p>
 */
class PooledConnection implements InvocationHandler {

  private static final String CLOSE = "close";
  /** 代理类需要实现的接口数组 */
  private static final Class<?>[] IFACES = new Class<?>[] { Connection.class };

  /** 真实连接的哈希码，用于 equals/hashCode 比较 */
  private final int hashCode;
  /** 数据源，用于归还连接 */
  private final PooledDataSource dataSource;
  /** 被代理的真实数据库连接 */
  private final Connection realConnection;
  /** 代理后的连接对象，提供给使用者 */
  private final Connection proxyConnection;
  /** 连接被取出的时间戳 */
  private long checkoutTimestamp;
  /** 连接创建的时间戳 */
  private long createdTimestamp;
  /** 连接最后一次被使用的时间戳 */
  private long lastUsedTimestamp;
  /** 连接类型编码（基于 URL+ 用户名 + 密码） */
  private int connectionTypeCode;
  /** 连接是否有效 */
  private boolean valid;

  /*
   * Constructor for SimplePooledConnection that uses the Connection and PooledDataSource passed in
   *
   * @param connection - the connection that is to be presented as a pooled connection
   * @param dataSource - the dataSource that the connection is from
   */
  /**
   * 构造池化连接代理对象。
   * <p>
   * 初始化连接的所有时间戳信息，并创建真实连接的代理对象。
   * </p>
   *
   * @param connection 要包装的真实连接
   * @param dataSource 该连接所属的数据源
   */
  public PooledConnection(Connection connection, PooledDataSource dataSource) {
    this.hashCode = connection.hashCode();
    this.realConnection = connection;
    this.dataSource = dataSource;
    this.createdTimestamp = System.currentTimeMillis();
    this.lastUsedTimestamp = System.currentTimeMillis();
    this.valid = true;
    this.proxyConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), IFACES, this);
  }

  /*
   * Invalidates the connection
   */
  /**
   * 使连接失效。
   * <p>
   * 调用后，该连接将无法再被使用，通常用于连接被关闭或发生错误时。
   * </p>
   */
  public void invalidate() {
    valid = false;
  }

  /*
   * Method to see if the connection is usable
   *
   * @return True if the connection is usable
   */
  /**
   * 检查连接是否可用。
   * <p>
   * 连接可用的条件：标记为有效、真实连接不为 null、且能通过数据源的 ping 检测。
   * </p>
   *
   * @return 如果连接可用返回 true，否则返回 false
   */
  public boolean isValid() {
    return valid && realConnection != null && dataSource.pingConnection(this);
  }

  /*
   * Getter for the *real* connection that this wraps
   *
   * @return The connection
   */
  /**
   * 获取被代理的真实连接对象。
   * <p>
   * 注意：直接操作真实连接会绕过连接池的管理机制。
   * </p>
   *
   * @return 真实的数据库连接
   */
  public Connection getRealConnection() {
    return realConnection;
  }

  /*
   * Getter for the proxy for the connection
   *
   * @return The proxy
   */
  /**
   * 获取代理连接对象。
   * <p>
   * 这是提供给使用者使用的连接，调用其 close() 方法会将连接归还到池中。
   * </p>
   *
   * @return 代理后的数据库连接
   */
  public Connection getProxyConnection() {
    return proxyConnection;
  }

  /*
   * Gets the hashcode of the real connection (or 0 if it is null)
   *
   * @return The hashcode of the real connection (or 0 if it is null)
   */
  /**
   * 获取真实连接的哈希码。
   *
   * @return 真实连接的哈希码，如果真实连接为 null 则返回 0
   */
  public int getRealHashCode() {
    return realConnection == null ? 0 : realConnection.hashCode();
  }

  /*
   * Getter for the connection type (based on url + user + password)
   *
   * @return The connection type
   */
  /**
   * 获取连接类型编码。
   * <p>
   * 该编码基于数据库 URL、用户名和密码生成，用于区分不同配置的连接。
   * </p>
   *
   * @return 连接类型编码
   */
  public int getConnectionTypeCode() {
    return connectionTypeCode;
  }

  /*
   * Setter for the connection type
   *
   * @param connectionTypeCode - the connection type
   */
  /**
   * 设置连接类型编码。
   *
   * @param connectionTypeCode 连接类型编码
   */
  public void setConnectionTypeCode(int connectionTypeCode) {
    this.connectionTypeCode = connectionTypeCode;
  }

  /*
   * Getter for the time that the connection was created
   *
   * @return The creation timestamp
   */
  /**
   * 获取连接创建的时间戳。
   *
   * @return 连接创建时的毫秒时间戳
   */
  public long getCreatedTimestamp() {
    return createdTimestamp;
  }

  /*
   * Setter for the time that the connection was created
   *
   * @param createdTimestamp - the timestamp
   */
  /**
   * 设置连接创建的时间戳。
   *
   * @param createdTimestamp 要设置的毫秒时间戳
   */
  public void setCreatedTimestamp(long createdTimestamp) {
    this.createdTimestamp = createdTimestamp;
  }

  /*
   * Getter for the time that the connection was last used
   *
   * @return - the timestamp
   */
  /**
   * 获取连接最后一次被使用的时间戳。
   *
   * @return 最后使用时间的毫秒时间戳
   */
  public long getLastUsedTimestamp() {
    return lastUsedTimestamp;
  }

  /*
   * Setter for the time that the connection was last used
   *
   * @param lastUsedTimestamp - the timestamp
   */
  /**
   * 设置连接最后被使用的时间戳。
   *
   * @param lastUsedTimestamp 要设置的毫秒时间戳
   */
  public void setLastUsedTimestamp(long lastUsedTimestamp) {
    this.lastUsedTimestamp = lastUsedTimestamp;
  }

  /*
   * Getter for the time since this connection was last used
   *
   * @return - the time since the last use
   */
  /**
   * 获取距离连接上次使用至今经过的时间。
   *
   * @return 经过的毫秒数
   */
  public long getTimeElapsedSinceLastUse() {
    return System.currentTimeMillis() - lastUsedTimestamp;
  }

  /*
   * Getter for the age of the connection
   *
   * @return the age
   */
  /**
   * 获取连接的年龄（从创建至今的时间）。
   *
   * @return 连接存在的毫秒数
   */
  public long getAge() {
    return System.currentTimeMillis() - createdTimestamp;
  }

  /*
   * Getter for the timestamp that this connection was checked out
   *
   * @return the timestamp
   */
  /**
   * 获取连接被取出的时间戳。
   *
   * @return 连接被取出时的毫秒时间戳
   */
  public long getCheckoutTimestamp() {
    return checkoutTimestamp;
  }

  /*
   * Setter for the timestamp that this connection was checked out
   *
   * @param timestamp the timestamp
   */
  /**
   * 设置连接被取出的时间戳。
   *
   * @param timestamp 要设置的毫秒时间戳
   */
  public void setCheckoutTimestamp(long timestamp) {
    this.checkoutTimestamp = timestamp;
  }

  /*
   * Getter for the time that this connection has been checked out
   *
   * @return the time
   */
  /**
   * 获取连接被借出的时长。
   *
   * @return 从借出至今的毫秒数
   */
  public long getCheckoutTime() {
    return System.currentTimeMillis() - checkoutTimestamp;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  /*
   * Allows comparing this connection to another
   *
   * @param obj - the other connection to test for equality
   * @see Object#equals(Object)
   */
  /**
   * 判断当前连接是否与另一个对象相等。
   * <p>
   * 比较规则：
   * <ul>
   *   <li>如果是 PooledConnection 类型，比较真实连接的哈希码</li>
   *   <li>如果是 Connection 类型，比较代理连接的哈希码</li>
   *   <li>其他类型返回 false</li>
   * </ul>
   * </p>
   *
   * @param obj 要比较的对象
   * @return 如果相等返回 true，否则返回 false
   * @see Object#equals(Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PooledConnection) {
      return realConnection.hashCode() == ((PooledConnection) obj).realConnection.hashCode();
    } else if (obj instanceof Connection) {
      return hashCode == obj.hashCode();
    } else {
      return false;
    }
  }

  /*
   * Required for InvocationHandler implementation.
   *
   * @param proxy  - not used
   * @param method - the method to be executed
   * @param args   - the parameters to be passed to the method
   * @see java.lang.reflect.InvocationHandler#invoke(Object, java.lang.reflect.Method, Object[])
   */
  /**
   * 代理连接的方法调用拦截器。
   * <p>
   * 当调用 {@code close()} 方法时，不会真正关闭连接，而是将其归还到连接池；
   * 调用其他方法时，委托给真实连接执行。
   * </p>
   *
   * @param proxy  代理对象（未使用）
   * @param method 要执行的方法
   * @param args   方法参数
   * @return 方法返回值
   * @throws Throwable 方法执行异常
   * @see java.lang.reflect.InvocationHandler#invoke(Object, java.lang.reflect.Method, Object[])
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    if (CLOSE.hashCode() == methodName.hashCode() && CLOSE.equals(methodName)) {
      dataSource.pushConnection(this);
      return null;
    } else {
      try {
        if (!Object.class.equals(method.getDeclaringClass())) {
          // issue #579 toString() should never fail
          // throw an SQLException instead of a Runtime
          checkConnection();
        }
        return method.invoke(realConnection, args);
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    }
  }

  /**
   * 检查连接是否有效。
   * <p>
   * 如果连接已失效，抛出 SQLException 异常。
   * </p>
   *
   * @throws SQLException 当连接无效时抛出
   */
  private void checkConnection() throws SQLException {
    if (!valid) {
      throw new SQLException("Error accessing PooledConnection. Connection is invalid.");
    }
  }

}
