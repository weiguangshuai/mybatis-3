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

import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.session.Configuration;

/**
 * 处理动态 SQL 中 <where> 标签，自动添加 WHERE 前缀并去除多余的 AND/OR。
 *
 * @author Clinton Begin
 */
public class WhereSqlNode extends TrimSqlNode {

  /**
   * 需要被去除的前缀 List，包括 AND/OR 及其后跟随的各种空白字符。
   */
  private static List<String> prefixList = Arrays.asList("AND ","OR ","AND\n", "OR\n", "AND\r", "OR\r", "AND\t", "OR\t");

  /**
   * 构造 WhereSqlNode 实例，使用 WHERE 作为前缀并指定需要覆盖的前缀 List。
   *
   * @param configuration MyBatis Configuration
   * @param contents 内部的 SqlNode
   */
  public WhereSqlNode(Configuration configuration, SqlNode contents) {
    super(configuration, contents, "WHERE", prefixList, null, null);
  }

}
