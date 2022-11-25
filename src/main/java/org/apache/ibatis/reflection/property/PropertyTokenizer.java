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
package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * 属性分词器，用于分解带索引或嵌套的属性表达式。
 * 例如 "person[0].address.name" 会分解为 person[0] 和 address.name 两部分。
 *
 * @author Clinton Begin
 */
public class PropertyTokenizer implements Iterator<PropertyTokenizer> {
  /** 属性名，不包含索引部分 */
  private String name;
  /** 带索引的完整属性名，如 person[0] */
  private final String indexedName;
  /** 索引值，如数组或列表的下标 */
  private String index;
  /** 嵌套的子属性表达式 */
  private final String children;

  /**
   * 构造分词器，解析属性表达式。
   *
   * @param fullname 完整的属性表达式，如 "person[0].address.name"
   */
  public PropertyTokenizer(String fullname) {
    // 查找第一个点号，分离主属性和嵌套属性
    int delim = fullname.indexOf('.');
    if (delim > -1) {
      name = fullname.substring(0, delim);
      children = fullname.substring(delim + 1);
    } else {
      name = fullname;
      children = null;
    }
    indexedName = name;
    // 查找方括号，提取索引值
    delim = name.indexOf('[');
    if (delim > -1) {
      index = name.substring(delim + 1, name.length() - 1);
      name = name.substring(0, delim);
    }
  }

  /**
   * 获取属性名（不含索引部分）。
   *
   * @return 属性名
   */
  public String getName() {
    return name;
  }

  /**
   * 获取索引值（数组或列表下标）。
   *
   * @return 索引值，如无索引则返回 null
   */
  public String getIndex() {
    return index;
  }

  /**
   * 获取带索引的完整属性名。
   *
   * @return 带索引的属性名，如 person[0]
   */
  public String getIndexedName() {
    return indexedName;
  }

  /**
   * 获取嵌套的子属性表达式。
   *
   * @return 子属性表达式，如嵌套属性则返回 null
   */
  public String getChildren() {
    return children;
  }

  /**
   * 判断是否有嵌套属性需要继续解析。
   *
   * @return 存在嵌套属性返回 true
   */
  @Override
  public boolean hasNext() {
    return children != null;
  }

  /**
   * 获取下一个分词器，用于解析嵌套属性。
   *
   * @return 嵌套属性的分词器
   */
  @Override
  public PropertyTokenizer next() {
    return new PropertyTokenizer(children);
  }

  /**
   * 不支持删除操作。
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
  }
}
