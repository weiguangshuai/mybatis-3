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
 * 用于设置对象字段值的调用器实现
 *
 * @author Clinton Begin
 */
public class SetFieldInvoker implements Invoker {
  /** 要设置的 Java 反射字段对象 */
  private final Field field;

  /**
   * 创建字段设置调用器
   *
   * @param field 要操作的字段对象
   */
  public SetFieldInvoker(Field field) {
    this.field = field;
  }

  /**
   * 执行字段赋值操作
   *
   * @param target 目标对象
   * @param args 参数数组，第一个元素为要设置的值
   * @return 返回 null
   * @throws IllegalAccessException 无访问权限时抛出
   * @throws InvocationTargetException 字段 setter 抛出异常时封装抛出
   */
  @Override
  public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
    // 通过反射设置目标对象的字段值
    field.set(target, args[0]);
    return null;
  }

  /**
   * 获取字段类型
   *
   * @return 字段的 Java 类型
   */
  @Override
  public Class<?> getType() {
    return field.getType();
  }
}
