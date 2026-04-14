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
package org.apache.ibatis.builder;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * MyBatis 构建器模块的异常基类，用于封装 XML 映射器和注解配置解析过程中的错误。
 *
 * @author Clinton Begin
 */
public class BuilderException extends PersistenceException {

  /** 序列化版本标识符，确保反序列化兼容性 */
  private static final long serialVersionUID = -3885164021020443281L;

  /** 无参构造函数 */
  public BuilderException() {
    super();
  }

  /**
   * 带错误信息的构造函数。
   *
   * @param message 错误描述信息
   */
  public BuilderException(String message) {
    super(message);
  }

  /**
   * 带错误信息和Cause的构造函数。
   *
   * @param message 错误描述信息
   * @param cause 原始异常
   */
  public BuilderException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 带Cause的构造函数。
   *
   * @param cause 原始异常
   */
  public BuilderException(Throwable cause) {
    super(cause);
  }
}
