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

import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.reflection.ArrayUtil;

/**
 * JDBC 日志记录基类，为数据库操作代理提供日志支持。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public abstract class BaseJdbcLogger {

  /** 存储 PreparedStatement 的 set* 方法名，用于识别参数设置操作 */
  protected static final Set<String> SET_METHODS;
  /** 存储执行方法名，用于识别 SQL 执行操作 */
  protected static final Set<String> EXECUTE_METHODS = new HashSet<>();

  /** 列名到列值的映射表 */
  private final Map<Object, Object> columnMap = new HashMap<>();

  /** 记录列名顺序 */
  private final List<Object> columnNames = new ArrayList<>();
  /** 记录列值顺序 */
  private final List<Object> columnValues = new ArrayList<>();

  /** 语句日志记录器 */
  protected final Log statementLog;
  /** 查询嵌套层级，用于生成日志缩进前缀 */
  protected final int queryStack;

  /*
   * Default constructor
   */
  /**
   * 构造基类实例，初始化日志器和查询层级。
   *
   * @param log        日志记录器
   * @param queryStack 查询嵌套层级
   */
  public BaseJdbcLogger(Log log, int queryStack) {
    this.statementLog = log;
    // 确保查询层级至少为 1，避免前缀生成异常
    if (queryStack == 0) {
      this.queryStack = 1;
    } else {
      this.queryStack = queryStack;
    }
  }

  static {
    // 反射获取 PreparedStatement 的 set 方法（参数个数 > 1，排除 setNull 等单参数方法）
    SET_METHODS = Arrays.stream(PreparedStatement.class.getDeclaredMethods())
            .filter(method -> method.getName().startsWith("set"))
            .filter(method -> method.getParameterCount() > 1)
            .map(Method::getName)
            .collect(Collectors.toSet());

    // 记录需要拦截的 SQL 执行方法
    EXECUTE_METHODS.add("execute");
    EXECUTE_METHODS.add("executeUpdate");
    EXECUTE_METHODS.add("executeQuery");
    EXECUTE_METHODS.add("addBatch");
  }

  /**
   * 记录列名和列值的对应关系。
   *
   * @param key   列名
   * @param value 列值
   */
  protected void setColumn(Object key, Object value) {
    columnMap.put(key, value);
    columnNames.add(key);
    columnValues.add(value);
  }

  /**
   * 根据列名获取列值。
   *
   * @param key 列名
   * @return 列值，不存在则返回 null
   */
  protected Object getColumn(Object key) {
    return columnMap.get(key);
  }

  /**
   * 生成参数值的可读字符串，格式为：值(类型), 值(类型), ...
   *
   * @return 参数字符串
   */
  protected String getParameterValueString() {
    List<Object> typeList = new ArrayList<>(columnValues.size());
    for (Object value : columnValues) {
      if (value == null) {
        typeList.add("null");
      } else {
        typeList.add(objectValueString(value) + "(" + value.getClass().getSimpleName() + ")");
      }
    }
    // 移除 List.toString() 生成的首尾方括号
    final String parameters = typeList.toString();
    return parameters.substring(1, parameters.length() - 1);
  }

  /**
   * 将对象转换为可读字符串，特殊处理数组类型。
   *
   * @param value 要转换的对象
   * @return 对象的字符串表示
   */
  protected String objectValueString(Object value) {
    // 特殊处理 SQL 数组类型，转换为可读字符串
    if (value instanceof Array) {
      try {
        return ArrayUtil.toString(((Array) value).getArray());
      } catch (SQLException e) {
        // 转换失败时降级为普通 toString
        return value.toString();
      }
    }
    return value.toString();
  }

  /**
   * 获取列名列表的字符串表示。
   *
   * @return 列名字符串
   */
  protected String getColumnString() {
    return columnNames.toString();
  }

  /**
   * 清空所有列信息，包括列名和列值的记录。
   */
  protected void clearColumnInfo() {
    columnMap.clear();
    columnNames.clear();
    columnValues.clear();
  }

  /**
   * 移除 SQL 语句中的多余空白字符。
   *
   * @param original 原始 SQL 语句
   * @return 处理后的 SQL 语句
   */
  protected String removeExtraWhitespace(String original) {
    return SqlSourceBuilder.removeExtraWhitespaces(original);
  }

  /**
   * 检查调试日志是否启用。
   *
   * @return 是否启用调试日志
   */
  protected boolean isDebugEnabled() {
    return statementLog.isDebugEnabled();
  }

  /**
   * 检查跟踪日志是否启用。
   *
   * @return 是否启用跟踪日志
   */
  protected boolean isTraceEnabled() {
    return statementLog.isTraceEnabled();
  }

  /**
   * 输出调试日志。
   *
   * @param text  日志文本
   * @param input true 表示输入参数，false 表示返回结果
   */
  protected void debug(String text, boolean input) {
    if (statementLog.isDebugEnabled()) {
      statementLog.debug(prefix(input) + text);
    }
  }

  /**
   * 输出跟踪日志。
   *
   * @param text  日志文本
   * @param input true 表示输入参数，false 表示返回结果
   */
  protected void trace(String text, boolean input) {
    if (statementLog.isTraceEnabled()) {
      statementLog.trace(prefix(input) + text);
    }
  }

  private String prefix(boolean isInput) {
    // 生成日志前缀，如 "==> "（输入）或 "<== "（输出），层级越深前缀越长
    char[] buffer = new char[queryStack * 2 + 2];
    Arrays.fill(buffer, '=');
    buffer[queryStack * 2 + 1] = ' ';
    // 输入参数用 > 结尾，输出结果用 < 开头
    if (isInput) {
      buffer[queryStack * 2] = '>';
    } else {
      buffer[0] = '<';
    }
    return new String(buffer);
  }

}
