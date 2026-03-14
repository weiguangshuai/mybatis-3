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
 * 元数据类，用于动态访问和检测Java Bean的属性信息。
 * 内部委托Reflector完成实际的反射操作，支持嵌套属性解析和泛型类型解析。
 *
 * @author Clinton Begin
 */
public class MetaClass {

  /** Reflector工厂，用于创建和缓存Reflector实例 */
  private final ReflectorFactory reflectorFactory;
  /** 目标类的Reflector，封装了该类的getter/setter及属性元数据 */
  private final Reflector reflector;

  /**
   * 私有构造函数，通过ReflectorFactory创建MetaClass实例。
   *
   * @param type 目标类的Class对象
   * @param reflectorFactory Reflector工厂实例
   */
  private MetaClass(Class<?> type, ReflectorFactory reflectorFactory) {
    this.reflectorFactory = reflectorFactory;
    this.reflector = reflectorFactory.findForClass(type);
  }

  /**
   * 创建指定类的MetaClass实例。
   *
   * @param type 目标类的Class对象
   * @param reflectorFactory Reflector工厂实例
   * @return MetaClass实例
   */
  public static MetaClass forClass(Class<?> type, ReflectorFactory reflectorFactory) {
    return new MetaClass(type, reflectorFactory);
  }

  /**
   * 获取指定属性类型的MetaClass实例，用于处理嵌套属性。
   *
   * @param name 属性名
   * @return 该属性类型的MetaClass实例
   */
  public MetaClass metaClassForProperty(String name) {
    Class<?> propType = reflector.getGetterType(name);
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   * 查找属性名，支持嵌套属性（如"user.name"）。
   *
   * @param name 属性表达式
   * @return 规范化后的属性名，不存在则返回null
   */
  public String findProperty(String name) {
    StringBuilder prop = buildProperty(name, new StringBuilder());
    return prop.length() > 0 ? prop.toString() : null;
  }

  /**
   * 查找属性名，支持是否将下划线转换为驼峰命名。
   *
   * @param name 属性表达式
   * @param useCamelCaseMapping 是否将下划线去除后按驼峰处理
   * @return 规范化后的属性名，不存在则返回null
   */
  public String findProperty(String name, boolean useCamelCaseMapping) {
    if (useCamelCaseMapping) {
      name = name.replace("_", "");
    }
    return findProperty(name);
  }

  /**
   * 获取所有可读属性名（具有getter方法的属性）。
   *
   * @return getter属性名数组
   */
  public String[] getGetterNames() {
    return reflector.getGetablePropertyNames();
  }

  /**
   * 获取所有可写属性名（具有setter方法的属性）。
   *
   * @return setter属性名数组
   */
  public String[] getSetterNames() {
    return reflector.getSetablePropertyNames();
  }

  /**
   * 获取setter方法的参数类型，支持嵌套属性解析。
   *
   * @param name 属性表达式
   * @return setter参数的类型
   */
  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaClass metaProp = metaClassForProperty(prop.getName());
      return metaProp.getSetterType(prop.getChildren());
    } else {
      return reflector.getSetterType(prop.getName());
    }
  }

  /**
   * 获取getter方法的返回类型，支持嵌套属性和集合泛型类型解析。
   *
   * @param name 属性表达式
   * @return getter返回值的类型
   */
  public Class<?> getGetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaClass metaProp = metaClassForProperty(prop);
      return metaProp.getGetterType(prop.getChildren());
    }
    // issue #506. Resolve the type inside a Collection Object
    return getGetterType(prop);
  }

  /**
   * 根据属性解析器获取该属性类型的MetaClass实例。
   *
   * @param prop 属性解析器
   * @return 属性类型的MetaClass实例
   */
  private MetaClass metaClassForProperty(PropertyTokenizer prop) {
    Class<?> propType = getGetterType(prop);
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   * 获取属性类型，若属性是集合类型则解析其泛型参数。
   *
   * @param prop 属性解析器
   * @return 属性类型
   */
  private Class<?> getGetterType(PropertyTokenizer prop) {
    Class<?> type = reflector.getGetterType(prop.getName());
    // 集合类型需要解析泛型参数，如List<String> -> String
    if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {
      Type returnType = getGenericGetterType(prop.getName());
      if (returnType instanceof ParameterizedType) {
        Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          returnType = actualTypeArguments[0];
          if (returnType instanceof Class) {
            type = (Class<?>) returnType;
          } else if (returnType instanceof ParameterizedType) {
            // 嵌套泛型如List<List<String>>，取原始类型
            type = (Class<?>) ((ParameterizedType) returnType).getRawType();
          }
        }
      }
    }
    return type;
  }

  /**
   * 获取getter的泛型返回类型。
   *
   * @param propertyName 属性名
   * @return 泛型类型，若解析失败则返回null
   */
  private Type getGenericGetterType(String propertyName) {
    try {
      Invoker invoker = reflector.getGetInvoker(propertyName);
      if (invoker instanceof MethodInvoker) {
        // 通过反射获取MethodInvoker中封装的Method对象
        Field _method = MethodInvoker.class.getDeclaredField("method");
        _method.setAccessible(true);
        Method method = (Method) _method.get(invoker);
        return TypeParameterResolver.resolveReturnType(method, reflector.getType());
      } else if (invoker instanceof GetFieldInvoker) {
        // 通过反射获取GetFieldInvoker中封装的Field对象
        Field _field = GetFieldInvoker.class.getDeclaredField("field");
        _field.setAccessible(true);
        Field field = (Field) _field.get(invoker);
        return TypeParameterResolver.resolveFieldType(field, reflector.getType());
      }
    } catch (NoSuchFieldException e) {
      // 忽略异常，返回null
    } catch (IllegalAccessException e) {
      // 忽略异常，返回null
    }
    return null;
  }

  /**
   * 检查是否存在指定的setter方法，支持嵌套属性。
   *
   * @param name 属性表达式
   * @return 是否存在setter
   */
  public boolean hasSetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      if (reflector.hasSetter(prop.getName())) {
        // 递归检查嵌套属性
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
   * 检查是否存在指定的getter方法，支持嵌套属性。
   *
   * @param name 属性表达式
   * @return 是否存在getter
   */
  public boolean hasGetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      if (reflector.hasGetter(prop.getName())) {
        // 递归检查嵌套属性
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
   * 获取属性getter的调用器。
   *
   * @param name 属性名
   * @return Invoker实例
   */
  public Invoker getGetInvoker(String name) {
    return reflector.getGetInvoker(name);
  }

  /**
   * 获取属性setter的调用器。
   *
   * @param name 属性名
   * @return Invoker实例
   */
  public Invoker getSetInvoker(String name) {
    return reflector.getSetInvoker(name);
  }

  /**
   * 递归构建属性名，将嵌套属性转换为点号分隔的形式。
   *
   * @param name 属性表达式
   * @param builder 用于拼接结果的StringBuilder
   * @return 拼接后的StringBuilder
   */
  private StringBuilder buildProperty(String name, StringBuilder builder) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      // 处理嵌套属性，递归解析子属性
      String propertyName = reflector.findPropertyName(prop.getName());
      if (propertyName != null) {
        builder.append(propertyName);
        builder.append(".");
        MetaClass metaProp = metaClassForProperty(propertyName);
        metaProp.buildProperty(prop.getChildren(), builder);
      }
    } else {
      // 处理基本属性
      String propertyName = reflector.findPropertyName(name);
      if (propertyName != null) {
        builder.append(propertyName);
      }
    }
    return builder;
  }

  /**
   * 检查类是否具有无参构造函数。
   *
   * @return 是否有无参构造函数
   */
  public boolean hasDefaultConstructor() {
    return reflector.hasDefaultConstructor();
  }

}
