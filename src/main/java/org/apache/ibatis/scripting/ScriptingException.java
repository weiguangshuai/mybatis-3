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
package org.apache.ibatis.scripting;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * ScriptingException - 在解析或执行动态 SQL 脚本时抛出
 *
 * @author Frank D. Martinez [mnesarco]
 */
public class ScriptingException extends PersistenceException {

  /** 序列化版本号 */
  private static final long serialVersionUID = 7642570221267566591L;

  /** 构造一个无详细信息的 ScriptingException */
  public ScriptingException() {
    super();
  }

  /**
   * 构造一个带指定错误信息的 ScriptingException
   *
   * @param message 异常描述信息
   */
  public ScriptingException(String message) {
    super(message);
  }

  /**
   * 构造一个带指定错误信息和底层原因的 ScriptingException
   *
   * @param message 异常描述信息
   * @param cause 导致该异常的原始异常
   */
  public ScriptingException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 构造一个由指定底层原因触发的 ScriptingException
   *
   * @param cause 导致该异常的原始异常
   */
  public ScriptingException(Throwable cause) {
    super(cause);
  }

}
