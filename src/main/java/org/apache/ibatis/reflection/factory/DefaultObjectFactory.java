/**
 *    Copyright 2009-2018 the original author or authors.
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
package org.apache.ibatis.reflection.factory;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * 默认对象工厂实现，负责创建 MyBatis 运行时所需的对象实例。
 *
 * @author Clinton Begin
 */
public class DefaultObjectFactory implements ObjectFactory, Serializable {

  /** 序列化版本 UID，用于反序列化时版本兼容性校验 */
  private static final long serialVersionUID = -8855120656740914948L;

  /**
   * 使用无参构造函数创建对象实例。
   *
   * @param type 要创建的对象类型
   * @return 创建的对象实例
   */
  @Override
  public <T> T create(Class<T> type) {
    return create(type, null, null);
  }

  /**
   * 根据指定的构造函数参数类型和参数值创建对象实例。
   *
   * @param type 要创建的对象类型
   * @param constructorArgTypes 构造函数参数类型列表
   * @param constructorArgs 构造函数参数值列表
   * @return 创建的对象实例
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    // 将接口类型解析为具体的实现类
    Class<?> classToCreate = resolveInterface(type);
    return (T) instantiateClass(classToCreate, constructorArgTypes, constructorArgs);
  }

  /**
   * 设置对象工厂的配置属性。
   * 默认实现不支持配置属性，子类可重写此方法以支持自定义配置。
   *
   * @param properties 配置属性
   */
  @Override
  public void setProperties(Properties properties) {
    // 默认实现不使用任何配置属性
  }

  /**
   * 实例化指定类型的对象，通过反射调用构造函数。
   *
   * @param type 要实例化的类
   * @param constructorArgTypes 构造函数参数类型
   * @param constructorArgs 构造函数参数值
   * @return 创建的对象实例
   */
  private  <T> T instantiateClass(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    try {
      Constructor<T> constructor;
      // 无参构造函数：直接获取并调用
      if (constructorArgTypes == null || constructorArgs == null) {
        constructor = type.getDeclaredConstructor();
        // 允许访问私有构造函数
        if (!constructor.isAccessible()) {
          constructor.setAccessible(true);
        }
        return constructor.newInstance();
      }
      // 有参构造函数：根据参数类型查找匹配的构造函数
      constructor = type.getDeclaredConstructor(constructorArgTypes.toArray(new Class[constructorArgTypes.size()]));
      if (!constructor.isAccessible()) {
        constructor.setAccessible(true);
      }
      return constructor.newInstance(constructorArgs.toArray(new Object[constructorArgs.size()]));
    } catch (Exception e) {
      // 构建错误信息，用于异常诊断
      StringBuilder argTypes = new StringBuilder();
      if (constructorArgTypes != null && !constructorArgTypes.isEmpty()) {
        for (Class<?> argType : constructorArgTypes) {
          argTypes.append(argType.getSimpleName());
          argTypes.append(",");
        }
        argTypes.deleteCharAt(argTypes.length() - 1); // 移除末尾逗号
      }
      StringBuilder argValues = new StringBuilder();
      if (constructorArgs != null && !constructorArgs.isEmpty()) {
        for (Object argValue : constructorArgs) {
          argValues.append(String.valueOf(argValue));
          argValues.append(",");
        }
        argValues.deleteCharAt(argValues.length() - 1); // 移除末尾逗号
      }
      throw new ReflectionException("Error instantiating " + type + " with invalid types (" + argTypes + ") or values (" + argValues + "). Cause: " + e, e);
    }
  }

  /**
   * 将接口类型解析为具体的实现类。
   * MyBatis 运行时需要创建具体类而非接口，此方法将常见的集合接口映射到默认实现类。
   *
   * @param type 接口或抽象类类型
   * @return 实际要创建的具体类
   */
  protected Class<?> resolveInterface(Class<?> type) {
    Class<?> classToCreate;
    // 映射集合接口到具体实现类
    if (type == List.class || type == Collection.class || type == Iterable.class) {
      classToCreate = ArrayList.class;
    } else if (type == Map.class) {
      classToCreate = HashMap.class;
    } else if (type == SortedSet.class) { // issue #510 Collections Support
      classToCreate = TreeSet.class;
    } else if (type == Set.class) {
      classToCreate = HashSet.class;
    } else {
      classToCreate = type;
    }
    return classToCreate;
  }

  /**
   * 判断指定类型是否为集合类型。
   *
   * @param type 要检查的类型
   * @return 如果类型是 Collection 的实现类或子类则返回 true
   */
  @Override
  public <T> boolean isCollection(Class<T> type) {
    return Collection.class.isAssignableFrom(type);
  }

}
