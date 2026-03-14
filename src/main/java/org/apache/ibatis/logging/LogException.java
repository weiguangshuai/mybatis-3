/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.logging;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * 日志模块异常，用于处理日志操作中发生的错误。
 *
 * @author Clinton Begin
 */
public class LogException extends PersistenceException {

  private static final long serialVersionUID = 1022924004852350942L;

  /**
   * 构造无详细消息的 LogException。
   */
  public LogException() {
    super();
  }

  /**
   * 构造带指定详细消息的 LogException。
   *
   * @param message 异常详细消息
   */
  public LogException(String message) {
    super(message);
  }

  /**
   * 构造带详细消息和根因异常的 LogException。
   *
   * @param message 异常详细消息
   * @param cause 根因异常
   */
  public LogException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 构造带根因异常的 LogException。
   *
   * @param cause 根因异常
   */
  public LogException(Throwable cause) {
    super(cause);
  }

}
