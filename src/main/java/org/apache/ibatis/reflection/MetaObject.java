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
package org.apache.ibatis.reflection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.apache.ibatis.reflection.wrapper.CollectionWrapper;
import org.apache.ibatis.reflection.wrapper.MapWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * 对象元数据的封装类，提供对象属性的统一访问和修改机制。
 *
 * @author Clinton Begin
 */
public class MetaObject {

  /** 原始被包装的对象 */
  private final Object originalObject;
  /** 对象包装器，负责属性读写 */
  private final ObjectWrapper objectWrapper;
  /** 对象工厂，用于创建新对象实例 */
  private final ObjectFactory objectFactory;
  /** 对象包装器工厂，创建适合的包装器 */
  private final ObjectWrapperFactory objectWrapperFactory;
  /** 反射器工厂，创建类的元数据 */
  private final ReflectorFactory reflectorFactory;

  /**
   * 构造方法，根据对象类型创建相应的包装器。
   *
   * @param object                要包装的对象
   * @param objectFactory         对象工厂
   * @param objectWrapperFactory  对象包装器工厂
   * @param reflectorFactory      反射器工厂
   */
  private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    this.originalObject = object;
    this.objectFactory = objectFactory;
    this.objectWrapperFactory = objectWrapperFactory;
    this.reflectorFactory = reflectorFactory;

    // 根据对象类型选择合适的包装器
    if (object instanceof ObjectWrapper) {
      this.objectWrapper = (ObjectWrapper) object;
    } else if (objectWrapperFactory.hasWrapperFor(object)) {
      this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
    } else if (object instanceof Map) {
      this.objectWrapper = new MapWrapper(this, (Map) object);
    } else if (object instanceof Collection) {
      this.objectWrapper = new CollectionWrapper(this, (Collection) object);
    } else {
      this.objectWrapper = new BeanWrapper(this, object);
    }
  }

  /**
   * 为指定对象创建 MetaObject 包装。
   *
   * @param object                要包装的对象，null 时返回空对象
   * @param objectFactory         对象工厂
   * @param objectWrapperFactory  对象包装器工厂
   * @param reflectorFactory      反射器工厂
   * @return 对象的 MetaObject 包装，null 对象返回 NULL_META_OBJECT
   */
  public static MetaObject forObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    if (object == null) {
      return SystemMetaObject.NULL_META_OBJECT;
    } else {
      return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
    }
  }

  /**
   * 获取对象工厂。
   */
  public ObjectFactory getObjectFactory() {
    return objectFactory;
  }

  /**
   * 获取对象包装器工厂。
   */
  public ObjectWrapperFactory getObjectWrapperFactory() {
    return objectWrapperFactory;
  }

  /**
   * 获取反射器工厂。
   */
  public ReflectorFactory getReflectorFactory() {
    return reflectorFactory;
  }

  /**
   * 获取原始对象。
   */
  public Object getOriginalObject() {
    return originalObject;
  }

  /**
   * 查找指定属性名，支持下划线转驼峰映射。
   *
   * @param propName             属性名
   * @param useCamelCaseMapping 是否启用驼峰映射
   * @return 实际属性名，不存在则返回 null
   */
  public String findProperty(String propName, boolean useCamelCaseMapping) {
    return objectWrapper.findProperty(propName, useCamelCaseMapping);
  }

  /**
   * 获取所有 getter 方法对应的属性名。
   */
  public String[] getGetterNames() {
    return objectWrapper.getGetterNames();
  }

  /**
   * 获取所有 setter 方法对应的属性名。
   */
  public String[] getSetterNames() {
    return objectWrapper.getSetterNames();
  }

  /**
   * 获取 setter 方法的参数类型。
   *
   * @param name 属性名
   */
  public Class<?> getSetterType(String name) {
    return objectWrapper.getSetterType(name);
  }

  /**
   * 获取 getter 方法的返回类型。
   *
   * @param name 属性名
   */
  public Class<?> getGetterType(String name) {
    return objectWrapper.getGetterType(name);
  }

  /**
   * 判断指定属性是否有 setter 方法。
   *
   * @param name 属性名
   */
  public boolean hasSetter(String name) {
    return objectWrapper.hasSetter(name);
  }

  /**
   * 判断指定属性是否有 getter 方法。
   *
   * @param name 属性名
   */
  public boolean hasGetter(String name) {
    return objectWrapper.hasGetter(name);
  }

  /**
   * 获取嵌套属性值，支持索引和嵌套访问。
   *
   * @param name 属性名，支持如 "user.name"、"list[0].id" 等形式
   * @return 属性值，不存在则返回 null
   */
  public Object getValue(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 有子属性时递归获取
    if (prop.hasNext()) {
      MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return null;
      } else {
        return metaValue.getValue(prop.getChildren());
      }
    } else {
      return objectWrapper.get(prop);
    }
  }

  /**
   * 设置嵌套属性值，支持索引和嵌套访问。
   *
   * @param name  属性名，支持如 "user.name"、"list[0].id" 等形式
   * @param value 要设置的值
   */
  public void setValue(String name, Object value) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 有子属性时递归设置
    if (prop.hasNext()) {
      MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        if (value == null) {
          // 值为 null 时不实例化子路径
          return;
        } else {
          metaValue = objectWrapper.instantiatePropertyValue(name, prop, objectFactory);
        }
      }
      metaValue.setValue(prop.getChildren(), value);
    } else {
      objectWrapper.set(prop, value);
    }
  }

  /**
   * 获取指定属性的 MetaObject 包装。
   *
   * @param name 属性名
   * @return 属性的 MetaObject 包装
   */
  public MetaObject metaObjectForProperty(String name) {
    Object value = getValue(name);
    return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
  }

  /**
   * 获取对象包装器。
   */
  public ObjectWrapper getObjectWrapper() {
    return objectWrapper;
  }

  /**
   * 判断对象是否为集合类型。
   */
  public boolean isCollection() {
    return objectWrapper.isCollection();
  }

  /**
   * 添加元素到集合。
   *
   * @param element 要添加的元素
   */
  public void add(Object element) {
    objectWrapper.add(element);
  }

  /**
   * 批量添加元素到集合。
   *
   * @param list 要添加的元素列表
   */
  public <E> void addAll(List<E> list) {
    objectWrapper.addAll(list);
  }

}
