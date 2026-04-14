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

import org.apache.ibatis.reflection.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 包装数据库连接以提供连接池功能的代理类。
 *
 * @author Clinton Begin
 */
class PooledConnection implements InvocationHandler {

  /**
   * 用于识别连接的方法名
   */
  private static final String CLOSE = "close";
  /**
   * 代理接口类型
   */
  private static final Class<?>[] IFACES = new Class<?>[]{Connection.class};

  /**
   * 标识此连接池对象的哈希码
   */
  private final int hashCode;
  /**
   * 创建此连接的数据源
   */
  private final PooledDataSource dataSource;
  /**
   * 真实的数据库连接
   */
  private final Connection realConnection;
  /**
   * 代理连接，用于拦截 close 方法
   */
  private final Connection proxyConnection;
  /**
   * 记录连接被借出的时间戳
   */
  private long checkoutTimestamp;
  /**
   * 记录连接池对象创建的时间戳
   */
  private long createdTimestamp;
  /**
   * 记录连接最后一次使用的时间戳
   */
  private long lastUsedTimestamp;
  /**
   * 连接类型标识符，由 URL+用户名+密码生成
   */
  private int connectionTypeCode;
  /**
   * 标识连接是否有效
   */
  private boolean valid;

  /**
   * 使用给定的数据库连接和数据源构造池化连接对象。
   *
   * @param connection 要被包装为池化连接的数据库连接
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

  /**
   * 使连接失效，后续使用该连接时会抛出异常。
   */
  public void invalidate() {
    valid = false;
  }

  /**
   * 检查连接是否可用。
   *
   * @return 连接可用返回 true，否则返回 false
   */
  public boolean isValid() {
    return valid && realConnection != null && dataSource.pingConnection(this);
  }


  /**
   * 获取被包装的真实数据库连接。
   *
   * @return 真实的数据库连接
   */
  public Connection getRealConnection() {
    return realConnection;
  }

  /**
   * 获取代理连接对象。
   *
   * @return 代理连接
   */
  public Connection getProxyConnection() {
    return proxyConnection;
  }

  /**
   * 获取真实连接的哈希码，如果连接为 null 则返回 0。
   *
   * @return 真实连接的哈希码
   */
  public int getRealHashCode() {
    return realConnection == null ? 0 : realConnection.hashCode();
  }

  /**
   * 获取连接类型标识符（基于 URL+用户名+密码生成）。
   *
   * @return 连接类型标识符
   */
  public int getConnectionTypeCode() {
    return connectionTypeCode;
  }

  /**
   * 设置连接类型标识符。
   *
   * @param connectionTypeCode 连接类型标识符
   */
  public void setConnectionTypeCode(int connectionTypeCode) {
    this.connectionTypeCode = connectionTypeCode;
  }

  /**
   * 获取连接池对象的创建时间戳。
   *
   * @return 创建时间戳
   */
  public long getCreatedTimestamp() {
    return createdTimestamp;
  }

  /**
   * 设置连接池对象的创建时间戳。
   *
   * @param createdTimestamp 创建时间戳
   */
  public void setCreatedTimestamp(long createdTimestamp) {
    this.createdTimestamp = createdTimestamp;
  }

  /**
   * 获取连接最后一次使用的时间戳。
   *
   * @return 最后使用时间戳
   */
  public long getLastUsedTimestamp() {
    return lastUsedTimestamp;
  }

  /**
   * 设置连接最后一次使用的时间戳。
   *
   * @param lastUsedTimestamp 最后使用时间戳
   */
  public void setLastUsedTimestamp(long lastUsedTimestamp) {
    this.lastUsedTimestamp = lastUsedTimestamp;
  }

  /**
   * 获取连接距离上次使用经过的时间（毫秒）。
   *
   * @return 经过的时间（毫秒）
   */
  public long getTimeElapsedSinceLastUse() {
    return System.currentTimeMillis() - lastUsedTimestamp;
  }

  /**
   * 获取连接的存活时间（毫秒），即从创建到现在经过的时间。
   *
   * @return 存活时间（毫秒）
   */
  public long getAge() {
    return System.currentTimeMillis() - createdTimestamp;
  }

  /**
   * 获取连接被借出的时间戳。
   *
   * @return 借出时间戳
   */
  public long getCheckoutTimestamp() {
    return checkoutTimestamp;
  }

  /**
   * 设置连接被借出的时间戳。
   *
   * @param timestamp 借出时间戳
   */
  public void setCheckoutTimestamp(long timestamp) {
    this.checkoutTimestamp = timestamp;
  }

  /**
   * 获取连接已被借出的时长（毫秒）。
   *
   * @return 借出时长（毫秒）
   */
  public long getCheckoutTime() {
    return System.currentTimeMillis() - checkoutTimestamp;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * 比较此连接与其他对象是否相等。
   *
   * @param obj 要比较的对象
   * @return 相等返回 true，否则返回 false
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

  /**
   * InvocationHandler 接口实现，拦截代理连接的方法调用。
   * 特殊处理 close 方法，将其重定向到连接池归还逻辑。
   *
   * @param proxy  代理对象（未使用）
   * @param method 被调用的方法
   * @param args   方法参数
   * @return 方法执行结果
   * @throws Throwable 方法执行可能抛出的异常
   * @see java.lang.reflect.InvocationHandler#invoke(Object, java.lang.reflect.Method, Object[])
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    // 调用 close 方法时，将连接归还到连接池
    if (CLOSE.equals(methodName)) {
      dataSource.pushConnection(this);
      return null;
    }
    try {
      // 非 Object 类声明的方法（如 toString）需要检查连接有效性
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

  /**
   * 检查连接是否有效
   */
  private void checkConnection() throws SQLException {
    if (!valid) {
      throw new SQLException("Error accessing PooledConnection. Connection is invalid.");
    }
  }

}
