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
package org.apache.ibatis.logging.log4j;

import org.apache.ibatis.logging.Log;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Log4j 日志实现类，将 MyBatis 日志输出到 Log4j 框架。
 *
 * @author Eduardo Macarron
 */
public class Log4jImpl implements Log {
  
  /** 用于标识日志调用者的全限定类名 */
  private static final String FQCN = Log4jImpl.class.getName();

  /** Log4j Logger 实例 */
  private final Logger log;

  /**
   * 构造方法，获取指定类的 Log4j Logger 实例。
   *
   * @param clazz 日志输出所属的类
   */
  public Log4jImpl(String clazz) {
    log = Logger.getLogger(clazz);
  }

  @Override
  public boolean isDebugEnabled() {
    // 检查 Log4j 配置是否启用了 DEBUG 级别
    return log.isDebugEnabled();
  }

  @Override
  public boolean isTraceEnabled() {
    // 检查 Log4j 配置是否启用了 TRACE 级别
    return log.isTraceEnabled();
  }

  /**
   * 记录 ERROR 级别日志，带异常信息。
   *
   * @param s 日志消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    log.log(FQCN, Level.ERROR, s, e);
  }

  /**
   * 记录 ERROR 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void error(String s) {
    log.log(FQCN, Level.ERROR, s, null);
  }

  /**
   * 记录 DEBUG 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void debug(String s) {
    log.log(FQCN, Level.DEBUG, s, null);
  }

  /**
   * 记录 TRACE 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void trace(String s) {
    log.log(FQCN, Level.TRACE, s, null);
  }

  /**
   * 记录 WARN 级别日志。
   *
   * @param s 日志消息
   */
  @Override
  public void warn(String s) {
    log.log(FQCN, Level.WARN, s, null);
  }

}
