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
package org.apache.ibatis.logging.commons;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 基于 Apache Commons Logging 的日志实现适配器。
 *
 * @author Clinton Begin
 */
public class JakartaCommonsLoggingImpl implements org.apache.ibatis.logging.Log {

  /** 内部封装的 Commons Logging 日志实例 */
  private final Log log;

  /**
   * 构造方法，根据指定类名创建日志实例。
   *
   * @param clazz 日志所属的类，用于标识日志来源
   */
  public JakartaCommonsLoggingImpl(String clazz) {
    log = LogFactory.getLog(clazz);
  }

  /**
   * 检查 debug 级别日志是否启用。
   *
   * @return 是否启用 debug 级别日志
   */
  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  /**
   * 检查 trace 级别日志是否启用。
   *
   * @return 是否启用 trace 级别日志
   */
  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  /**
   * 记录错误级别日志，并附带异常信息。
   *
   * @param s  日志消息
   * @param e  异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    log.error(s, e);
  }

  /**
   * 记录错误级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void error(String s) {
    log.error(s);
  }

  /**
   * 记录调试级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void debug(String s) {
    log.debug(s);
  }

  /**
   * 记录跟踪级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void trace(String s) {
    log.trace(s);
  }

  /**
   * 记录警告级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void warn(String s) {
    log.warn(s);
  }

}
