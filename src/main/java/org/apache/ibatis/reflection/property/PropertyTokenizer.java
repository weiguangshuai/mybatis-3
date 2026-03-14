/**
 *    Copyright 2009-2017 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * 属性分词器，将嵌套属性名（如 "user.name"、"items[0].name"）分解为可迭代的组成部分。
 *
 * @author Clinton Begin
 */
public class PropertyTokenizer implements Iterator<PropertyTokenizer> {
  /** 当前层级的属性名 */
  private String name;
  /** 带索引的完整名称，如 "items[0]" */
  private final String indexedName;
  /** 方括号中的索引值，如数组/列表索引 "0" */
  private String index;
  /** 剩余的嵌套属性部分，用于迭代解析 */
  private final String children;

  /**
   * 构造分词器，解析嵌套属性名。
   *
   * @param fullname 完整的属性名，如 "user.name" 或 "items[0].id"
   */
  public PropertyTokenizer(String fullname) {
    // 查找第一个 "." 分隔符，用于分离当前属性和嵌套属性
    int delim = fullname.indexOf('.');
    if (delim > -1) {
      name = fullname.substring(0, delim);
      children = fullname.substring(delim + 1);
    } else {
      name = fullname;
      children = null;
    }
    indexedName = name;
    // 查找 "[" 分隔符，用于分离属性名和索引
    delim = name.indexOf('[');
    if (delim > -1) {
      index = name.substring(delim + 1, name.length() - 1);
      name = name.substring(0, delim);
    }
  }

  /**
   * 获取当前层级的属性名。
   *
   * @return 属性名，如 "user" 或 "items"
   */
  public String getName() {
    return name;
  }

  /**
   * 获取方括号中的索引值。
   *
   * @return 索引值，如 "0"，无索引时返回 null
   */
  public String getIndex() {
    return index;
  }

  /**
   * 获取带索引的完整名称。
   *
   * @return 带索引的名称，如 "items[0]"
   */
  public String getIndexedName() {
    return indexedName;
  }

  /**
   * 获取剩余的嵌套属性部分。
   *
   * @return 嵌套属性字符串，无嵌套时返回 null
   */
  public String getChildren() {
    return children;
  }

  @Override
  public boolean hasNext() {
    // 存在子属性时才可继续迭代
    return children != null;
  }

  @Override
  public PropertyTokenizer next() {
    // 创建新的分词器解析剩余的嵌套属性
    return new PropertyTokenizer(children);
  }

  @Override
  public void remove() {
    // 属性解析不支持删除操作
    throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
  }
}
