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

import java.util.List;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * Bean 对象的属性包装器，提供对普通 JavaBean 的属性读写能力。
 *
 * @author Clinton Begin
 */
public class BeanWrapper extends BaseWrapper {

  /** 被包装的 Bean 实例 */
  private final Object object;
  /** Bean 的元信息类，包含属性 getter/setter 的反射调用器 */
  private final MetaClass metaClass;

  /**
   * 构造 BeanWrapper 实例。
   *
   * @param metaObject 父级 MetaObject
   * @param object 要包装的 Bean 实例
   */
  public BeanWrapper(MetaObject metaObject, Object object) {
    super(metaObject);
    this.object = object;
    this.metaClass = MetaClass.forClass(object.getClass(), metaObject.getReflectorFactory());
  }

  @Override
  public Object get(PropertyTokenizer prop) {
    // 索引属性（如 list[0]）按集合处理
    if (prop.getIndex() != null) {
      Object collection = resolveCollection(prop, object);
      return getCollectionValue(prop, collection);
    } else {
      // 普通 Bean 属性直接获取
      return getBeanProperty(prop, object);
    }
  }

  @Override
  public void set(PropertyTokenizer prop, Object value) {
    // 索引属性按集合处理
    if (prop.getIndex() != null) {
      Object collection = resolveCollection(prop, object);
      setCollectionValue(prop, collection, value);
    } else {
      // 普通 Bean 属性直接设置
      setBeanProperty(prop, object, value);
    }
  }

  /**
   * 查找指定名称的属性。
   *
   * @param name 属性名称
   * @param useCamelCaseMapping 是否启用驼峰映射
   * @return 找到的属性名，不存在则返回 null
   */
  @Override
  public String findProperty(String name, boolean useCamelCaseMapping) {
    return metaClass.findProperty(name, useCamelCaseMapping);
  }

  /**
   * 获取所有可读取的属性名称。
   *
   * @return getter 方法对应的属性名数组
   */
  @Override
  public String[] getGetterNames() {
    return metaClass.getGetterNames();
  }

  /**
   * 获取所有可写入的属性名称。
   *
   * @return setter 方法对应的属性名数组
   */
  @Override
  public String[] getSetterNames() {
    return metaClass.getSetterNames();
  }

  /**
   * 获取指定属性的 setter 方法参数类型。
   *
   * @param name 属性名称，支持嵌套属性
   * @return setter 方法的参数类型
   */
  @Override
  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 嵌套属性：递归获取子属性的 setter 类型
    if (prop.hasNext()) {
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return metaClass.getSetterType(name);
      } else {
        return metaValue.getSetterType(prop.getChildren());
      }
    } else {
      return metaClass.getSetterType(name);
    }
  }

  /**
   * 获取指定属性的 getter 方法返回类型。
   *
   * @param name 属性名称，支持嵌套属性
   * @return getter 方法的返回类型
   */
  @Override
  public Class<?> getGetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 嵌套属性：递归获取子属性的 getter 类型
    if (prop.hasNext()) {
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return metaClass.getGetterType(name);
      } else {
        return metaValue.getGetterType(prop.getChildren());
      }
    } else {
      return metaClass.getGetterType(name);
    }
  }

  /**
   * 检查是否存在指定名称的 setter 方法。
   *
   * @param name 属性名称，支持嵌套属性
   * @return 是否存在 setter
   */
  @Override
  public boolean hasSetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 嵌套属性需逐层检查
    if (prop.hasNext()) {
      if (metaClass.hasSetter(prop.getIndexedName())) {
        MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
        if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
          return metaClass.hasSetter(name);
        } else {
          return metaValue.hasSetter(prop.getChildren());
        }
      } else {
        return false;
      }
    } else {
      return metaClass.hasSetter(name);
    }
  }

  /**
   * 检查是否存在指定名称的 getter 方法。
   *
   * @param name 属性名称，支持嵌套属性
   * @return 是否存在 getter
   */
  @Override
  public boolean hasGetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 嵌套属性需逐层检查
    if (prop.hasNext()) {
      if (metaClass.hasGetter(prop.getIndexedName())) {
        MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
        if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
          return metaClass.hasGetter(name);
        } else {
          return metaValue.hasGetter(prop.getChildren());
        }
      } else {
        return false;
      }
    } else {
      return metaClass.hasGetter(name);
    }
  }

  /**
   * 为指定属性实例化一个对象，用于延迟加载嵌套属性。
   *
   * @param name 属性名称
   * @param prop 属性解析器
   * @param objectFactory 对象工厂
   * @return 新创建对象的 MetaObject
   */
  @Override
  public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
    MetaObject metaValue;
    Class<?> type = getSetterType(prop.getName());
    try {
      // 通过 ObjectFactory 创建新实例
      Object newObject = objectFactory.create(type);
      metaValue = MetaObject.forObject(newObject, metaObject.getObjectFactory(), metaObject.getObjectWrapperFactory(), metaObject.getReflectorFactory());
      // 将新实例设置到属性中
      set(prop, newObject);
    } catch (Exception e) {
      throw new ReflectionException("Cannot set value of property '" + name + "' because '" + name + "' is null and cannot be instantiated on instance of " + type.getName() + ". Cause:" + e.toString(), e);
    }
    return metaValue;
  }

  /**
   * 获取 Bean 属性的值，通过 getter 方法反射调用。
   */
  private Object getBeanProperty(PropertyTokenizer prop, Object object) {
    try {
      // 获取属性对应的 getter 调用器
      Invoker method = metaClass.getGetInvoker(prop.getName());
      try {
        return method.invoke(object, NO_ARGUMENTS);
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable t) {
      throw new ReflectionException("Could not get property '" + prop.getName() + "' from " + object.getClass() + ".  Cause: " + t.toString(), t);
    }
  }

  /**
   * 设置 Bean 属性的值，通过 setter 方法反射调用。
   */
  private void setBeanProperty(PropertyTokenizer prop, Object object, Object value) {
    try {
      // 获取属性对应的 setter 调用器
      Invoker method = metaClass.getSetInvoker(prop.getName());
      Object[] params = {value};
      try {
        method.invoke(object, params);
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    } catch (Throwable t) {
      throw new ReflectionException("Could not set property '" + prop.getName() + "' of '" + object.getClass() + "' with value '" + value + "' Cause: " + t.toString(), t);
    }
  }

  /**
   * BeanWrapper 不是集合类型。
   *
   * @return 始终返回 false
   */
  @Override
  public boolean isCollection() {
    return false;
  }

  /**
   * 不支持向 Bean 添加元素操作。
   *
   * @param element 要添加的元素
   * @throws UnsupportedOperationException 总是抛出此异常
   */
  @Override
  public void add(Object element) {
    throw new UnsupportedOperationException();
  }

  /**
   * 不支持向 Bean 批量添加元素操作。
   *
   * @param list 要添加的元素列表
   * @throws UnsupportedOperationException 总是抛出此异常
   */
  @Override
  public <E> void addAll(List<E> list) {
    throw new UnsupportedOperationException();
  }

}
