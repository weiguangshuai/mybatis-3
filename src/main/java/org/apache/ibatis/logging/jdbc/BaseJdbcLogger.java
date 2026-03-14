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

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.reflection.ArrayUtil;

/**
 * JDBC 日志代理基类，为 JDBC 操作提供日志记录能力
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public abstract class BaseJdbcLogger {

  /** 用于设置参数值的 JDBC 方法集合 */
  protected static final Set<String> SET_METHODS = new HashSet<String>();
  /** 用于执行 SQL 的 JDBC 方法集合 */
  protected static final Set<String> EXECUTE_METHODS = new HashSet<String>();

  /** 列名到列值的映射，用于记录参数绑定信息 */
  private final Map<Object, Object> columnMap = new HashMap<Object, Object>();

  /** 参数绑定的列名列表 */
  private final List<Object> columnNames = new ArrayList<Object>();
  /** 参数绑定的列值列表 */
  private final List<Object> columnValues = new ArrayList<Object>();

  /** 日志记录器，用于输出 SQL 语句和参数 */
  protected Log statementLog;
  /** 查询嵌套深度，用于格式化日志输出前缀 */
  protected int queryStack;

  /**
   * 构造函数，初始化日志记录器
   *
   * @param log        日志记录器
   * @param queryStack 查询嵌套深度，0 时默认为 1
   */
  public BaseJdbcLogger(Log log, int queryStack) {
    this.statementLog = log;
    if (queryStack == 0) {
      this.queryStack = 1;
    } else {
      this.queryStack = queryStack;
    }
  }

  static {
    SET_METHODS.add("setString");
    SET_METHODS.add("setNString");
    SET_METHODS.add("setInt");
    SET_METHODS.add("setByte");
    SET_METHODS.add("setShort");
    SET_METHODS.add("setLong");
    SET_METHODS.add("setDouble");
    SET_METHODS.add("setFloat");
    SET_METHODS.add("setTimestamp");
    SET_METHODS.add("setDate");
    SET_METHODS.add("setTime");
    SET_METHODS.add("setArray");
    SET_METHODS.add("setBigDecimal");
    SET_METHODS.add("setAsciiStream");
    SET_METHODS.add("setBinaryStream");
    SET_METHODS.add("setBlob");
    SET_METHODS.add("setBoolean");
    SET_METHODS.add("setBytes");
    SET_METHODS.add("setCharacterStream");
    SET_METHODS.add("setNCharacterStream");
    SET_METHODS.add("setClob");
    SET_METHODS.add("setNClob");
    SET_METHODS.add("setObject");
    SET_METHODS.add("setNull");

    EXECUTE_METHODS.add("execute");
    EXECUTE_METHODS.add("executeUpdate");
    EXECUTE_METHODS.add("executeQuery");
    EXECUTE_METHODS.add("addBatch");
  }

  /**
   * 记录参数绑定信息
   *
   * @param key   参数名称
   * @param value 参数值
   */
  protected void setColumn(Object key, Object value) {
    columnMap.put(key, value);
    columnNames.add(key);
    columnValues.add(value);
  }

  /**
   * 根据参数名获取对应的值
   *
   * @param key 参数名称
   * @return 参数值，不存在时返回 null
   */
  protected Object getColumn(Object key) {
    return columnMap.get(key);
  }

  /**
   * 生成参数字符串表示，格式如: value1(type1), value2(type2)
   *
   * @return 参数字符串
   */
  protected String getParameterValueString() {
    List<Object> typeList = new ArrayList<Object>(columnValues.size());
    for (Object value : columnValues) {
      if (value == null) {
        typeList.add("null");
      } else {
        typeList.add(objectValueString(value) + "(" + value.getClass().getSimpleName() + ")");
      }
    }
    final String parameters = typeList.toString();
    // 去掉首尾方括号
    return parameters.substring(1, parameters.length() - 1);
  }

  /**
   * 将对象转换为字符串表示，特殊处理 Array 类型
   *
   * @param value 待转换的对象
   * @return 对象的字符串表示
   */
  protected String objectValueString(Object value) {
    if (value instanceof Array) {
      try {
        return ArrayUtil.toString(((Array) value).getArray());
      } catch (SQLException e) {
        // 转换失败时使用 toString
        return value.toString();
      }
    }
    return value.toString();
  }

  /**
   * 获取所有列名组成的字符串
   *
   * @return 列名字符串
   */
  protected String getColumnString() {
    return columnNames.toString();
  }

  /**
   * 清除所有记录的列信息，用于每次执行前重置状态
   */
  protected void clearColumnInfo() {
    columnMap.clear();
    columnNames.clear();
    columnValues.clear();
  }

  /**
   * 移除字符串中的换行符，将连续空白符替换为单个空格
   *
   * @param original 原始字符串
   * @return 处理后的字符串
   */
  protected String removeBreakingWhitespace(String original) {
    StringTokenizer whitespaceStripper = new StringTokenizer(original);
    StringBuilder builder = new StringBuilder();
    while (whitespaceStripper.hasMoreTokens()) {
      builder.append(whitespaceStripper.nextToken());
      builder.append(" ");
    }
    return builder.toString();
  }

  /**
   * 判断调试日志是否启用
   *
   * @return 是否启用调试日志
   */
  protected boolean isDebugEnabled() {
    return statementLog.isDebugEnabled();
  }

  /**
   * 判断跟踪日志是否启用
   *
   * @return 是否启用跟踪日志
   */
  protected boolean isTraceEnabled() {
    return statementLog.isTraceEnabled();
  }

  /**
   * 输出调试日志
   *
   * @param text  日志内容
   * @param input true 表示输入参数，false 表示输出结果
   */
  protected void debug(String text, boolean input) {
    if (statementLog.isDebugEnabled()) {
      statementLog.debug(prefix(input) + text);
    }
  }

  /**
   * 输出跟踪日志
   *
   * @param text  日志内容
   * @param input true 表示输入参数，false 表示输出结果
   */
  protected void trace(String text, boolean input) {
    if (statementLog.isTraceEnabled()) {
      statementLog.trace(prefix(input) + text);
    }
  }

  /**
   * 生成日志前缀，用于区分输入输出
   *
   * @param isInput true 表示输入，false 表示输出
   * @return 格式化的前缀字符串
   */
  private String prefix(boolean isInput) {
    char[] buffer = new char[queryStack * 2 + 2];
    Arrays.fill(buffer, '=');
    buffer[queryStack * 2 + 1] = ' ';
    if (isInput) {
      buffer[queryStack * 2] = '>';
    } else {
      buffer[0] = '<';
    }
    return new String(buffer);
  }

}
