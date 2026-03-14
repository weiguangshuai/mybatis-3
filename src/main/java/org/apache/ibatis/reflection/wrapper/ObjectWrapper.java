/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.reflection.wrapper;

import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 对象属性包装器接口，用于统一访问不同类型对象的属性
 *
 * @author Clinton Begin
 */
public interface ObjectWrapper {

  /**
   * 获取属性值，支持嵌套属性访问
   * @param prop 属性标记器，包含属性名和索引信息
   * @return 属性值，若不存在则返回 null
   */
  Object get(PropertyTokenizer prop);

  /**
   * 设置属性值，支持嵌套属性访问
   * @param prop 属性标记器
   * @param value 要设置的值
   */
  void set(PropertyTokenizer prop, Object value);

  /**
   * 查找属性名，处理下划线转驼峰映射
   * @param name 属性名
   * @param useCamelCaseMapping 是否启用驼峰映射
   * @return 找到的属性名，若未找到则返回 null
   */
  String findProperty(String name, boolean useCamelCaseMapping);

  /**
   * 获取所有 getter 方法对应的属性名
   * @return 属性名数组
   */
  String[] getGetterNames();

  /**
   * 获取所有 setter 方法对应的属性名
   * @return 属性名数组
   */
  String[] getSetterNames();

  /**
   * 获取 setter 方法的参数类型
   * @param name 属性名
   * @return 参数类型，若不存在则返回 null
   */
  Class<?> getSetterType(String name);

  /**
   * 获取 getter 方法的返回类型
   * @param name 属性名
   * @return 返回类型，若不存在则返回 null
   */
  Class<?> getGetterType(String name);

  /**
   * 判断是否存在指定名称的 setter 方法
   * @param name 属性名
   * @return 是否存在 setter
   */
  boolean hasSetter(String name);

  /**
   * 判断是否存在指定名称的 getter 方法
   * @param name 属性名
   * @return 是否存在 getter
   */
  boolean hasGetter(String name);

  /**
   * 为属性创建嵌套对象的 MetaObject
   * @param name 属性名
   * @param prop 属性标记器
   * @param objectFactory 对象工厂，用于创建嵌套对象
   * @return 嵌套对象的 MetaObject
   */
  MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

  /**
   * 判断当前对象是否为集合类型
   * @return 是否为集合
   */
  boolean isCollection();

  /**
   * 添加元素到集合
   * @param element 要添加的元素
   */
  void add(Object element);

  /**
   * 批量添加元素到集合
   * @param element 要添加的元素列表
   */
  <E> void addAll(List<E> element);

}
