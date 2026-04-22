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

import java.util.Collections;
import java.util.List;

import org.apache.ibatis.session.Configuration;

/**
 * 代表 SQL 中的 SET SqlNode，用于动态生成 UPDATE 语句的 SET 子句。
 * 继承自 TrimSqlNode，自动去除末尾多余的逗号。
 *
 * @author Clinton Begin
 */
public class SetSqlNode extends TrimSqlNode {

  /** 用于匹配和去除多余逗号的单元素 List */
  private static final List<String> COMMA = Collections.singletonList(",");

  /**
   * 构造一个 SET SqlNode。
   *
   * @param configuration Configuration
   * @param contents 子 SqlNode
   */
  public SetSqlNode(Configuration configuration,SqlNode contents) {
    // 调用父类构造，前缀为 "SET"，前后缀均去除多余逗号
    super(configuration, contents, "SET", COMMA, null, COMMA);
  }

}
