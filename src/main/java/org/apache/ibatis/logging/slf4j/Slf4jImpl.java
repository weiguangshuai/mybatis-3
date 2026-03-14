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
package org.apache.ibatis.logging.slf4j;

import org.apache.ibatis.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

/**
 * MyBatis SLF4J日志实现适配器。
 * 负责将MyBatis的Log接口调用转发到SLF4J日志框架。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class Slf4jImpl implements Log {

  /** 内部日志委托对象，可能是Slf4jLocationAwareLoggerImpl或Slf4jLoggerImpl */
  private Log log;

  /**
   * 构造SLF4J日志适配器实例。
   *
   * @param clazz 日志对应的类对象名称
   */
  public Slf4jImpl(String clazz) {
    Logger logger = LoggerFactory.getLogger(clazz);

    if (logger instanceof LocationAwareLogger) {
      try {
        // 检查SLF4J版本是否支持1.6及以上的方法签名
        logger.getClass().getMethod("log", Marker.class, String.class, int.class, String.class, Object[].class, Throwable.class);
        log = new Slf4jLocationAwareLoggerImpl((LocationAwareLogger) logger);
        return;
      } catch (SecurityException e) {
        // SLF4J版本不支持，回退到普通日志实现
      } catch (NoSuchMethodException e) {
        // SLF4J版本不支持，回退到普通日志实现
      }
    }

    // 使用普通日志实现（Logger不是LocationAwareLogger或SLF4J版本低于1.6）
    log = new Slf4jLoggerImpl(logger);
  }

  /**
   * 判断DEBUG级别日志是否启用。
   *
   * @return 如果启用DEBUG级别日志则返回true
   */
  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  /**
   * 判断TRACE级别日志是否启用。
   *
   * @return 如果启用TRACE级别日志则返回true
   */
  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  /**
   * 记录ERROR级别日志（带异常信息）。
   *
   * @param s 日志消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    log.error(s, e);
  }

  /**
   * 记录ERROR级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void error(String s) {
    log.error(s);
  }

  /**
   * 记录DEBUG级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void debug(String s) {
    log.debug(s);
  }

  /**
   * 记录TRACE级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void trace(String s) {
    log.trace(s);
  }

  /**
   * 记录WARN级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void warn(String s) {
    log.warn(s);
  }

}
