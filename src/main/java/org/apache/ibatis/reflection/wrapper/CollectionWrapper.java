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
package org.apache.ibatis.reflection.wrapper;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * Collection 对象的包装器，实现 ObjectWrapper 接口。
 * 用于在 MyBatis 反射机制中处理集合类型的属性。
 *
 * @author Clinton Begin
 */
public class CollectionWrapper implements ObjectWrapper {

  /** 被包装的 Collection 对象 */
  private final Collection<Object> object;

  /**
   * 构造方法，初始化 CollectionWrapper。
   *
   * @param metaObject 元对象
   * @param object 要包装的 Collection 对象
   */
  public CollectionWrapper(MetaObject metaObject, Collection<Object> object) {
    this.object = object;
  }

  @Override
  public Object get(PropertyTokenizer prop) {
    // Collection 不支持索引访问，抛出不支持操作异常
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(PropertyTokenizer prop, Object value) {
    // Collection 不支持索引赋值，抛出不支持操作异常
    throw new UnsupportedOperationException();
  }

  @Override
  public String findProperty(String name, boolean useCamelCaseMapping) {
    // Collection 不支持属性查找，抛出不支持操作异常
    throw new UnsupportedOperationException();
  }

  @Override
  public String[] getGetterNames() {
    // Collection 不支持获取 getter 名称列表，抛出不支持操作异常
    throw new UnsupportedOperationException();
  }

  @Override
  public String[] getSetterNames() {
    // Collection 不支持获取 setter 名称列表，抛出不支持操作异常
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<?> getSetterType(String name) {
    // Collection 不支持获取 setter 类型，抛出不支持操作异常
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<?> getGetterType(String name) {
    // Collection 不支持获取 getter 类型，抛出不支持操作异常
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasSetter(String name) {
    // Collection 不支持检查 setter，抛出不支持操作异常
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasGetter(String name) {
    // Collection 不支持检查 getter，抛出不支持操作异常
    throw new UnsupportedOperationException();
  }

  @Override
  public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
    // Collection 不支持属性值实例化，抛出不支持操作异常
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isCollection() {
    // 当前对象是 Collection 包装器
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
   * @param <E> 元素类型
   */
  @Override
  public <E> void addAll(List<E> element) {
    object.addAll(element);
  }

}
