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
package org.apache.ibatis.logging.commons;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Apache Commons Logging 日志实现。
 *
 * @author Clinton Begin
 */
public class JakartaCommonsLoggingImpl implements org.apache.ibatis.logging.Log {

  /** 底层 Commons Log 实例 */
  private final Log log;

  /**
   * 构造方法。
   *
   * @param clazz 日志所属的类
   */
  public JakartaCommonsLoggingImpl(String clazz) {
    log = LogFactory.getLog(clazz);
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
   * 记录错误日志。
   *
   * @param s 日志消息
   * @param e 异常信息
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
