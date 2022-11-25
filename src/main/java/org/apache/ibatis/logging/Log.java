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

/**
 * 日志抽象接口，屏蔽不同日志框架的差异，提供统一的日志操作方法。
 *
 * @author Clinton Begin
 */
public interface Log {

  /**
   * 判断是否启用 DEBUG 级别日志。
   */
  boolean isDebugEnabled();

  /**
   * 判断是否启用 TRACE 级别日志。
   */
  boolean isTraceEnabled();

  /**
   * 记录 ERROR 级别日志及异常信息。
   *
   * @param s 日志消息
   * @param e 异常对象
   */
  void error(String s, Throwable e);

  /**
   * 记录 ERROR 级别日志。
   *
   * @param s 日志消息
   */
  void error(String s);

  /**
   * 记录 DEBUG 级别日志。
   *
   * @param s 日志消息
   */
  void debug(String s);

  /**
   * 记录 TRACE 级别日志。
   *
   * @param s 日志消息
   */
  void trace(String s);

  /**
   * 记录 WARN 级别日志。
   *
   * @param s 日志消息
   */
  void warn(String s);

}
