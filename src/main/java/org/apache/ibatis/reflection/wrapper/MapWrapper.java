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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * Map 对象的属性包装器，支持通过属性访问方式操作 Map 中的数据。
 *
 * @author Clinton Begin
 */
public class MapWrapper extends BaseWrapper {

  /** 底层封装的 Map 对象 */
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

  @Override
  public Object get(PropertyTokenizer prop) {
    // 支持索引访问（如 list[0]）
    if (prop.getIndex() != null) {
      Object collection = resolveCollection(prop, map);
      return getCollectionValue(prop, collection);
    } else {
      // 普通属性直接通过 key 获取
      return map.get(prop.getName());
    }
  }

  @Override
  public void set(PropertyTokenizer prop, Object value) {
    // 支持索引访问（如 list[0] = value）
    if (prop.getIndex() != null) {
      Object collection = resolveCollection(prop, map);
      setCollectionValue(prop, collection, value);
    } else {
      // 普通属性直接 put 到 Map 中
      map.put(prop.getName(), value);
    }
  }

  /**
   * 查找属性名。Map 的 key 直接返回，无需转换。
   *
   * @param name 属性名
   * @param useCamelCaseMapping 是否启用驼峰转换
   * @return 属性名
   */
  @Override
  public String findProperty(String name, boolean useCamelCaseMapping) {
    return name;
  }

  /**
   * 获取所有 getter 方法名，对应 Map 的所有 key。
   *
   * @return getter 方法名数组
   */
  @Override
  public String[] getGetterNames() {
    return map.keySet().toArray(new String[map.keySet().size()]);
  }

  /**
   * 获取所有 setter 方法名，对应 Map 的所有 key。
   *
   * @return setter 方法名数组
   */
  @Override
  public String[] getSetterNames() {
    return map.keySet().toArray(new String[map.keySet().size()]);
  }

  /**
   * 获取 setter 方法的参数类型。
   *
   * @param name 属性名
   * @return setter 参数类型
   */
  @Override
  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 支持嵌套属性访问
    if (prop.hasNext()) {
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return Object.class;
      } else {
        return metaValue.getSetterType(prop.getChildren());
      }
    } else {
      // 根据 Map 中已有值的类型确定类型
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
   * @return getter 返回类型
   */
  @Override
  public Class<?> getGetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 支持嵌套属性访问
    if (prop.hasNext()) {
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return Object.class;
      } else {
        return metaValue.getGetterType(prop.getChildren());
      }
    } else {
      // 根据 Map 中已有值的类型确定类型
      if (map.get(name) != null) {
        return map.get(name).getClass();
      } else {
        return Object.class;
      }
    }
  }

  /**
   * Map 支持任意属性的 setter，因此始终返回 true。
   *
   * @param name 属性名
   * @return 始终返回 true
   */
  @Override
  public boolean hasSetter(String name) {
    return true;
  }

  /**
   * 判断是否存在指定属性的 getter 方法。
   *
   * @param name 属性名
   * @return 是否存在 getter
   */
  @Override
  public boolean hasGetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 支持嵌套属性访问
    if (prop.hasNext()) {
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
      // 检查 Map 中是否包含该 key
      return map.containsKey(prop.getName());
    }
  }

  /**
   * 为指定属性创建并初始化属性值。
   * 当访问嵌套属性时，如果中间属性不存在，则创建一个新的 HashMap。
   *
   * @param name 属性名
   * @param prop 属性解析器
   * @param objectFactory 对象工厂
   * @return 新创建的属性的 MetaObject
   */
  @Override
  public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
    HashMap<String, Object> map = new HashMap<String, Object>();
    set(prop, map);
    return MetaObject.forObject(map, metaObject.getObjectFactory(), metaObject.getObjectWrapperFactory(), metaObject.getReflectorFactory());
  }

  /**
   * Map 不是集合类型。
   *
   * @return 始终返回 false
   */
  @Override
  public boolean isCollection() {
    return false;
  }

  /**
   * Map 不支持 add 操作。
   *
   * @param element 要添加的元素
   * @throws UnsupportedOperationException 始终抛出
   */
  @Override
  public void add(Object element) {
    throw new UnsupportedOperationException();
  }

  /**
   * Map 不支持 addAll 操作。
   *
   * @param element 要添加的元素列表
   * @throws UnsupportedOperationException 始终抛出
   */
  @Override
  public <E> void addAll(List<E> element) {
    throw new UnsupportedOperationException();
  }

}
