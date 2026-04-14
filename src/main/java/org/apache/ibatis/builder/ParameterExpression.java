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
package org.apache.ibatis.builder;

import java.util.HashMap;

/**
 * 内联参数表达式解析器。
 * 将MyBatis内联参数解析为属性名、JDBC类型及属性键值对。
 *
 * @author Frank D. Martinez [mnesarco]
 */
public class ParameterExpression extends HashMap<String, String> {

  /** 序列化版本UID */
  private static final long serialVersionUID = -2417552199605158680L;

  /**
   * 解析内联参数表达式。
   * @param expression 内联参数表达式，如 "id,jdbcType=INTEGER" 或 "(表达式),jdbcType=VARCHAR"
   */
  public ParameterExpression(String expression) {
    parse(expression);
  }

  /** 解析表达式，判断是表达式语法还是属性语法 */
  private void parse(String expression) {
    int p = skipWS(expression, 0);
    // 以 '(' 开头表示表达式语法，否则为属性语法
    if (expression.charAt(p) == '(') {
      expression(expression, p + 1);
    } else {
      property(expression, p);
    }
  }

  /** 解析表达式语法 "(expression),jdbcType,attr" */
  private void expression(String expression, int left) {
    // 匹配括号，找到对应的闭合括号
    int match = 1;
    int right = left + 1;
    while (match > 0) {
      if (expression.charAt(right) == ')') {
        match--;
      } else if (expression.charAt(right) == '(') {
        match++;
      }
      right++;
    }
    put("expression", expression.substring(left, right - 1));
    jdbcTypeOpt(expression, right);
  }

  /** 解析属性语法 "propertyName,jdbcType,attr" */
  private void property(String expression, int left) {
    if (left < expression.length()) {
      int right = skipUntil(expression, left, ",:");
      put("property", trimmedStr(expression, left, right));
      jdbcTypeOpt(expression, right);
    }
  }

  /** 跳过空白字符，返回第一个非空白字符的位置 */
  private int skipWS(String expression, int p) {
    for (int i = p; i < expression.length(); i++) {
      if (expression.charAt(i) > 0x20) {
        return i;
      }
    }
    return expression.length();
  }

  /** 跳过字符直到遇到指定字符之一，返回该字符位置 */
  private int skipUntil(String expression, int p, final String endChars) {
    for (int i = p; i < expression.length(); i++) {
      char c = expression.charAt(i);
      if (endChars.indexOf(c) > -1) {
        return i;
      }
    }
    return expression.length();
  }

  /** 处理JDBC类型或属性选项 */
  private void jdbcTypeOpt(String expression, int p) {
    p = skipWS(expression, p);
    if (p < expression.length()) {
      // ':' 后面是JDBC类型，',' 后面是属性选项
      if (expression.charAt(p) == ':') {
        jdbcType(expression, p + 1);
      } else if (expression.charAt(p) == ',') {
        option(expression, p + 1);
      } else {
        throw new BuilderException("Parsing error in {" + expression + "} in position " + p);
      }
    }
  }

  /** 解析JDBC类型 */
  private void jdbcType(String expression, int p) {
    int left = skipWS(expression, p);
    int right = skipUntil(expression, left, ",");
    if (right > left) {
      put("jdbcType", trimmedStr(expression, left, right));
    } else {
      throw new BuilderException("Parsing error in {" + expression + "} in position " + p);
    }
    option(expression, right + 1);
  }

  /** 递归解析属性选项，格式为 key=value,key=value,... */
  private void option(String expression, int p) {
    int left = skipWS(expression, p);
    if (left < expression.length()) {
      int right = skipUntil(expression, left, "=");
      String name = trimmedStr(expression, left, right);
      left = right + 1;
      right = skipUntil(expression, left, ",");
      String value = trimmedStr(expression, left, right);
      put(name, value);
      option(expression, right + 1);
    }
  }

  /** 去除字符串首尾空白字符 */
  private String trimmedStr(String str, int start, int end) {
    // 跳过开头空白字符
    while (str.charAt(start) <= 0x20) {
      start++;
    }
    // 跳过结尾空白字符
    while (str.charAt(end - 1) <= 0x20) {
      end--;
    }
    return start >= end ? "" : str.substring(start, end);
  }

}
