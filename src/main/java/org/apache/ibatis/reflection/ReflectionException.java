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
package org.apache.ibatis.reflection;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * 反射操作相关的异常，用于封装 Java 反射 API 调用时发生的错误。
 *
 * @author Clinton Begin
 */
public class ReflectionException extends PersistenceException {

  /** 序列化版本 UID，用于反序列化时的版本兼容校验 */
  private static final long serialVersionUID = 7642570221267566591L;

  /** 无参构造方法，创建一个空的反射异常 */
  public ReflectionException() {
    super();
  }

  /** 带错误信息的构造方法
   * @param message 异常错误信息 */
  public ReflectionException(String message) {
    super(message);
  }

  /** 带错误信息和根因的构造方法
   * @param message 异常错误信息
   * @param cause 根因异常 */
  public ReflectionException(String message, Throwable cause) {
    super(message, cause);
  }

  /** 带根因的构造方法
   * @param cause 根因异常 */
  public ReflectionException(Throwable cause) {
    super(cause);
  }

}
