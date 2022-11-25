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
package org.apache.ibatis.logging.slf4j;

import org.apache.ibatis.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

/**
 * SLF4J 日志实现适配器，根据 SLF4J 版本自动选择合适的日志实现。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class Slf4jImpl implements Log {

  /** 委托的日志实例，可能是 Slf4jLocationAwareLoggerImpl 或 Slf4jLoggerImpl */
  private Log log;

  /**
   * 构造 SLF4J 日志实现，根据 SLF4J 版本选择 LocationAware 或普通实现。
   *
   * @param clazz 日志所属的类名
   */
  public Slf4jImpl(String clazz) {
    Logger logger = LoggerFactory.getLogger(clazz);

    // SLF4J 1.6+ 提供 LocationAwareLogger，支持更精确的日志位置追踪
    if (logger instanceof LocationAwareLogger) {
      try {
        // 检查是否支持 1.6+ 版本的方法签名
        logger.getClass().getMethod("log", Marker.class, String.class, int.class, String.class, Object[].class, Throwable.class);
        log = new Slf4jLocationAwareLoggerImpl((LocationAwareLogger) logger);
        return;
      } catch (SecurityException | NoSuchMethodException e) {
        // 版本不支持，降级使用普通实现
      }
    }

    // 非 LocationAwareLogger 或 SLF4J 版本低于 1.6，使用普通实现
    log = new Slf4jLoggerImpl(logger);
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
   * 输出错误日志，包含异常信息。
   *
   * @param s 日志消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    log.error(s, e);
  }

  @Override
  public void error(String s) {
    log.error(s);
  }

  @Override
  public void debug(String s) {
    log.debug(s);
  }

  @Override
  public void trace(String s) {
    log.trace(s);
  }

  @Override
  public void warn(String s) {
    log.warn(s);
  }

}
