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
package org.apache.ibatis.binding;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * MyBatis 绑定异常，用于处理 Mapper 接口与 XML 配置之间的映射错误
 *
 * @author Clinton Begin
 */
public class BindingException extends PersistenceException {

  /** 序列化版本号，确保反序列化兼容性 */
  private static final long serialVersionUID = 4300802238789381562L;

  public BindingException() {
    super();
  }

  /**
   * 构造带错误信息的绑定异常
   *
   * @param message 异常描述信息
   */
  public BindingException(String message) {
    super(message);
  }

  /**
   * 构造带错误信息和根因的绑定异常
   *
   * @param message 异常描述信息
   * @param cause 原始异常
   */
  public BindingException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 构造带根因的绑定异常
   *
   * @param cause 原始异常
   */
  public BindingException(Throwable cause) {
    super(cause);
  }
}
