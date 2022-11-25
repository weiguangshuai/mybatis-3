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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * Map 对象的属性包装器，提供对 Map 类型对象的属性读写访问。
 *
 * @author Clinton Begin
 */
public class MapWrapper extends BaseWrapper {

  /** 被包装的 Map 对象 */
  private final Map<String, Object> map;

  /**
   * 构造 MapWrapper 实例。
   *
   * @param metaObject 元对象
   * @param map 要包装的 Map 对象
   */
  public MapWrapper(MetaObject metaObject, Map<String, Object> map) {
    super(metaObject);
    this.map = map;
  }

  /**
   * 获取属性值。
   * 支持嵌套属性访问，如 map[key] 或 map[key].subKey。
   *
   * @param prop 属性解析器
   * @return 属性值
   */
  @Override
  public Object get(PropertyTokenizer prop) {
    if (prop.getIndex() != null) {
      // 处理集合类型属性
      Object collection = resolveCollection(prop, map);
      return getCollectionValue(prop, collection);
    } else {
      return map.get(prop.getName());
    }
  }

  /**
   * 设置属性值。
   * 支持嵌套属性设置。
   *
   * @param prop 属性解析器
   * @param value 要设置的值
   */
  @Override
  public void set(PropertyTokenizer prop, Object value) {
    if (prop.getIndex() != null) {
      // 处理集合类型属性
      Object collection = resolveCollection(prop, map);
      setCollectionValue(prop, collection, value);
    } else {
      map.put(prop.getName(), value);
    }
  }

  /**
   * 查找属性名。
   * Map 的 key 直接返回，无需映射处理。
   *
   * @param name 属性名
   * @param useCamelCaseMapping 是否启用驼峰命名映射
   * @return 查找后的属性名
   */
  @Override
  public String findProperty(String name, boolean useCamelCaseMapping) {
    return name;
  }

  /**
   * 获取所有 getter 方法对应的属性名。
   *
   * @return 属性名数组
   */
  @Override
  public String[] getGetterNames() {
    return map.keySet().toArray(new String[0]);
  }

  /**
   * 获取所有 setter 方法对应的属性名。
   *
   * @return 属性名数组
   */
  @Override
  public String[] getSetterNames() {
    return map.keySet().toArray(new String[0]);
  }

  /**
   * 获取 setter 方法的参数类型。
   *
   * @param name 属性名
   * @return setter 参数类型，若未知则返回 Object.class
   */
  @Override
  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      // 处理嵌套属性
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return Object.class;
      } else {
        return metaValue.getSetterType(prop.getChildren());
      }
    } else {
      // 根据现有值推断类型
      if (map.get(name) != null) {
        return map.get(name).getClass();
      } else {
        return Object.class;
      }
    }
  }

  /**
   * 获取 getter 方法的返回类型。
   *
   * @param name 属性名
   * @return getter 返回类型，若未知则返回 Object.class
   */
  @Override
  public Class<?> getGetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      // 处理嵌套属性
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return Object.class;
      } else {
        return metaValue.getGetterType(prop.getChildren());
      }
    } else {
      // 根据现有值推断类型
      if (map.get(name) != null) {
        return map.get(name).getClass();
      } else {
        return Object.class;
      }
    }
  }

  /**
   * 判断是否存在 setter 方法。
   * Map 总是支持设置任意 key 的值。
   *
   * @param name 属性名
   * @return 始终返回 true
   */
  @Override
  public boolean hasSetter(String name) {
    return true;
  }

  /**
   * 判断是否存在 getter 方法。
   *
   * @param name 属性名
   * @return 是否存在 getter
   */
  @Override
  public boolean hasGetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      // 处理嵌套属性
      if (map.containsKey(prop.getIndexedName())) {
        MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
        if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
          return true;
        } else {
          return metaValue.hasGetter(prop.getChildren());
        }
      } else {
        return false;
      }
    } else {
      return map.containsKey(prop.getName());
    }
  }

  /**
   * 为指定属性创建并初始化嵌套的 MetaObject。
   * 适用于属性值为空时的延迟创建。
   *
   * @param name 属性名
   * @param prop 属性解析器
   * @param objectFactory 对象工厂
   * @return 新创建的 MetaObject
   */
  @Override
  public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
    HashMap<String, Object> map = new HashMap<>();
    set(prop, map);
    return MetaObject.forObject(map, metaObject.getObjectFactory(), metaObject.getObjectWrapperFactory(), metaObject.getReflectorFactory());
  }

  /**
   * 判断是否为集合类型。
   *
   * @return Map 不是集合，返回 false
   */
  @Override
  public boolean isCollection() {
    return false;
  }

  /**
   * 添加元素。Map 不支持此操作。
   *
   * @param element 要添加的元素
   * @throws UnsupportedOperationException 总是抛出此异常
   */
  @Override
  public void add(Object element) {
    throw new UnsupportedOperationException();
  }

  /**
   * 批量添加元素。Map 不支持此操作。
   *
   * @param element 要添加的元素列表
   * @throws UnsupportedOperationException 总是抛出此异常
   */
  @Override
  public <E> void addAll(List<E> element) {
    throw new UnsupportedOperationException();
  }

}
