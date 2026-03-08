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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * {@link PreparedStatement} 的日志代理实现。
 * <p>
 * 该类负责拦截预编译语句的参数设置与执行过程：一方面记录通过各种 {@code setXxx()} 方法绑定的参数，
 * 另一方面在执行查询或更新前输出参数日志，并在查询返回 {@link ResultSet} 时继续包装为结果集日志代理。
 * 这样可以把 SQL 模板、参数和值结果串联起来，便于排查 JDBC 层问题。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 *
 */
public final class PreparedStatementLogger extends BaseJdbcLogger implements InvocationHandler {

  /**
   * 被代理的真实预编译语句对象。
   * <p>
   * 生命周期与当前代理一致，所有实际 JDBC 调用最终都会委托给它执行。
   */
  private final PreparedStatement statement;

  /**
   * 创建预编译语句日志代理处理器。
   *
   * @param stmt
   *          真实预编译语句
   * @param statementLog
   *          日志输出器
   * @param queryStack
   *          当前查询堆栈深度，用于控制日志缩进层级
   */
  private PreparedStatementLogger(PreparedStatement stmt, Log statementLog, int queryStack) {
    super(statementLog, queryStack);
    this.statement = stmt;
  }

  /**
   * 拦截预编译语句调用，并在参数绑定、执行和结果集访问时补充日志能力。
   *
   * @param proxy
   *          代理对象本身
   * @param method
   *          当前调用的方法
   * @param params
   *          方法参数，可能为 {@code null}
   * @return 原方法返回值，若结果为结果集则返回包装后的代理对象
   * @throws Throwable
   *          反射调用产生的底层异常，抛出前会解包为真实原因
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
    try {
      // Object 的基础方法交由当前处理器执行，避免继续透传到底层 statement。
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, params);
      }

      // 执行类方法需要先打印已经收集到的参数，再清理本次执行前缓存的列信息。
      if (EXECUTE_METHODS.contains(method.getName())) {
        if (isDebugEnabled()) {
          debug("Parameters: " + getParameterValueString(), true);
        }
        clearColumnInfo();
        if ("executeQuery".equals(method.getName())) {
          // 查询结果继续包装，以便逐行输出列名和列值。
          ResultSet rs = (ResultSet) method.invoke(statement, params);
          return rs == null ? null : ResultSetLogger.newInstance(rs, statementLog, queryStack);
        } else {
          return method.invoke(statement, params);
        }

      // setXxx 方法用于记录占位符参数值，供执行前统一输出。
      } else if (SET_METHODS.contains(method.getName())) {
        if ("setNull".equals(method.getName())) {
          setColumn(params[0], null);
        } else {
          setColumn(params[0], params[1]);
        }
        return method.invoke(statement, params);

      // 某些调用链会在执行后单独获取结果集，这里同样补上结果集日志代理。
      } else if ("getResultSet".equals(method.getName())) {
        ResultSet rs = (ResultSet) method.invoke(statement, params);
        return rs == null ? null : ResultSetLogger.newInstance(rs, statementLog, queryStack);

      // 受影响较小的其他方法保持原始行为。
      } else if ("getUpdateCount".equals(method.getName())) {
        int updateCount = (Integer) method.invoke(statement, params);
        if (updateCount != -1) {
          debug("   Updates: " + updateCount, false);
        }
        return updateCount;
      } else {
        return method.invoke(statement, params);
      }
    } catch (Throwable t) {
      // 统一解包反射层异常，避免调用方看到包装后的 InvocationTargetException。
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }

  /**
   * 为预编译语句创建带日志能力的代理实例。
   *
   * @param stmt
   *          原始预编译语句
   * @param statementLog
   *          日志输出器
   * @param queryStack
   *          当前查询堆栈深度
   * @return 包装后的预编译语句代理
   */
  public static PreparedStatement newInstance(PreparedStatement stmt, Log statementLog, int queryStack) {
    InvocationHandler handler = new PreparedStatementLogger(stmt, statementLog, queryStack);
    ClassLoader cl = PreparedStatement.class.getClassLoader();
    return (PreparedStatement) Proxy.newProxyInstance(cl, new Class[]{PreparedStatement.class, CallableStatement.class}, handler);
  }

  /**
   * 返回当前代理包装的真实预编译语句。
   *
   * @return 底层 {@link PreparedStatement} 对象
   */
  public PreparedStatement getPreparedStatement() {
    return statement;
  }

}
