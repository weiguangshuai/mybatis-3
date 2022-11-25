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
package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.Field;

import org.apache.ibatis.reflection.Reflector;

/**
 * 通过反射调用 getter 方法获取字段值的调用器。
 *
 * @author Clinton Begin
 */
public class GetFieldInvoker implements Invoker {
  /** 要获取值的 Java 反射字段对象 */
  private final Field field;

  /**
   * 构造方法，初始化字段调用器。
   *
   * @param field 要操作的字段对象
   */
  public GetFieldInvoker(Field field) {
    this.field = field;
  }

  /**
   * 调用字段的 getter 方法获取字段值。
   *
   * @param target 目标对象
   * @param args 方法参数（此处未使用）
   * @return 字段值
   * @throws IllegalAccessException 无法访问字段时抛出
   */
  @Override
  public Object invoke(Object target, Object[] args) throws IllegalAccessException {
    try {
      return field.get(target);
    } catch (IllegalAccessException e) {
      // 尝试设置可访问标志后重试，适用于私有字段或 JDK 内部类
      if (Reflector.canControlMemberAccessible()) {
        field.setAccessible(true);
        return field.get(target);
      } else {
        throw e;
      }
    }
  }

  /**
   * 获取字段的类型。
   *
   * @return 字段的 Class 类型
   */
  @Override
  public Class<?> getType() {
    return field.getType();
  }
}
