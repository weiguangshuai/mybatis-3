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
package org.apache.ibatis.scripting.defaults;

import java.util.HashMap;

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.session.Configuration;

/**
 * RawSqlSource，在启动时即完成映射计算，因此比 {@link DynamicSqlSource} 更快。
 *
 * @since 3.2.0
 * @author Eduardo Macarron
 */
public class RawSqlSource implements SqlSource {

  /** 解析后得到的静态 SqlSource，用于实际生成 BoundSql。 */
  private final SqlSource sqlSource;

  /**
   * 通过 SqlNode 构造 RawSqlSource。
   *
   * @param configuration Configuration
   * @param rootSqlNode   根 SqlNode
   * @param parameterType 参数类型
   */
  public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
    this(configuration, getSql(configuration, rootSqlNode), parameterType);
  }

  /**
   * 通过原始 SQL 字符串构造 RawSqlSource，并在构造时完成 SQL 解析。
   *
   * @param configuration Configuration
   * @param sql           原始 SQL 字符串
   * @param parameterType 参数类型
   */
  public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType) {
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    // 参数类型为空时，默认使用 Object.class
    Class<?> clazz = parameterType == null ? Object.class : parameterType;
    sqlSource = sqlSourceParser.parse(sql, clazz, new HashMap<>());
  }

  /**
   * 从 SqlNode 中提取最终 SQL 字符串。
   *
   * @param configuration Configuration
   * @param rootSqlNode   根 SqlNode
   * @return 解析后的 SQL 字符串
   */
  private static String getSql(Configuration configuration, SqlNode rootSqlNode) {
    DynamicContext context = new DynamicContext(configuration, null);
    rootSqlNode.apply(context);
    return context.getSql();
  }

  /**
   * 获取 BoundSql 对象。
   *
   * @param parameterObject 实际参数对象
   * @return 包含最终 SQL 与参数的 BoundSql
   */
  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    return sqlSource.getBoundSql(parameterObject);
  }

}
