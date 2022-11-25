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
package org.apache.ibatis.logging.jdk14;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ibatis.logging.Log;

/**
 * 基于 Java Util Logging 的日志实现。
 * 将日志操作委托给 JDK 内置的 Logger。
 *
 * @author Clinton Begin
 */
public class Jdk14LoggingImpl implements Log {

  /** JDK 日志记录器实例 */
  private final Logger log;

  /**
   * 创建指定类的日志实例。
   *
   * @param clazz 日志所属的类名
   */
  public Jdk14LoggingImpl(String clazz) {
    log = Logger.getLogger(clazz);
  }

  @Override
  public boolean isDebugEnabled() {
    // JDK FINE 级别对应 Debug
    return log.isLoggable(Level.FINE);
  }

  @Override
  public boolean isTraceEnabled() {
    // JDK FINER 级别对应 Trace
    return log.isLoggable(Level.FINER);
  }

  /**
   * 记录错误日志及异常信息。
   *
   * @param s 错误消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    log.log(Level.SEVERE, s, e);
  }

  /**
   * 记录错误日志。
   *
   * @param s 错误消息
   */
  @Override
  public void error(String s) {
    log.log(Level.SEVERE, s);
  }

  /**
   * 记录调试日志。
   *
   * @param s 调试消息
   */
  @Override
  public void debug(String s) {
    log.log(Level.FINE, s);
  }

  /**
   * 记录跟踪日志。
   *
   * @param s 跟踪消息
   */
  @Override
  public void trace(String s) {
    log.log(Level.FINER, s);
  }

  /**
   * 记录警告日志。
   *
   * @param s 警告消息
   */
  @Override
  public void warn(String s) {
    log.log(Level.WARNING, s);
  }

}
