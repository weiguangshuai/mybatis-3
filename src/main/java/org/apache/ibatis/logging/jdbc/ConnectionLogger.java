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
 * Connection 的代理类，用于在执行 SQL 准备操作时记录日志。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 *
 */
public final class ConnectionLogger extends BaseJdbcLogger implements InvocationHandler {

  /** 被代理的真实数据库连接 */
  private final Connection connection;

  /**
   * 构造方法，初始化日志记录器并保存真实连接。
   *
   * @param conn 原始数据库连接
   * @param statementLog 语句日志记录器
   * @param queryStack 查询堆栈层级
   */
  private ConnectionLogger(Connection conn, Log statementLog, int queryStack) {
    super(statementLog, queryStack);
    this.connection = conn;
  }

  /**
   * 处理被代理Connection对象上的方法调用。
   * 当调用 prepareStatement 或 prepareCall 时记录 SQL 语句；
   * 当调用 createStatement 时包装返回的 Statement 对象。
   *
   * @param proxy 代理对象
   * @param method 被调用的方法
   * @param params 方法参数
   * @return 方法执行结果
   * @throws Throwable 方法执行过程中的异常
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] params)
      throws Throwable {
    try {
      // Object 类的方法直接调用，不做任何包装
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, params);
      }
      // 预处理 SQL 语句的方法，输出日志并包装 PreparedStatement
      if ("prepareStatement".equals(method.getName()) || "prepareCall".equals(method.getName())) {
        if (isDebugEnabled()) {
          debug(" Preparing: " + removeExtraWhitespace((String) params[0]), true);
        }
        PreparedStatement stmt = (PreparedStatement) method.invoke(connection, params);
        // 返回包装后的 PreparedStatement，用于记录后续执行日志
        stmt = PreparedStatementLogger.newInstance(stmt, statementLog, queryStack);
        return stmt;
      } else if ("createStatement".equals(method.getName())) {
        // 创建普通 Statement，包装后返回
        Statement stmt = (Statement) method.invoke(connection, params);
        stmt = StatementLogger.newInstance(stmt, statementLog, queryStack);
        return stmt;
      } else {
        // 其他方法直接转发给真实连接
        return method.invoke(connection, params);
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }

  /**
   * 创建带日志功能的 Connection 代理对象。
   *
   * @param conn 原始数据库连接
   * @param statementLog 语句日志记录器
   * @param queryStack 查询堆栈层级
   * @return 带日志功能的 Connection 代理
   */
  public static Connection newInstance(Connection conn, Log statementLog, int queryStack) {
    InvocationHandler handler = new ConnectionLogger(conn, statementLog, queryStack);
    ClassLoader cl = Connection.class.getClassLoader();
    return (Connection) Proxy.newProxyInstance(cl, new Class[]{Connection.class}, handler);
  }

  /**
   * 获取被代理的真实数据库连接。
   *
   * @return 原始 Connection 对象
   */
  public Connection getConnection() {
    return connection;
  }

}
