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
package org.apache.ibatis.builder;

import java.util.List;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

/**
 * 表示静态 SQL 语句的 SqlSource 实现。
 *
 * @author Clinton Begin
 */
public class StaticSqlSource implements SqlSource {

  /** SQL 语句文本 */
  private final String sql;
  /** 参数映射列表 */
  private final List<ParameterMapping> parameterMappings;
  /** MyBatis 配置对象 */
  private final Configuration configuration;

  /**
   * 构造静态 SQL 源（无参数映射）。
   *
   * @param configuration MyBatis 配置对象
   * @param sql SQL 语句文本
   */
  public StaticSqlSource(Configuration configuration, String sql) {
    this(configuration, sql, null);
  }

  /**
   * 构造静态 SQL 源。
   *
   * @param configuration MyBatis 配置对象
   * @param sql SQL 语句文本
   * @param parameterMappings 参数映射列表，可为 null
   */
  public StaticSqlSource(Configuration configuration, String sql, List<ParameterMapping> parameterMappings) {
    this.sql = sql;
    this.parameterMappings = parameterMappings;
    this.configuration = configuration;
  }

  /**
   * 获取绑定后的 SQL 对象。
   *
   * @param parameterObject 参数对象
   * @return 绑定好的 SQL 对象
   */
  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    return new BoundSql(configuration, sql, parameterMappings, parameterObject);
  }

}
