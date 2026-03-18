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
package org.apache.ibatis.builder;

/**
 * 当 MyBatis 映射配置中的元素不完整或解析失败时抛出的异常。
 *
 * @author Eduardo Macarron
 */
public class IncompleteElementException extends BuilderException {
  /** 序列化版本UID */
  private static final long serialVersionUID = -3697292286890900315L;

  /** 无参构造方法 */
  public IncompleteElementException() {
    super();
  }

  /**
   * 带详细消息和原始异常的构造方法。
   *
   * @param message 异常描述信息
   * @param cause 导致此异常的原始异常
   */
  public IncompleteElementException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 带详细消息的构造方法。
   *
   * @param message 异常描述信息
   */
  public IncompleteElementException(String message) {
    super(message);
  }

  /**
   * 带原始异常的构造方法。
   *
   * @param cause 导致此异常的原始异常
   */
  public IncompleteElementException(Throwable cause) {
    super(cause);
  }

}
