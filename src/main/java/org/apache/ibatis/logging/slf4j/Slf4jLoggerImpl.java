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

/**
 * 基于 SLF4J 框架的日志实现，将日志操作委托给 SLF4J 的 Logger。
 *
 * @author Eduardo Macarron
 */
class Slf4jLoggerImpl implements Log {

  /** SLF4J 日志器实例 */
  private final Logger log;

  /**
   * 构造方法，接收 SLF4J Logger 实例。
   *
   * @param logger SLF4J 日志器
   */
  public Slf4jLoggerImpl(Logger logger) {
    log = logger;
  }

  /**
   * 判断当前日志级别是否支持 DEBUG 级别。
   *
   * @return 如果支持 DEBUG 级别则返回 true
   */
  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  /**
   * 判断当前日志级别是否支持 TRACE 级别。
   *
   * @return 如果支持 TRACE 级别则返回 true
   */
  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  /**
   * 记录错误日志，并附带异常信息。
   *
   * @param s 日志消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    log.error(s, e);
  }

  /**
   * 记录错误日志。
   *
   * @param s 日志消息
   */
  @Override
  public void error(String s) {
    log.error(s);
  }

  /**
   * 记录调试日志。
   *
   * @param s 日志消息
   */
  @Override
  public void debug(String s) {
    log.debug(s);
  }

  /**
   * 记录跟踪日志。
   *
   * @param s 日志消息
   */
  @Override
  public void trace(String s) {
    log.trace(s);
  }

  /**
   * 记录警告日志。
   *
   * @param s 日志消息
   */
  @Override
  public void warn(String s) {
    log.warn(s);
  }

}
