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

import java.lang.reflect.Field;

/**
 * 属性复制工具类，用于在对象之间复制同名的属性值。
 *
 * @author Clinton Begin
 */
public final class PropertyCopier {

  private PropertyCopier() {
    // Prevent Instantiation of Static Class
  }

  /**
   * 复制源对象的属性值到目标对象，包括父类的属性。
   *
   * @param type 对象类型，用于遍历类层次结构
   * @param sourceBean 源对象
   * @param destinationBean 目标对象
   */
  public static void copyBeanProperties(Class<?> type, Object sourceBean, Object destinationBean) {
    Class<?> parent = type;
    // 遍历类的继承层次，包括父类
    while (parent != null) {
      final Field[] fields = parent.getDeclaredFields();
      for(Field field : fields) {
        try {
          field.setAccessible(true);
          field.set(destinationBean, field.get(sourceBean));
        } catch (Exception e) {
          // 忽略异常：final 字段等无法复制的字段会被静默跳过
        }
      }
      // 继续处理父类
      parent = parent.getSuperclass();
    }
  }

}
