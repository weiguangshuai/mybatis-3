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
package org.apache.ibatis.logging;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * MyBatis 日志模块异常基类。
 * 用于封装日志操作过程中发生的各种错误。
 *
 * @author Clinton Begin
 */
public class LogException extends PersistenceException {

  /** 序列化版本UID，确保反序列化兼容性 */
  private static final long serialVersionUID = 1022924004852350942L;

  /** 无参构造函数 */
  public LogException() {
    super();
  }

  /**
   * 带错误消息的构造函数。
   *
   * @param message 错误描述信息
   */
  public LogException(String message) {
    super(message);
  }

  /**
   * 带错误消息和根因异常的构造函数。
   *
   * @param message 错误描述信息
   * @param cause 原始异常
   */
  public LogException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 带根因异常的构造函数。
   *
   * @param cause 原始异常
   */
  public LogException(Throwable cause) {
    super(cause);
  }

}
