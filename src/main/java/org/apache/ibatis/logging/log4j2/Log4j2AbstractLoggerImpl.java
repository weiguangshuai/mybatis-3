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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

/**
 * Log4j2 日志抽象实现类，实现了 MyBatis 的 Log 接口。
 * 负责将日志调用委托给 Log4j2 的 ExtendedLoggerWrapper 进行实际输出。
 *
 * @author Eduardo Macarron
 */
public class Log4j2AbstractLoggerImpl implements Log {

  /** MyBatis 日志标记，用于标识日志来源 */
  private static final Marker MARKER = MarkerManager.getMarker(LogFactory.MARKER);

  /** 完全限定类名，用于标识日志调用者来源 */
  private static final String FQCN = Log4j2Impl.class.getName();

  /** Log4j2 扩展日志包装器，提供日志记录能力 */
  private final ExtendedLoggerWrapper log;

  /**
   * 构造 Log4j2 抽象日志实现类。
   *
   * @param abstractLogger Log4j2 的抽象日志实例，用于包装为扩展日志包装器
   */
  public Log4j2AbstractLoggerImpl(AbstractLogger abstractLogger) {
    log = new ExtendedLoggerWrapper(abstractLogger, abstractLogger.getName(), abstractLogger.getMessageFactory());
  }

  /**
   * 检查 DEBUG 级别日志是否启用。
   *
   * @return 如果启用返回 true，否则返回 false
   */
  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  /**
   * 检查 TRACE 级别日志是否启用。
   *
   * @return 如果启用返回 true，否则返回 false
   */
  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  /**
   * 记录 ERROR 级别日志，包含异常信息。
   *
   * @param s 日志消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    log.logIfEnabled(FQCN, Level.ERROR, MARKER, (Message) new SimpleMessage(s), e);
  }

  /**
   * 记录 ERROR 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void error(String s) {
    log.logIfEnabled(FQCN, Level.ERROR, MARKER, (Message) new SimpleMessage(s), null);
  }

  /**
   * 记录 DEBUG 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void debug(String s) {
    log.logIfEnabled(FQCN, Level.DEBUG, MARKER, (Message) new SimpleMessage(s), null);
  }

  /**
   * 记录 TRACE 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void trace(String s) {
    log.logIfEnabled(FQCN, Level.TRACE, MARKER, (Message) new SimpleMessage(s), null);
  }

  /**
   * 记录 WARN 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void warn(String s) {
    log.logIfEnabled(FQCN, Level.WARN, MARKER, (Message) new SimpleMessage(s), null);
  }

}
