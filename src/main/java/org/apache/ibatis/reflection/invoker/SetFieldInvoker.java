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
 * 调用器实现类，用于通过反射设置对象的字段值。
 *
 * @author Clinton Begin
 */
public class SetFieldInvoker implements Invoker {
  /** 要设置的字段对象 */
  private final Field field;

  /**
   * 构造方法，保存待操作的字段引用。
   *
   * @param field 要设置的字段
   */
  public SetFieldInvoker(Field field) {
    this.field = field;
  }

  /**
   * 执行字段值设置操作。
   *
   * @param target 目标对象
   * @param args 参数数组，第一个元素为要设置的值
   * @return 返回 null
   * @throws IllegalAccessException 如果无法访问字段
   */
  @Override
  public Object invoke(Object target, Object[] args) throws IllegalAccessException {
    try {
      // 直接尝试设置字段值
      field.set(target, args[0]);
    } catch (IllegalAccessException e) {
      // 若访问被拒绝，检查是否可控制成员访问权限
      if (Reflector.canControlMemberAccessible()) {
        // 强制设置可访问后重试
        field.setAccessible(true);
        field.set(target, args[0]);
      } else {
        // 无法处理时抛出异常
        throw e;
      }
    }
    return null;
  }

  /**
   * 获取字段的类型。
   *
   * @return 字段的 Java 类型
   */
  @Override
  public Class<?> getType() {
    return field.getType();
  }
}
