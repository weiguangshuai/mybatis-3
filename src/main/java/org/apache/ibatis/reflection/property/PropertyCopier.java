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

import java.lang.reflect.Field;

import org.apache.ibatis.reflection.Reflector;

/**
 * 属性复制工具类，用于将源对象的属性值复制到目标对象。
 * 支持继承链上的所有属性，包括私有字段。
 *
 * @author Clinton Begin
 */
public final class PropertyCopier {

  private PropertyCopier() {
    // Prevent Instantiation of Static Class
  }

  /**
   * 将源对象的属性值复制到目标对象。
   * <p>
   * 该方法会遍历类的整条继承链（包括父类），将源对象中所有字段的值
   * 复制到目标对象的对应字段中。如果字段访问受限，会尝试设置可访问后复制。
   *
   * @param type            目标对象的类型，用于确定要复制的属性范围
   * @param sourceBean      源对象
   * @param destinationBean 目标对象
   */
  public static void copyBeanProperties(Class<?> type, Object sourceBean, Object destinationBean) {
    // 从当前类开始，向上遍历整条继承链
    Class<?> parent = type;
    while (parent != null) {
      // 获取当前层级的所有声明字段
      final Field[] fields = parent.getDeclaredFields();
      for (Field field : fields) {
        try {
          // 尝试直接设置字段值
          try {
            field.set(destinationBean, field.get(sourceBean));
          } catch (IllegalAccessException e) {
            // 私有字段或受保护字段，尝试设置可访问后再复制
            if (Reflector.canControlMemberAccessible()) {
              field.setAccessible(true);
              field.set(destinationBean, field.get(sourceBean));
            } else {
              throw e;
            }
          }
        } catch (Exception e) {
          // 忽略失败情况，如 final 字段无法复制
        }
      }
      // 继续处理父类
      parent = parent.getSuperclass();
    }
  }

}
