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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.ibatis.reflection.Reflector;

/**
 * 方法调用器，通过 Java 反射机制执行目标对象的指定方法。
 *
 * @author Clinton Begin
 */
public class MethodInvoker implements Invoker {

  /** 方法返回值类型或 setter 参数类型 */
  private final Class<?> type;
  /** 要执行的反射方法对象 */
  private final Method method;

  /**
   * 创建方法调用器。
   *
   * @param method 要执行的方法
   */
  public MethodInvoker(Method method) {
    this.method = method;

    // setter 方法有一个参数，getter 方法无参数
    if (method.getParameterTypes().length == 1) {
      type = method.getParameterTypes()[0];
    } else {
      type = method.getReturnType();
    }
  }

  /**
   * 执行目标对象上的方法调用。
   *
   * @param target 目标对象
   * @param args 方法参数
   * @return 方法返回值
   * @throws IllegalAccessException 无法访问方法时抛出
   * @throws InvocationTargetException 方法执行异常时抛出
   */
  @Override
  public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
    try {
      return method.invoke(target, args);
    } catch (IllegalAccessException e) {
      // 私有方法或受保护方法可能需要强制访问
      if (Reflector.canControlMemberAccessible()) {
        method.setAccessible(true);
        return method.invoke(target, args);
      } else {
        throw e;
      }
    }
  }

  /**
   * 获取方法参数类型或返回值类型。
   *
   * @return 类型信息
   */
  @Override
  public Class<?> getType() {
    return type;
  }
}
