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

/**
 * 属性访问器接口，用于通过反射调用 getter 或 setter 方法。
 *
 * @author Clinton Begin
 */
public interface Invoker {
  /**
   * 调用目标对象的属性访问方法。
   *
   * @param target 目标对象
   * @param args   方法参数，getter 时为空数组，setter 时为单个值
   * @return getter 调用返回属性值，setter 调用返回 null
   * @throws IllegalAccessException    反射访问权限不足
   * @throws InvocationTargetException 目标方法本身抛出异常
   */
  Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException;

  /**
   * 获取该访问器所操作的属性类型。
   *
   * @return 属性的 Java 类型
   */
  Class<?> getType();
}
