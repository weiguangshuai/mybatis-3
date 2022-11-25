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
package org.apache.ibatis.logging.log4j;

import org.apache.ibatis.logging.Log;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Log4j日志实现类，基于Apache Log4j 1.x提供日志记录功能。
 * @author Eduardo Macarron
 * @deprecated Since 3.5.9 - See https://github.com/mybatis/mybatis-3/issues/1223. This class will remove future.
 */
@Deprecated
public class Log4jImpl implements Log {

  /** 日志调用者的完全限定类名，用于在日志中正确显示调用者位置 */
  private static final String FQCN = Log4jImpl.class.getName();

  /** Log4j的Logger实例，用于执行实际的日志记录操作 */
  private final Logger log;

  /**
   * 构造方法，根据指定的类名获取Log4j Logger实例。
   * @param clazz 日志输出所在的类，用于创建对应的Logger
   */
  public Log4jImpl(String clazz) {
    log = Logger.getLogger(clazz);
  }

  /** 检查DEBUG级别日志是否启用 */
  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  /** 检查TRACE级别日志是否启用 */
  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  /**
   * 记录ERROR级别日志，包含异常信息。
   * @param s 日志消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    log.log(FQCN, Level.ERROR, s, e);
  }

  /** 记录ERROR级别日志。 */
  @Override
  public void error(String s) {
    // 异常参数为null，表示仅记录消息不含异常堆栈
    log.log(FQCN, Level.ERROR, s, null);
  }

  /** 记录DEBUG级别日志。 */
  @Override
  public void debug(String s) {
    log.log(FQCN, Level.DEBUG, s, null);
  }

  /** 记录TRACE级别日志。 */
  @Override
  public void trace(String s) {
    log.log(FQCN, Level.TRACE, s, null);
  }

  /** 记录WARN级别日志。 */
  @Override
  public void warn(String s) {
    log.log(FQCN, Level.WARN, s, null);
  }

}
