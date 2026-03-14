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
package org.apache.ibatis.logging.slf4j;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.spi.LocationAwareLogger;

/**
 * 基于 SLF4J LocationAwareLogger 的日志实现类。
 * 实现了 MyBatis 的 Log 接口，将日志操作委托给 SLF4J 的 LocationAwareLogger。
 *
 * @author Eduardo Macarron
 */
class Slf4jLocationAwareLoggerImpl implements Log {

  /** SLF4J 标记，用于标识 MyBatis 日志 */
  private static final Marker MARKER = MarkerFactory.getMarker(LogFactory.MARKER);

  /** 完全限定类名，用于日志定位 */
  private static final String FQCN = Slf4jImpl.class.getName();

  /** SLF4J LocationAwareLogger 实例 */
  private final LocationAwareLogger logger;

  /**
   * 构造方法。
   *
   * @param logger SLF4J LocationAwareLogger 实例
   */
  Slf4jLocationAwareLoggerImpl(LocationAwareLogger logger) {
    this.logger = logger;
  }

  /**
   * 判断 DEBUG 级别日志是否启用。
   *
   * @return 如果启用返回 true
   */
  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  /**
   * 判断 TRACE 级别日志是否启用。
   *
   * @return 如果启用返回 true
   */
  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  /**
   * 输出 ERROR 级别日志。
   *
   * @param s 日志消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    logger.log(MARKER, FQCN, LocationAwareLogger.ERROR_INT, s, null, e);
  }

  /**
   * 输出 ERROR 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void error(String s) {
    logger.log(MARKER, FQCN, LocationAwareLogger.ERROR_INT, s, null, null);
  }

  /**
   * 输出 DEBUG 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void debug(String s) {
    logger.log(MARKER, FQCN, LocationAwareLogger.DEBUG_INT, s, null, null);
  }

  /**
   * 输出 TRACE 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void trace(String s) {
    logger.log(MARKER, FQCN, LocationAwareLogger.TRACE_INT, s, null, null);
  }

  /**
   * 输出 WARN 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void warn(String s) {
    logger.log(MARKER, FQCN, LocationAwareLogger.WARN_INT, s, null, null);
  }

}
