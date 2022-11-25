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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * 元数据类，用于动态访问和操作 Java 类的属性信息。
 *
 * @author Clinton Begin
 */
public class MetaClass {

  /** 用于创建 Reflector 实例的工厂 */
  private final ReflectorFactory reflectorFactory;
  /** 封装类的元数据信息，包含 getter/setter 方法等 */
  private final Reflector reflector;

  /**
   * 私有构造方法，通过 ReflectorFactory 创建 MetaClass 实例。
   *
   * @param type 要处理的类类型
   * @param reflectorFactory 用于创建 Reflector 的工厂
   */
  private MetaClass(Class<?> type, ReflectorFactory reflectorFactory) {
    this.reflectorFactory = reflectorFactory;
    this.reflector = reflectorFactory.findForClass(type);
  }

  /**
   * 创建指定类的 MetaClass 实例。
   *
   * @param type 要处理的类类型
   * @param reflectorFactory 用于创建 Reflector 的工厂
   * @return MetaClass 实例
   */
  public static MetaClass forClass(Class<?> type, ReflectorFactory reflectorFactory) {
    return new MetaClass(type, reflectorFactory);
  }

  /**
   * 获取指定属性类型的 MetaClass 实例，用于处理嵌套属性。
   *
   * @param name 属性名
   * @return 属性类型的 MetaClass 实例
   */
  public MetaClass metaClassForProperty(String name) {
    Class<?> propType = reflector.getGetterType(name);
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   * 查找指定属性名，支持嵌套属性（如 user.name）。
   *
   * @param name 属性名
   * @return 规范化后的属性名，不存在则返回 null
   */
  public String findProperty(String name) {
    StringBuilder prop = buildProperty(name, new StringBuilder());
    return prop.length() > 0 ? prop.toString() : null;
  }

  /**
   * 查找属性名，支持是否将下划线转换为驼峰命名。
   *
   * @param name 属性名
   * @param useCamelCaseMapping 是否将下划线移除后进行驼峰匹配
   * @return 规范化后的属性名，不存在则返回 null
   */
  public String findProperty(String name, boolean useCamelCaseMapping) {
    // 将下划线移除，支持 user_name 到 userName 的映射
    if (useCamelCaseMapping) {
      name = name.replace("_", "");
    }
    return findProperty(name);
  }

  /**
   * 获取所有可读属性名（即有 getter 方法的属性）。
   *
   * @return getter 方法对应的属性名数组
   */
  public String[] getGetterNames() {
    return reflector.getGetablePropertyNames();
  }

  /**
   * 获取所有可写属性名（即有 setter 方法的属性）。
   *
   * @return setter 方法对应的属性名数组
   */
  public String[] getSetterNames() {
    return reflector.getSetablePropertyNames();
  }

  /**
   * 获取指定属性 setter 方法的参数类型，支持嵌套属性。
   *
   * @param name 属性名
   * @return setter 方法的参数类型
   */
  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      // 嵌套属性，递归获取子属性的 setter 类型
      MetaClass metaProp = metaClassForProperty(prop.getName());
      return metaProp.getSetterType(prop.getChildren());
    } else {
      return reflector.getSetterType(prop.getName());
    }
  }

  /**
   * 获取指定属性 getter 方法的返回类型，支持嵌套属性和集合泛型。
   *
   * @param name 属性名
   * @return getter 方法的返回类型
   */
  public Class<?> getGetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      // 嵌套属性，递归获取子属性的 getter 类型
      MetaClass metaProp = metaClassForProperty(prop);
      return metaProp.getGetterType(prop.getChildren());
    }
    // issue #506. Resolve the type inside a Collection Object
    // 处理集合泛型类型，如 List<String> 返回 String 类型
    return getGetterType(prop);
  }

  /**
   * 根据属性标记器获取属性类型的 MetaClass 实例。
   *
   * @param prop 属性标记器
   * @return 属性类型的 MetaClass 实例
   */
  private MetaClass metaClassForProperty(PropertyTokenizer prop) {
    Class<?> propType = getGetterType(prop);
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   * 获取属性的 getter 类型，处理集合泛型。
   *
   * @param prop 属性标记器
   * @return getter 方法的返回类型
   */
  private Class<?> getGetterType(PropertyTokenizer prop) {
    Class<?> type = reflector.getGetterType(prop.getName());
    // 处理带泛型的集合类型，如 List<String>、Map<String, Object>
    if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {
      Type returnType = getGenericGetterType(prop.getName());
      if (returnType instanceof ParameterizedType) {
        Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          returnType = actualTypeArguments[0];
          // 泛型可能是 Class 或另一个 ParameterizedType
          if (returnType instanceof Class) {
            type = (Class<?>) returnType;
          } else if (returnType instanceof ParameterizedType) {
            type = (Class<?>) ((ParameterizedType) returnType).getRawType();
          }
        }
      }
    }
    return type;
  }

  /**
   * 通过反射获取属性的泛型类型。
   *
   * @param propertyName 属性名
   * @return 泛型类型，解析失败返回 null
   */
  private Type getGenericGetterType(String propertyName) {
    try {
      Invoker invoker = reflector.getGetInvoker(propertyName);
      if (invoker instanceof MethodInvoker) {
        // 通过 MethodInvoker 获取方法的泛型返回类型
        Field declaredMethod = MethodInvoker.class.getDeclaredField("method");
        declaredMethod.setAccessible(true);
        Method method = (Method) declaredMethod.get(invoker);
        return TypeParameterResolver.resolveReturnType(method, reflector.getType());
      } else if (invoker instanceof GetFieldInvoker) {
        // 通过 GetFieldInvoker 获取字段的泛型类型
        Field declaredField = GetFieldInvoker.class.getDeclaredField("field");
        declaredField.setAccessible(true);
        Field field = (Field) declaredField.get(invoker);
        return TypeParameterResolver.resolveFieldType(field, reflector.getType());
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      // 忽略异常，返回 null
    }
    return null;
  }

  /**
   * 判断是否存在指定属性的 setter 方法，支持嵌套属性。
   *
   * @param name 属性名
   * @return 存在返回 true，否则返回 false
   */
  public boolean hasSetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      // 嵌套属性，递归检查子属性
      if (reflector.hasSetter(prop.getName())) {
        MetaClass metaProp = metaClassForProperty(prop.getName());
        return metaProp.hasSetter(prop.getChildren());
      } else {
        return false;
      }
    } else {
      return reflector.hasSetter(prop.getName());
    }
  }

  /**
   * 判断是否存在指定属性的 getter 方法，支持嵌套属性。
   *
   * @param name 属性名
   * @return 存在返回 true，否则返回 false
   */
  public boolean hasGetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      // 嵌套属性，递归检查子属性
      if (reflector.hasGetter(prop.getName())) {
        MetaClass metaProp = metaClassForProperty(prop);
        return metaProp.hasGetter(prop.getChildren());
      } else {
        return false;
      }
    } else {
      return reflector.hasGetter(prop.getName());
    }
  }

  /**
   * 获取属性 getter 方法的调用器。
   *
   * @param name 属性名
   * @return getter 方法的调用器
   */
  public Invoker getGetInvoker(String name) {
    return reflector.getGetInvoker(name);
  }

  /**
   * 获取属性 setter 方法的调用器。
   *
   * @param name 属性名
   * @return setter 方法的调用器
   */
  public Invoker getSetInvoker(String name) {
    return reflector.getSetInvoker(name);
  }

  /**
   * 递归构建属性名，将嵌套属性连接成完整路径。
   *
   * @param name 属性名
   * @param builder 用于拼接属性名的 StringBuilder
   * @return 包含完整属性路径的 StringBuilder
   */
  private StringBuilder buildProperty(String name, StringBuilder builder) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      // 处理嵌套属性，递归构建
      String propertyName = reflector.findPropertyName(prop.getName());
      if (propertyName != null) {
        builder.append(propertyName);
        builder.append(".");
        MetaClass metaProp = metaClassForProperty(propertyName);
        metaProp.buildProperty(prop.getChildren(), builder);
      }
    } else {
      // 叶子属性，直接追加
      String propertyName = reflector.findPropertyName(name);
      if (propertyName != null) {
        builder.append(propertyName);
      }
    }
    return builder;
  }

  /**
   * 判断类是否具有无参构造方法。
   *
   * @return 具有无参构造方法返回 true，否则返回 false
   */
  public boolean hasDefaultConstructor() {
    return reflector.hasDefaultConstructor();
  }

}
