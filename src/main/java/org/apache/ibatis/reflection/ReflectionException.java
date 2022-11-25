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
package org.apache.ibatis.reflection;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * MyBatis 反射模块专用的异常类型，用于封装反射操作过程中发生的错误。
 *
 * @author Clinton Begin
 */
public class ReflectionException extends PersistenceException {

  /** 序列化版本标识符，确保反序列化兼容性 */
  private static final long serialVersionUID = 7642570221267566591L;

  /**
   * 默认构造方法，创建一个无详细信息的反射异常。
   */
  public ReflectionException() {
    super();
  }

  /**
   * 根据指定错误信息创建反射异常。
   *
   * @param message 异常描述信息
   */
  public ReflectionException(String message) {
    super(message);
  }

  /**
   * 根据错误信息和原始异常创建反射异常。
   *
   * @param message 异常描述信息
   * @param cause 导致当前异常的原始异常
   */
  public ReflectionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 根据原始异常创建反射异常。
   *
   * @param cause 导致当前异常的原始异常
   */
  public ReflectionException(Throwable cause) {
    super(cause);
  }

}
