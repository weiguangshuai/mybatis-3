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
package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * 用于通过反射获取字段值的 Invoker 实现类。
 *
 * @author Clinton Begin
 */
public class GetFieldInvoker implements Invoker {
  /** 要操作的字段对象 */
  private final Field field;

  /**
   * 构造方法，指定要操作的字段。
   * @param field 要获取值的字段
   */
  public GetFieldInvoker(Field field) {
    this.field = field;
  }

  /**
   * 获取目标对象的字段值。
   * @param target 目标对象
   * @param args 方法参数（此处未使用）
   * @return 字段的当前值
   */
  @Override
  public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
    // 通过反射获取指定对象的字段值
    return field.get(target);
  }

  /**
   * 获取字段的类型。
   * @return 字段的 Java 类型
   */
  @Override
  public Class<?> getType() {
    return field.getType();
  }
}
