/**
 *    Copyright 2009-2017 the original author or authors.
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
package org.apache.ibatis.logging.jdk14;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ibatis.logging.Log;

/**
 * 基于 Java Util Logging 的日志实现适配器。
 *
 * @author Clinton Begin
 */
public class Jdk14LoggingImpl implements Log {

  /** JDK Logger 实例 */
  private final Logger log;

  /**
   * 创建指定类名的日志记录器。
   *
   * @param clazz 日志所属的类名
   */
  public Jdk14LoggingImpl(String clazz) {
    log = Logger.getLogger(clazz);
  }

  /**
   * 检查 DEBUG 级别日志是否启用。
   *
   * @return DEBUG 级别日志是否可记录
   */
  @Override
  public boolean isDebugEnabled() {
    // FINE 对应 DEBUG 级别
    return log.isLoggable(Level.FINE);
  }

  /**
   * 检查 TRACE 级别日志是否启用。
   *
   * @return TRACE 级别日志是否可记录
   */
  @Override
  public boolean isTraceEnabled() {
    // FINER 对应 TRACE 级别
    return log.isLoggable(Level.FINER);
  }

  /**
   * 记录 ERROR 级别日志，并附带异常信息。
   *
   * @param s 日志消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    // SEVERE 对应 ERROR 级别
    log.log(Level.SEVERE, s, e);
  }

  /**
   * 记录 ERROR 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void error(String s) {
    log.log(Level.SEVERE, s);
  }

  /**
   * 记录 DEBUG 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void debug(String s) {
    log.log(Level.FINE, s);
  }

  /**
   * 记录 TRACE 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void trace(String s) {
    log.log(Level.FINER, s);
  }

  /**
   * 记录 WARN 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void warn(String s) {
    log.log(Level.WARNING, s);
  }

}
