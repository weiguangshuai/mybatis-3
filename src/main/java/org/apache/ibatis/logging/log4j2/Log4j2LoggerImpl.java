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
package org.apache.ibatis.logging.log4j2;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * Log4j2 日志实现，将 MyBatis 日志桥接到 Log4j2 框架。
 *
 * @author Eduardo Macarron
 */
public class Log4j2LoggerImpl implements Log {

  /** Log4j2 标记，用于标识 MyBatis 产生的日志 */
  private static final Marker MARKER = MarkerManager.getMarker(LogFactory.MARKER);

  /** Log4j2 的 Logger 实例，实际日志输出委托给此对象 */
  private final Logger log;

  /**
   * 构造方法，接收 Log4j2 的 Logger 实例。
   *
   * @param logger Log4j2 Logger 对象
   */
  public Log4j2LoggerImpl(Logger logger) {
    log = logger;
  }

  /**
   * 检查调试级别日志是否启用。
   *
   * @return 如果启用调试级别返回 true
   */
  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  /**
   * 检查跟踪级别日志是否启用。
   *
   * @return 如果启用跟踪级别返回 true
   */
  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  /**
   * 记录错误级别日志，包含异常信息。
   *
   * @param s 日志消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    log.error(MARKER, s, e);
  }

  /**
   * 记录错误级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void error(String s) {
    log.error(MARKER, s);
  }

  /**
   * 记录调试级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void debug(String s) {
    log.debug(MARKER, s);
  }

  /**
   * 记录跟踪级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void trace(String s) {
    log.trace(MARKER, s);
  }

  /**
   * 记录警告级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void warn(String s) {
    log.warn(MARKER, s);
  }

}
