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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.AbstractLogger;

/**
 * Log4j2 日志实现，适配 MyBatis 日志接口到 Log4j2 框架。
 *
 * @author Eduardo Macarron
 */
public class Log4j2Impl implements Log {

  /** 内部日志对象，根据 Log4j2 版本选择具体实现 */
  private final Log log;

  /**
   * 构造 Log4j2 日志实例。
   *
   * @param clazz 日志所属的类，用于获取对应的 Logger
   */
  public Log4j2Impl(String clazz) {
    Logger logger = LogManager.getLogger(clazz);

    // 根据 Log4j2 版本选择合适的适配器实现
    if (logger instanceof AbstractLogger) {
      log = new Log4j2AbstractLoggerImpl((AbstractLogger) logger);
    } else {
      log = new Log4j2LoggerImpl(logger);
    }
  }

  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  /**
   * 输出错误日志及异常堆栈。
   *
   * @param s  日志消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    log.error(s, e);
  }

  /**
   * 输出错误日志。
   *
   * @param s 日志消息
   */
  @Override
  public void error(String s) {
    log.error(s);
  }

  /**
   * 输出调试日志。
   *
   * @param s 日志消息
   */
  @Override
  public void debug(String s) {
    log.debug(s);
  }

  /**
   * 输出跟踪日志。
   *
   * @param s 日志消息
   */
  @Override
  public void trace(String s) {
    log.trace(s);
  }

  /**
   * 输出警告日志。
   *
   * @param s 日志消息
   */
  @Override
  public void warn(String s) {
    log.warn(s);
  }

}
