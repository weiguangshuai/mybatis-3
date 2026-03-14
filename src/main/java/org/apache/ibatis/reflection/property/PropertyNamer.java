/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * 将方法名转换为属性名，或判断方法是否为属性访问器（getter/setter/is方法）。
 *
 * @author Clinton Begin
 */
public final class PropertyNamer {

  private PropertyNamer() {
    // Prevent Instantiation of Static Class
  }

  /**
   * 将方法名转换为对应的属性名。
   *
   * @param name 方法名（如 getUserName、setAge、isActive）
   * @return 属性名（如 userName、age、active）
   * @throws ReflectionException 如果方法名不以 is/get/set 开头
   */
  public static String methodToProperty(String name) {
    // 处理 isXxx 布尔类型 getter
    if (name.startsWith("is")) {
      name = name.substring(2);
    } else if (name.startsWith("get") || name.startsWith("set")) {
      // 处理 getXxx 和 setXxx 方法
      name = name.substring(3);
    } else {
      throw new ReflectionException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
    }

    // 首字母小写：单字符或第二字符非大写时需要转换
    if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
      name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
    }

    return name;
  }

  /**
   * 判断方法名是否为属性访问器（getter、setter 或 is 方法）。
   *
   * @param name 方法名
   * @return 如果是属性访问器返回 true
   */
  public static boolean isProperty(String name) {
    return name.startsWith("get") || name.startsWith("set") || name.startsWith("is");
  }

  /**
   * 判断方法名是否为 getter 方法（getXxx 或 isXxx）。
   *
   * @param name 方法名
   * @return 如果是 getter 方法返回 true
   */
  public static boolean isGetter(String name) {
    return name.startsWith("get") || name.startsWith("is");
  }

  /**
   * 判断方法名是否为 setter 方法（setXxx）。
   *
   * @param name 方法名
   * @return 如果是 setter 方法返回 true
   */
  public static boolean isSetter(String name) {
    return name.startsWith("set");
  }

}
