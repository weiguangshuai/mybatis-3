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
 * SqlNode - 负责将自身内容解析并追加到 DynamicContext 中
 */
public interface SqlNode {

  /**
   * 将当前 SqlNode 的 SQL 内容应用到 DynamicContext 中
   *
   * @param context DynamicContext，用于收集解析后的 SQL 片段和参数
   * @return 是否成功应用了该 SqlNode（部分 SqlNode 可能因条件不满足而返回 false）
   */
  boolean apply(DynamicContext context);
}
