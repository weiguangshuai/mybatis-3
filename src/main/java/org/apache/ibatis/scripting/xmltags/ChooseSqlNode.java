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

import java.util.List;

/**
 * @author Clinton Begin
 */
/**
 * Choose 动态 SqlNode，按顺序匹配首个满足条件的分支，否则执行默认分支
 */
public class ChooseSqlNode implements SqlNode {
  /**
   * 默认分支 SqlNode，当所有条件分支都不满足时执行
   */
  private final SqlNode defaultSqlNode;
  /**
   * 条件分支 SqlNode List，按顺序依次判断
   */
  private final List<SqlNode> ifSqlNodes;

  /**
   * 构造方法
   *
   * @param ifSqlNodes 条件分支 SqlNode List
   * @param defaultSqlNode 默认分支 SqlNode
   */
  public ChooseSqlNode(List<SqlNode> ifSqlNodes, SqlNode defaultSqlNode) {
    this.ifSqlNodes = ifSqlNodes;
    this.defaultSqlNode = defaultSqlNode;
  }

  /**
   * 按顺序应用首个满足条件的分支，无匹配则应用默认分支
   *
   * @param context DynamicContext
   * @return 是否有任意分支被应用
   */
  @Override
  public boolean apply(DynamicContext context) {
    // 遍历条件分支，返回首个匹配成功的 SqlNode 结果
    for (SqlNode sqlNode : ifSqlNodes) {
      if (sqlNode.apply(context)) {
        return true;
      }
    }
    // 无匹配时执行默认分支
    if (defaultSqlNode != null) {
      defaultSqlNode.apply(context);
      return true;
    }
    return false;
  }
}
