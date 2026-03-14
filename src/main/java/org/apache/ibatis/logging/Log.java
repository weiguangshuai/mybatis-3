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

/**
 * 日志抽象接口，定义统一的日志操作方法。
 *
 * @author Clinton Begin
 */
public interface Log {

  /**
   * 判断是否启用 debug 级别日志。
   *
   * @return 是否启用 debug 日志
   */
  boolean isDebugEnabled();

  /**
   * 判断是否启用 trace 级别日志。
   *
   * @return 是否启用 trace 日志
   */
  boolean isTraceEnabled();

  /**
   * 输出错误日志及异常堆栈信息。
   *
   * @param s 日志消息
   * @param e 异常对象
   */
  void error(String s, Throwable e);

  /**
   * 输出错误日志。
   *
   * @param s 日志消息
   */
  void error(String s);

  /**
   * 输出调试日志。
   *
   * @param s 日志消息
   */
  void debug(String s);

  /**
   * 输出跟踪日志。
   *
   * @param s 日志消息
   */
  void trace(String s);

  /**
   * 输出警告日志。
   *
   * @param s 日志消息
   */
  void warn(String s);

}
