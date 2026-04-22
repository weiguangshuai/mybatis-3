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

/**
 * @author Clinton Begin
 */

/**
 * 动态 SQL 的 <if> SqlNode，根据条件表达式决定是否拼接内部 SQL 片段
 */
public class IfSqlNode implements SqlNode {
  /** ExpressionEvaluator，用于解析 test 条件 */
  private final ExpressionEvaluator evaluator;
  /** 条件表达式字符串 */
  private final String test;
  /** 条件成立时应用的 SqlNode */
  private final SqlNode contents;

  /**
   * 构造 IfSqlNode
   *
   * @param contents 条件成立时应用的 SqlNode
   * @param test 条件表达式
   */
  public IfSqlNode(SqlNode contents, String test) {
    this.test = test;
    this.contents = contents;
    this.evaluator = new ExpressionEvaluator();
  }

  /**
   * 评估条件表达式，若成立则应用内部 SqlNode
   *
   * @param context DynamicContext
   * @return 条件成立返回 true，否则返回 false
   */
  @Override
  public boolean apply(DynamicContext context) {
    if (evaluator.evaluateBoolean(test, context.getBindings())) {
      // 条件满足，拼接内部 SQL 片段
      contents.apply(context);
      return true;
    }
    return false;
  }

}
