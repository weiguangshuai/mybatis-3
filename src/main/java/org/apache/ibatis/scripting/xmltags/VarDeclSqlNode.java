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
 * VarDeclSqlNode - 负责将 OGNL 表达式的计算结果绑定到 DynamicContext 中
 *
 * @author Frank D. Martinez [mnesarco]
 */
public class VarDeclSqlNode implements SqlNode {

  /** 变量名 */
  private final String name;
  /** OGNL 表达式 */
  private final String expression;

  /**
   * 构造方法
   *
   * @param name 变量名
   * @param exp  OGNL 表达式
   */
  public VarDeclSqlNode(String name, String exp) {
    this.name = name;
    this.expression = exp;
  }

  /**
   * 执行 OGNL 表达式并将结果绑定到 DynamicContext 中
   *
   * @param context DynamicContext
   * @return 始终返回 true
   */
  @Override
  public boolean apply(DynamicContext context) {
    // 通过 OGNL 计算表达式值
    final Object value = OgnlCache.getValue(expression, context.getBindings());
    // 将结果绑定到 DynamicContext，供后续 SqlNode 引用
    context.bind(name, value);
    return true;
  }

}
