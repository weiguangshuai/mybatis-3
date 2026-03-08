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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * {@link ResultSet} 的日志代理实现。
 * <p>
 * 该类在遍历结果集时输出列头、每一行的显示值以及最终总行数。对于二进制、大文本等不适合直接打印的列，
 * 它会做特殊标记，避免日志膨胀或触发不必要的 JDBC 读取异常。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 *
 */
public final class ResultSetLogger extends BaseJdbcLogger implements InvocationHandler {

  /**
   * 需要以占位文本输出的列类型集合。
   * <p>
   * 这些类型通常体积较大，或不适合直接通过 {@code getString()} 安全展示。
   */
  private static Set<Integer> BLOB_TYPES = new HashSet<Integer>();

  /**
   * 是否仍处于结果集首行之前。
   * <p>
   * 用于确保列头只打印一次。
   */
  private boolean first = true;

  /**
   * 已经遍历并输出的行数。
   */
  private int rows;

  /**
   * 被代理的真实结果集对象。
   */
  private final ResultSet rs;

  /**
   * 当前结果集中需要按 BLOB 占位符显示的列序号集合。
   */
  private final Set<Integer> blobColumns = new HashSet<Integer>();

  static {
    // 这些 JDBC 类型通常不适合在日志中直接展示真实内容。
    BLOB_TYPES.add(Types.BINARY);
    BLOB_TYPES.add(Types.BLOB);
    BLOB_TYPES.add(Types.CLOB);
    BLOB_TYPES.add(Types.LONGNVARCHAR);
    BLOB_TYPES.add(Types.LONGVARBINARY);
    BLOB_TYPES.add(Types.LONGVARCHAR);
    BLOB_TYPES.add(Types.NCLOB);
    BLOB_TYPES.add(Types.VARBINARY);
  }

  /**
   * 创建结果集日志代理处理器。
   *
   * @param rs
   *          真实结果集
   * @param statementLog
   *          日志输出器
   * @param queryStack
   *          当前查询堆栈深度
   */
  private ResultSetLogger(ResultSet rs, Log statementLog, int queryStack) {
    super(statementLog, queryStack);
    this.rs = rs;
  }

  /**
   * 拦截结果集方法调用，并在遍历行数据时输出列名、列值和总行数。
   *
   * @param proxy
   *          代理对象本身
   * @param method
   *          当前调用的方法
   * @param params
   *          调用参数，可能为 {@code null}
   * @return 原方法返回值
   * @throws Throwable
   *          底层真实异常，抛出前会解包反射包装
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
    try {
      // Object 的基础方法不需要转发到结果集对象。
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, params);
      }
      Object o = method.invoke(rs, params);
      if ("next".equals(method.getName())) {
        if (((Boolean) o)) {
          rows++;
          if (isTraceEnabled()) {
            ResultSetMetaData rsmd = rs.getMetaData();
            final int columnCount = rsmd.getColumnCount();
            if (first) {
              first = false;
              // 首次读取到有效行时打印列头，并识别需要特殊展示的列类型。
              printColumnHeaders(rsmd, columnCount);
            }
            // 每次游标前进到有效行时都打印当前行的展示值。
            printColumnValues(columnCount);
          }
        } else {
          // next() 返回 false 代表遍历结束，此时输出总行数汇总。
          debug("     Total: " + rows, false);
        }
      }
      // 结果集游标前进后，上一次行数据对应的列缓存已经不再需要。
      clearColumnInfo();
      return o;
    } catch (Throwable t) {
      // 保持向上抛出真实异常，减少反射层噪音。
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }

  /**
   * 输出结果集列头，并记录哪些列需要以占位方式展示。
   *
   * @param rsmd
   *          结果集元数据
   * @param columnCount
   *          列数量
   * @throws SQLException
   *          读取元数据时可能抛出的 SQL 异常
   */
  private void printColumnHeaders(ResultSetMetaData rsmd, int columnCount) throws SQLException {
    StringBuilder row = new StringBuilder();
    row.append("   Columns: ");
    for (int i = 1; i <= columnCount; i++) {
      if (BLOB_TYPES.contains(rsmd.getColumnType(i))) {
        // 记录大对象列，后续输出行数据时避免直接取值打印。
        blobColumns.add(i);
      }
      String colname = rsmd.getColumnLabel(i);
      row.append(colname);
      if (i != columnCount) {
        row.append(", ");
      }
    }
    trace(row.toString(), false);
  }

  /**
   * 输出当前行的各列展示值。
   *
   * @param columnCount
   *          列数量
   */
  private void printColumnValues(int columnCount) {
    StringBuilder row = new StringBuilder();
    row.append("       Row: ");
    for (int i = 1; i <= columnCount; i++) {
      String colname;
      try {
        if (blobColumns.contains(i)) {
          // 大对象列不直接打印真实内容，避免日志过大或无意义输出。
          colname = "<<BLOB>>";
        } else {
          colname = rs.getString(i);
        }
      } catch (SQLException e) {
        // 某些驱动即使列类型未命中，也可能不允许直接按字符串方式读取。
        colname = "<<Cannot Display>>";
      }
      row.append(colname);
      if (i != columnCount) {
        row.append(", ");
      }
    }
    trace(row.toString(), false);
  }

  /**
   * 为结果集创建带日志能力的代理实例。
   *
   * @param rs
   *          原始结果集
   * @param statementLog
   *          日志输出器
   * @param queryStack
   *          当前查询堆栈深度
   * @return 包装后的结果集代理
   */
  public static ResultSet newInstance(ResultSet rs, Log statementLog, int queryStack) {
    InvocationHandler handler = new ResultSetLogger(rs, statementLog, queryStack);
    ClassLoader cl = ResultSet.class.getClassLoader();
    return (ResultSet) Proxy.newProxyInstance(cl, new Class[]{ResultSet.class}, handler);
  }

  /**
   * 返回当前代理包装的真实结果集。
   *
   * @return 底层 {@link ResultSet} 对象
   */
  public ResultSet getRs() {
    return rs;
  }

}
