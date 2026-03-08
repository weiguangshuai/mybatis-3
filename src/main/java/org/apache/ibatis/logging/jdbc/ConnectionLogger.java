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
package org.apache.ibatis.logging.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * {@link Connection} 的日志代理实现。
 * <p>
 * 该类基于 JDK 动态代理拦截连接对象上的调用，在创建 {@link PreparedStatement}、存储过程调用或普通
 * {@link Statement} 时，包装对应的 JDBC 对象并补充 SQL 日志输出。这样可以在不改变调用方代码的前提下，
 * 将连接级别后续创建出的语句对象统一接入 MyBatis 的 JDBC 日志体系。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 *
 */
public final class ConnectionLogger extends BaseJdbcLogger implements InvocationHandler {

  /**
   * 被代理的真实数据库连接。
   * <p>
   * 该引用在构造后不再变化，所有未被特殊处理的方法最终都会委托给该连接执行。
   */
  private final Connection connection;

  /**
   * 基于真实连接创建日志代理处理器。
   *
   * @param conn
   *          真实 JDBC 连接
   * @param statementLog
   *          语句日志输出器
   * @param queryStack
   *          当前查询堆栈深度，用于格式化嵌套日志缩进
   */
  private ConnectionLogger(Connection conn, Log statementLog, int queryStack) {
    super(statementLog, queryStack);
    this.connection = conn;
  }

  /**
   * 拦截连接方法调用，并在创建语句对象时注入对应的日志代理。
   *
   * @param proxy
   *          代理对象本身
   * @param method
   *          当前被调用的方法
   * @param params
   *          调用参数，可能为 {@code null}
   * @return 方法执行结果；若创建的是语句对象，则返回包装后的代理实例
   * @throws Throwable
   *          底层反射调用抛出的异常，返回前会解包为真实原因
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] params)
      throws Throwable {
    try {
      // Object 基础方法直接由当前 InvocationHandler 处理，避免继续委托到底层连接。
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, params);
      }

      // 预编译语句需要先打印 SQL，再包装成支持参数日志输出的代理对象。
      if ("prepareStatement".equals(method.getName())) {
        if (isDebugEnabled()) {
          debug(" Preparing: " + removeBreakingWhitespace((String) params[0]), true);
        }
        PreparedStatement stmt = (PreparedStatement) method.invoke(connection, params);
        stmt = PreparedStatementLogger.newInstance(stmt, statementLog, queryStack);
        return stmt;

      // 存储过程调用同样复用 PreparedStatement 的日志代理能力。
      } else if ("prepareCall".equals(method.getName())) {
        if (isDebugEnabled()) {
          debug(" Preparing: " + removeBreakingWhitespace((String) params[0]), true);
        }
        PreparedStatement stmt = (PreparedStatement) method.invoke(connection, params);
        stmt = PreparedStatementLogger.newInstance(stmt, statementLog, queryStack);
        return stmt;

      // 普通 Statement 没有参数绑定过程，包装为专用的 Statement 日志代理即可。
      } else if ("createStatement".equals(method.getName())) {
        Statement stmt = (Statement) method.invoke(connection, params);
        stmt = StatementLogger.newInstance(stmt, statementLog, queryStack);
        return stmt;
      } else {
        // 其余连接操作保持原始语义，不做额外增强。
        return method.invoke(connection, params);
      }
    } catch (Throwable t) {
      // 反射调用通常会包裹 InvocationTargetException，这里统一解包成真实异常。
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }

  /**
   * 为真实连接创建带日志能力的代理实例。
   *
   * @param conn
   *          原始 JDBC 连接
   * @param statementLog
   *          语句日志输出器
   * @param queryStack
   *          当前查询堆栈深度
   * @return 包装后的连接代理
   */
  public static Connection newInstance(Connection conn, Log statementLog, int queryStack) {
    InvocationHandler handler = new ConnectionLogger(conn, statementLog, queryStack);
    ClassLoader cl = Connection.class.getClassLoader();
    return (Connection) Proxy.newProxyInstance(cl, new Class[]{Connection.class}, handler);
  }

  /**
   * 返回当前代理包装的真实连接。
   *
   * @return 底层 JDBC 连接对象
   */
  public Connection getConnection() {
    return connection;
  }

}
