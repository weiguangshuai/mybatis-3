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
package org.apache.ibatis.scripting.xmltags;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.BuilderException;

/**
 * ExpressionEvaluator - 负责将 OGNL 表达式在参数对象上求值，支持布尔判断和 Iterable 解析
 *
 * @author Clinton Begin
 */
public class ExpressionEvaluator {

  /**
   * 将 OGNL 表达式在参数对象上求值，并转换为布尔结果
   *
   * @param expression 布尔表达式
   * @param parameterObject 参数对象
   * @return 求值结果：Boolean 直接返回；Number 非零为 true；非 null 为 true
   */
  public boolean evaluateBoolean(String expression, Object parameterObject) {
    Object value = OgnlCache.getValue(expression, parameterObject);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof Number) {
      // 数值非零视为 true
      return new BigDecimal(String.valueOf(value)).compareTo(BigDecimal.ZERO) != 0;
    }
    return value != null;
  }

  /**
   * @deprecated Since 3.5.9, use the {@link #evaluateIterable(String, Object, boolean)}.
   */
  @Deprecated
  public Iterable<?> evaluateIterable(String expression, Object parameterObject) {
    return evaluateIterable(expression, parameterObject, false);
  }

  /**
   * 将 OGNL 表达式求值结果转换为 Iterable，支持集合、数组和 Map
   *
   * @param expression 表达式
   * @param parameterObject 参数对象
   * @param nullable 是否允许返回 null
   * @return Iterable 对象；若值为 null 且 nullable 为 false 则抛出异常
   * @since 3.5.9
   */
  public Iterable<?> evaluateIterable(String expression, Object parameterObject, boolean nullable) {
    Object value = OgnlCache.getValue(expression, parameterObject);
    if (value == null) {
      if (nullable) {
        return null;
      } else {
        // 不允许 null 时直接抛出异常，避免后续空指针
        throw new BuilderException("The expression '" + expression + "' evaluated to a null value.");
      }
    }
    if (value instanceof Iterable) {
      return (Iterable<?>) value;
    }
    if (value.getClass().isArray()) {
      // the array may be primitive, so Arrays.asList() may throw
      // a ClassCastException (issue 209).  Do the work manually
      // Curse primitives! :) (JGB)
      int size = Array.getLength(value);
      List<Object> answer = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        Object o = Array.get(value, i);
        answer.add(o);
      }
      return answer;
    }
    if (value instanceof Map) {
      // Map 通过 entrySet 转为可迭代形式
      return ((Map) value).entrySet();
    }
    throw new BuilderException("Error evaluating expression '" + expression + "'.  Return value (" + value + ") was not iterable.");
  }

}
