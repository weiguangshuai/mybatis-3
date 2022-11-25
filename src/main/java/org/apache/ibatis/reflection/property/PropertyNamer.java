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
package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * 属性名工具类，用于将方法名转换为对应的属性名，以及判断方法是否为 getter/setter。
 *
 * @author Clinton Begin
 */
public final class PropertyNamer {

  private PropertyNamer() {
    // Prevent Instantiation of Static Class
  }

  /**
   * 将方法名转换为属性名。
   * 支持 isXxx（布尔类型 getter）、getXxx、setXxx 格式的方法名。
   *
   * @param name 方法名
   * @return 转换后的属性名
   */
  public static String methodToProperty(String name) {
    // 处理 is 前缀（布尔类型 getter）
    if (name.startsWith("is")) {
      name = name.substring(2);
    } else if (name.startsWith("get") || name.startsWith("set")) {
      name = name.substring(3);
    } else {
      // 非法的属性方法名格式
      throw new ReflectionException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
    }

    // 处理首字母小写：单字符或第二字符为小写时需要转换
    if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
      name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
    }

    return name;
  }

  /**
   * 判断方法名是否为属性对应的方法（getter 或 setter）。
   *
   * @param name 方法名
   * @return 如果是属性方法返回 true，否则返回 false
   */
  public static boolean isProperty(String name) {
    return isGetter(name) || isSetter(name);
  }

  /**
   * 判断方法名是否为 getter 方法。
   * 支持 getXxx（长度大于3）和 isXxx（长度大于2，用于布尔类型）格式。
   *
   * @param name 方法名
   * @return 如果是 getter 方法返回 true，否则返回 false
   */
  public static boolean isGetter(String name) {
    return (name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2);
  }

  /**
   * 判断方法名是否为 setter 方法。
   * 支持 setXxx 格式（长度大于3）。
   *
   * @param name 方法名
   * @return 如果是 setter 方法返回 true，否则返回 false
   */
  public static boolean isSetter(String name) {
    return name.startsWith("set") && name.length() > 3;
  }

}
