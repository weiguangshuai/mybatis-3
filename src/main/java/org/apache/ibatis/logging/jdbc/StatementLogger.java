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
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * {@link Statement} 的日志代理实现。
 * <p>
 * 该类负责拦截普通 SQL 语句的执行过程，在执行前输出最终 SQL 文本，并在查询返回结果集时继续包装为
 * {@link ResultSet} 日志代理。由于 {@link Statement} 不具备参数绑定能力，因此它主要记录的是直接执行的
 * SQL 字符串本身。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 *
 */
public final class StatementLogger extends BaseJdbcLogger implements InvocationHandler {

  /**
   * 被代理的真实 Statement 对象。
   */
  private final Statement statement;

  /**
   * 创建普通语句日志代理处理器。
   *
   * @param stmt
   *          真实 Statement
   * @param statementLog
   *          日志输出器
   * @param queryStack
   *          当前查询堆栈深度
   */
  private StatementLogger(Statement stmt, Log statementLog, int queryStack) {
    super(statementLog, queryStack);
    this.statement = stmt;
  }

  /**
   * 拦截 Statement 方法调用，并在执行 SQL 或获取结果集时附加日志能力。
   *
   * @param proxy
   *          代理对象本身
   * @param method
   *          当前调用的方法
   * @param params
   *          调用参数，可能为 {@code null}
   * @return 原方法返回值；若为结果集则返回包装后的代理对象
   * @throws Throwable
   *          底层真实异常，抛出前会移除反射包装层
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
    try {
      // equals/hashCode/toString 等 Object 方法由当前处理器直接处理。
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, params);
      }

      // 执行类方法优先打印 SQL，便于与后续结果日志关联。
      if (EXECUTE_METHODS.contains(method.getName())) {
        if (isDebugEnabled()) {
          debug(" Executing: " + removeBreakingWhitespace((String) params[0]), true);
        }
        if ("executeQuery".equals(method.getName())) {
          // 查询结果继续包装，输出列头、行数据与总行数。
          ResultSet rs = (ResultSet) method.invoke(statement, params);
          return rs == null ? null : ResultSetLogger.newInstance(rs, statementLog, queryStack);
        } else {
          return method.invoke(statement, params);
        }

      // 兼容先执行再单独获取结果集的调用方式。
      } else if ("getResultSet".equals(method.getName())) {
        ResultSet rs = (ResultSet) method.invoke(statement, params);
        return rs == null ? null : ResultSetLogger.newInstance(rs, statementLog, queryStack);
      } else {
        return method.invoke(statement, params);
      }
    } catch (Throwable t) {
      // 统一向上抛出真实异常，隐藏反射调用细节。
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }

  /**
   * 为普通语句创建带日志能力的代理实例。
   *
   * @param stmt
   *          原始 Statement
   * @param statementLog
   *          日志输出器
   * @param queryStack
   *          当前查询堆栈深度
   * @return 包装后的 Statement 代理
   */
  public static Statement newInstance(Statement stmt, Log statementLog, int queryStack) {
    InvocationHandler handler = new StatementLogger(stmt, statementLog, queryStack);
    ClassLoader cl = Statement.class.getClassLoader();
    return (Statement) Proxy.newProxyInstance(cl, new Class[]{Statement.class}, handler);
  }

  /**
   * 返回当前代理包装的真实 Statement。
   *
   * @return 底层 Statement 对象
   */
  public Statement getStatement() {
    return statement;
  }

}
