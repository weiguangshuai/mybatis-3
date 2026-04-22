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
 * MixedSqlNode - 将多个子 SqlNode 组合成一个整体执行
 *
 * @author Clinton Begin
 */
public class MixedSqlNode implements SqlNode {
  /**
   * 子 SqlNode List
   */
  private final List<SqlNode> contents;

  /**
   * 构造方法
   *
   * @param contents 子 SqlNode List
   */
  public MixedSqlNode(List<SqlNode> contents) {
    this.contents = contents;
  }

  /**
   * 依次应用所有子 SqlNode 到 DynamicContext 中
   *
   * @param context DynamicContext
   * @return 始终返回 true
   */
  @Override
  public boolean apply(DynamicContext context) {
    // 遍历所有子 SqlNode，逐个应用到 DynamicContext 中
    contents.forEach(node -> node.apply(context));
    return true;
  }
}
