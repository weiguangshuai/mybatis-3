/*
 *    Copyright 2009-2026 the original author or authors.
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
package org.apache.ibatis.parsing;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * 解析异常，用于处理 XML、SQL 等配置文件的解析错误
 *
 * @author Clinton Begin
 */
public class ParsingException extends PersistenceException {
  /** 序列化版本号 */
  private static final long serialVersionUID = -176685891441325943L;

  public ParsingException() {
    super();
  }

  /**
   * 构造带错误信息的解析异常
   * @param message 错误信息
   */
  public ParsingException(String message) {
    super(message);
  }

  /**
   * 构造带错误信息和根因的解析异常
   * @param message 错误信息
   * @param cause 根因异常
   */
  public ParsingException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 构造带根因的解析异常
   * @param cause 根因异常
   */
  public ParsingException(Throwable cause) {
    super(cause);
  }
}
