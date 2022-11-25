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
package org.apache.ibatis.reflection.wrapper;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * Collection 类型的对象包装器。
 * 用于包装 Collection 对象，支持集合的添加操作。
 *
 * @author Clinton Begin
 */
public class CollectionWrapper implements ObjectWrapper {

  /** 被包装的 Collection 对象 */
  private final Collection<Object> object;

  /**
   * 构造函数。
   *
   * @param metaObject 元对象
   * @param object 要包装的 Collection 对象
   */
  public CollectionWrapper(MetaObject metaObject, Collection<Object> object) {
    this.object = object;
  }

  /**
   * 不支持获取集合元素操作。
   * Collection 包装器不支持按索引访问元素。
   */
  @Override
  public Object get(PropertyTokenizer prop) {
    throw new UnsupportedOperationException();
  }

  /**
   * 不支持设置集合元素操作。
   * Collection 包装器不支持按索引设置元素。
   */
  @Override
  public void set(PropertyTokenizer prop, Object value) {
    throw new UnsupportedOperationException();
  }

  /**
   * 不支持查找属性操作。
   * Collection 没有可遍历的属性名。
   */
  @Override
  public String findProperty(String name, boolean useCamelCaseMapping) {
    throw new UnsupportedOperationException();
  }

  /**
   * 不支持获取 getter 方法名列表。
   * Collection 没有可用的 getter 方法。
   */
  @Override
  public String[] getGetterNames() {
    throw new UnsupportedOperationException();
  }

  /**
   * 不支持获取 setter 方法名列表。
   * Collection 没有可用的 setter 方法。
   */
  @Override
  public String[] getSetterNames() {
    throw new UnsupportedOperationException();
  }

  /**
   * 不支持获取 setter 类型。
   *
   * @param name 属性名
   * @return 不返回值，始终抛出异常
   */
  @Override
  public Class<?> getSetterType(String name) {
    throw new UnsupportedOperationException();
  }

  /**
   * 不支持获取 getter 类型。
   *
   * @param name 属性名
   * @return 不返回值，始终抛出异常
   */
  @Override
  public Class<?> getGetterType(String name) {
    throw new UnsupportedOperationException();
  }

  /**
   * 不支持判断是否有 setter。
   *
   * @param name 属性名
   * @return 不返回值，始终抛出异常
   */
  @Override
  public boolean hasSetter(String name) {
    throw new UnsupportedOperationException();
  }

  /**
   * 不支持判断是否有 getter。
   *
   * @param name 属性名
   * @return 不返回值，始终抛出异常
   */
  @Override
  public boolean hasGetter(String name) {
    throw new UnsupportedOperationException();
  }

  /**
   * 不支持实例化属性值操作。
   *
   * @param name 属性名
   * @param prop 属性标记器
   * @param objectFactory 对象工厂
   * @return 不返回值，始终抛出异常
   */
  @Override
  public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
    throw new UnsupportedOperationException();
  }

  /**
   * 判断是否为 Collection 类型。
   *
   * @return 始终返回 true，表示这是 Collection 包装器
   */
  @Override
  public boolean isCollection() {
    return true;
  }

  /**
   * 向集合中添加单个元素。
   *
   * @param element 要添加的元素
   */
  @Override
  public void add(Object element) {
    object.add(element);
  }

  /**
   * 向集合中添加多个元素。
   *
   * @param element 要添加的元素列表
   */
  @Override
  public <E> void addAll(List<E> element) {
    object.addAll(element);
  }

}
