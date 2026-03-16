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
package org.apache.ibatis.binding;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * MyBatis 绑定异常，用于处理映射器（Mapper）调用过程中的各种错误，
 * 如方法未找到、参数不匹配、类型转换失败等。
 *
 * @author Clinton Begin
 */
public class BindingException extends PersistenceException {

  private static final long serialVersionUID = 4300802238789381562L;

  /**
   * 构造无详细消息的绑定异常。
   */
  public BindingException() {
    super();
  }

  /**
   * 构造带指定错误消息的绑定异常。
   *
   * @param message 错误描述信息
   */
  public BindingException(String message) {
    super(message);
  }

  /**
   * 构造带错误消息和根因异常的绑定异常。
   *
   * @param message 错误描述信息
   * @param cause   原始异常，通常为底层持久层或反射相关异常
   */
  public BindingException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 构造带根因异常的绑定异常。
   *
   * @param cause 原始异常，通常为底层持久层或反射相关异常
   */
  public BindingException(Throwable cause) {
    super(cause);
  }
}
