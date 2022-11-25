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

import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 对象包装器的基类，提供集合类型（Map、List、数组）的属性解析与赋值通用实现。
 *
 * @author Clinton Begin
 */
public abstract class BaseWrapper implements ObjectWrapper {

  /** 空参数数组，用于避免重复创建 */
  protected static final Object[] NO_ARGUMENTS = new Object[0];
  /** 当前 MetaObject 上下文，用于获取属性值 */
  protected final MetaObject metaObject;

  /**
   * 构造方法，初始化 MetaObject 上下文。
   *
   * @param metaObject 元对象
   */
  protected BaseWrapper(MetaObject metaObject) {
    this.metaObject = metaObject;
  }

  /**
   * 解析集合属性，返回集合对象本身或通过属性路径获取嵌套对象。
   *
   * @param prop   属性标记器
   * @param object 目标对象
   * @return 解析后的集合对象
   */
  protected Object resolveCollection(PropertyTokenizer prop, Object object) {
    // 空名称表示直接返回当前对象
    if ("".equals(prop.getName())) {
      return object;
    } else {
      // 否则通过属性路径获取嵌套属性
      return metaObject.getValue(prop.getName());
    }
  }

  /**
   * 获取集合中的元素值，支持 Map、List 及各种基本类型数组。
   *
   * @param prop      属性标记器
   * @param collection 集合对象
   * @return 元素值
   */
  protected Object getCollectionValue(PropertyTokenizer prop, Object collection) {
    if (collection instanceof Map) {
      // Map 类型使用键获取值
      return ((Map) collection).get(prop.getIndex());
    } else {
      // 其他集合类型使用索引访问
      int i = Integer.parseInt(prop.getIndex());
      if (collection instanceof List) {
        return ((List) collection).get(i);
      } else if (collection instanceof Object[]) {
        return ((Object[]) collection)[i];
      } else if (collection instanceof char[]) {
        return ((char[]) collection)[i];
      } else if (collection instanceof boolean[]) {
        return ((boolean[]) collection)[i];
      } else if (collection instanceof byte[]) {
        return ((byte[]) collection)[i];
      } else if (collection instanceof double[]) {
        return ((double[]) collection)[i];
      } else if (collection instanceof float[]) {
        return ((float[]) collection)[i];
      } else if (collection instanceof int[]) {
        return ((int[]) collection)[i];
      } else if (collection instanceof long[]) {
        return ((long[]) collection)[i];
      } else if (collection instanceof short[]) {
        return ((short[]) collection)[i];
      } else {
        throw new ReflectionException("The '" + prop.getName() + "' property of " + collection + " is not a List or Array.");
      }
    }
  }

  /**
   * 设置集合中的元素值，支持 Map、List 及各种基本类型数组。
   *
   * @param prop      属性标记器
   * @param collection 集合对象
   * @param value     要设置的值
   */
  protected void setCollectionValue(PropertyTokenizer prop, Object collection, Object value) {
    if (collection instanceof Map) {
      // Map 类型使用键值对设置
      ((Map) collection).put(prop.getIndex(), value);
    } else {
      // 其他集合类型使用索引设置
      int i = Integer.parseInt(prop.getIndex());
      if (collection instanceof List) {
        ((List) collection).set(i, value);
      } else if (collection instanceof Object[]) {
        ((Object[]) collection)[i] = value;
      } else if (collection instanceof char[]) {
        ((char[]) collection)[i] = (Character) value;
      } else if (collection instanceof boolean[]) {
        ((boolean[]) collection)[i] = (Boolean) value;
      } else if (collection instanceof byte[]) {
        ((byte[]) collection)[i] = (Byte) value;
      } else if (collection instanceof double[]) {
        ((double[]) collection)[i] = (Double) value;
      } else if (collection instanceof float[]) {
        ((float[]) collection)[i] = (Float) value;
      } else if (collection instanceof int[]) {
        ((int[]) collection)[i] = (Integer) value;
      } else if (collection instanceof long[]) {
        ((long[]) collection)[i] = (Long) value;
      } else if (collection instanceof short[]) {
        ((short[]) collection)[i] = (Short) value;
      } else {
        throw new ReflectionException("The '" + prop.getName() + "' property of " + collection + " is not a List or Array.");
      }
    }
  }

}
