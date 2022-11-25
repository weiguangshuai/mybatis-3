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

import org.apache.ibatis.reflection.ReflectionException;

/**
 * 当方法调用存在歧义（如重载方法无法确定具体调用哪一个）时使用的调用器。
 * 调用时直接抛出异常，避免运行时不确定性。
 */
public class AmbiguousMethodInvoker extends MethodInvoker {
  /** 记录方法歧义的具体原因，用于异常信息 */
  private final String exceptionMessage;

  /**
   * 构造歧义方法调用器。
   * @param method 存在歧义的方法
   * @param exceptionMessage 异常信息，描述歧义原因
   */
  public AmbiguousMethodInvoker(Method method, String exceptionMessage) {
    super(method);
    this.exceptionMessage = exceptionMessage;
  }

  /**
   * 执行方法调用，直接抛出异常表示该方法调用存在歧义无法执行。
   * @param target 目标对象
   * @param args 方法参数
   * @return 无返回值，始终抛出异常
   * @throws ReflectionException 始终抛出，表示方法解析歧义
   */
  @Override
  public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
    // 方法存在歧义，拒绝执行，抛出明确的异常信息
    throw new ReflectionException(exceptionMessage);
  }
}
