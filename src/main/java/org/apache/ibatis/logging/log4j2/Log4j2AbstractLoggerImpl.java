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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

/**
 * Log4j2 日志实现抽象类，基于 Log4j2 的 AbstractLogger 封装 MyBatis 的 Log 接口。
 *
 * @author Eduardo Macarron
 */
public class Log4j2AbstractLoggerImpl implements Log {

  /** Log4j2 标记，用于标识 MyBatis 日志 */
  private static final Marker MARKER = MarkerManager.getMarker(LogFactory.MARKER);

  /** 完全限定类名，用于日志调用者识别 */
  private static final String FQCN = Log4j2Impl.class.getName();

  /** Log4j2 扩展日志包装器 */
  private final ExtendedLoggerWrapper log;

  /**
   * 构造方法，使用 AbstractLogger 初始化日志包装器。
   *
   * @param abstractLogger Log4j2 抽象日志记录器
   */
  public Log4j2AbstractLoggerImpl(AbstractLogger abstractLogger) {
    log = new ExtendedLoggerWrapper(abstractLogger, abstractLogger.getName(), abstractLogger.getMessageFactory());
  }

  /**
   * 判断 DEBUG 级别日志是否启用。
   *
   * @return DEBUG 日志启用返回 true，否则返回 false
   */
  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  /**
   * 判断 TRACE 级别日志是否启用。
   *
   * @return TRACE 日志启用返回 true，否则返回 false
   */
  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  /**
   * 记录 ERROR 级别日志，包含异常信息。
   *
   * @param s 日志消息
   * e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    log.logIfEnabled(FQCN, Level.ERROR, MARKER, new SimpleMessage(s), e);
  }

  /**
   * 记录 ERROR 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void error(String s) {
    log.logIfEnabled(FQCN, Level.ERROR, MARKER, new SimpleMessage(s), null);
  }

  /**
   * 记录 DEBUG 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void debug(String s) {
    log.logIfEnabled(FQCN, Level.DEBUG, MARKER, new SimpleMessage(s), null);
  }

  /**
   * 记录 TRACE 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void trace(String s) {
    log.logIfEnabled(FQCN, Level.TRACE, MARKER, new SimpleMessage(s), null);
  }

  /**
   * 记录 WARN 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void warn(String s) {
    log.logIfEnabled(FQCN, Level.WARN, MARKER, new SimpleMessage(s), null);
  }

}
