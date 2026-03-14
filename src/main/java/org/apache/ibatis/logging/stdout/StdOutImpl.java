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
package org.apache.ibatis.logging.stdout;

import org.apache.ibatis.logging.Log;

/**
 * 将日志输出到标准控制台的日志实现。
 *
 * @author Clinton Begin
 */
public class StdOutImpl implements Log {

  /**
   * 构造函数，接收日志输出的类名。
   *
   * @param clazz 日志对应的类名
   */
  public StdOutImpl(String clazz) {
    // Do Nothing
  }

  /**
   * 判断是否启用 DEBUG 级别日志。
   *
   * @return 始终返回 true，表示始终启用 DEBUG 日志
   */
  @Override
  public boolean isDebugEnabled() {
    return true;
  }

  /**
   * 判断是否启用 TRACE 级别日志。
   *
   * @return 始终返回 true，表示始终启用 TRACE 日志
   */
  @Override
  public boolean isTraceEnabled() {
    return true;
  }

  /**
   * 输出 ERROR 级别日志及异常堆栈信息。
   *
   * @param s 日志消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    // 输出错误消息到标准错误流
    System.err.println(s);
    // 输出异常堆栈到标准错误流
    e.printStackTrace(System.err);
  }

  /**
   * 输出 ERROR 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void error(String s) {
    System.err.println(s);
  }

  /**
   * 输出 DEBUG 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void debug(String s) {
    System.out.println(s);
  }

  /**
   * 输出 TRACE 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void trace(String s) {
    System.out.println(s);
  }

  /**
   * 输出 WARN 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void warn(String s) {
    System.out.println(s);
  }
}
