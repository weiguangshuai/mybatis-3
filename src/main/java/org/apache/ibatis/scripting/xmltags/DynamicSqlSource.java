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

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

/**
 * DynamicSqlSource - 负责解析并生成包含动态标签（如 if、foreach）的 SQL 语句
 *
 * @author Clinton Begin
 */
public class DynamicSqlSource implements SqlSource {

  /** MyBatis Configuration */
  private final Configuration configuration;
  /** 动态 SqlNode 树的根 SqlNode */
  private final SqlNode rootSqlNode;

  /**
   * 构造方法
   *
   * @param configuration MyBatis Configuration
   * @param rootSqlNode 动态 SqlNode 树的根 SqlNode
   */
  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    this.configuration = configuration;
    this.rootSqlNode = rootSqlNode;
  }

  /**
   * 解析动态 SQL 并生成可执行的 BoundSql 对象
   *
   * @param parameterObject 参数对象
   * @return 包含最终 SQL 和参数的 BoundSql
   */
  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    DynamicContext context = new DynamicContext(configuration, parameterObject);
    // 执行动态 SqlNode，拼接原始 SQL 文本
    rootSqlNode.apply(context);
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    // 若参数为空，默认使用 Object 类型
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    // 将 DynamicContext 中解析出的额外参数绑定到 BoundSql
    context.getBindings().forEach(boundSql::setAdditionalParameter);
    return boundSql;
  }

}
