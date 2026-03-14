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
package org.apache.ibatis.logging.log4j2;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * Log4j2 日志实现适配器，将 MyBatis 日志接口桥接到 Log4j2 框架。
 *
 * @author Eduardo Macarron
 */
public class Log4j2LoggerImpl implements Log {
  
  /** MyBatis 专用的日志标记，用于在 Log4j2 中标识日志来源 */
  private static final Marker MARKER = MarkerManager.getMarker(LogFactory.MARKER);

  /** 底层 Log4j2 Logger 实例 */
  private final Logger log;

  /**
   * 构造 Log4j2 日志适配器。
   *
   * @param logger Log4j2 Logger 实例
   */
  public Log4j2LoggerImpl(Logger logger) {
    log = logger;
  }

  /**
   * 判断 DEBUG 级别日志是否启用。
   *
   * @return DEBUG 日志启用返回 true
   */
  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  /**
   * 判断 TRACE 级别日志是否启用。
   *
   * @return TRACE 日志启用返回 true
   */
  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  /**
   * 输出错误日志，并附带异常信息。
   *
   * @param s 日志消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    // 输出错误日志及异常堆栈
    log.error(MARKER, s, e);
  }

  /**
   * 输出错误日志。
   *
   * @param s 日志消息
   */
  @Override
  public void error(String s) {
    log.error(MARKER, s);
  }

  /**
   * 输出调试日志。
   *
   * @param s 日志消息
   */
  @Override
  public void debug(String s) {
    log.debug(MARKER, s);
  }

  /**
   * 输出跟踪日志。
   *
   * @param s 日志消息
   */
  @Override
  public void trace(String s) {
    log.trace(MARKER, s);
  }

  /**
   * 输出警告日志。
   *
   * @param s 日志消息
   */
  @Override
  public void warn(String s) {
    log.warn(MARKER, s);
  }

}
