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
package org.apache.ibatis.logging;

import java.lang.reflect.Constructor;

/**
 * 日志工厂类，负责为 MyBatis 提供统一的日志接口。
 * 自动检测并使用 classpath 中可用的日志框架（SLF4J > Commons Logging > Log4j2 > Log4j > JDK Logging）。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public final class LogFactory {

  /**
   * Marker to be used by logging implementations that support markers.
   */
  public static final String MARKER = "MYBATIS";

  /** 当前使用的日志实现类的构造函数 */
  private static Constructor<? extends Log> logConstructor;

  // 静态初始化：按优先级尝试加载日志框架，一旦成功则不再尝试其他框架
  static {
    tryImplementation(LogFactory::useSlf4jLogging);
    tryImplementation(LogFactory::useCommonsLogging);
    tryImplementation(LogFactory::useLog4J2Logging);
    tryImplementation(LogFactory::useLog4JLogging);
    tryImplementation(LogFactory::useJdkLogging);
    tryImplementation(LogFactory::useNoLogging);
  }

  private LogFactory() {
    // disable construction
  }

  /**
   * 获取指定类的日志实例。
   *
   * @param clazz 日志所属的类
   * @return 日志实例
   */
  public static Log getLog(Class<?> clazz) {
    return getLog(clazz.getName());
  }

  /**
   * 获取指定名称的日志实例。
   *
   * @param logger 日志名称
   * @return 日志实例
   */
  public static Log getLog(String logger) {
    try {
      // 使用已选择的日志实现类创建日志实例
      return logConstructor.newInstance(logger);
    } catch (Throwable t) {
      throw new LogException("Error creating logger for logger " + logger + ".  Cause: " + t, t);
    }
  }

  /**
   * 指定自定义的日志实现类。
   *
   * @param clazz 自定义的 Log 实现类
   */
  public static synchronized void useCustomLogging(Class<? extends Log> clazz) {
    setImplementation(clazz);
  }

  /** 使用 SLF4J 作为日志框架 */
  public static synchronized void useSlf4jLogging() {
    setImplementation(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
  }

  /** 使用 Apache Commons Logging 作为日志框架 */
  public static synchronized void useCommonsLogging() {
    setImplementation(org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl.class);
  }

  /**
   * @deprecated Since 3.5.9 - See https://github.com/mybatis/mybatis-3/issues/1223. This method will remove future.
   */
  @Deprecated
  public static synchronized void useLog4JLogging() {
    setImplementation(org.apache.ibatis.logging.log4j.Log4jImpl.class);
  }

  /** 使用 Log4j2 作为日志框架 */
  public static synchronized void useLog4J2Logging() {
    setImplementation(org.apache.ibatis.logging.log4j2.Log4j2Impl.class);
  }

  /** 使用 JDK Logging 作为日志框架 */
  public static synchronized void useJdkLogging() {
    setImplementation(org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl.class);
  }

  /** 使用标准输出流作为日志输出 */
  public static synchronized void useStdOutLogging() {
    setImplementation(org.apache.ibatis.logging.stdout.StdOutImpl.class);
  }

  /** 禁用日志功能 */
  public static synchronized void useNoLogging() {
    setImplementation(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
  }

  /**
   * 尝试执行日志框架初始化，仅在尚未确定日志实现时执行。
   * 初始化失败时静默忽略，继续尝试下一个框架。
   */
  private static void tryImplementation(Runnable runnable) {
    if (logConstructor == null) {
      try {
        runnable.run();
      } catch (Throwable t) {
        // 静默忽略，继续尝试其他日志框架
      }
    }
  }

  /**
   * 设置日志实现类，并验证该类是否可用。
   * 验证时创建一个测试日志实例，若成功则记录调试信息。
   */
  private static void setImplementation(Class<? extends Log> implClass) {
    try {
      Constructor<? extends Log> candidate = implClass.getConstructor(String.class);
      // 创建测试实例验证日志实现是否可用
      Log log = candidate.newInstance(LogFactory.class.getName());
      if (log.isDebugEnabled()) {
        log.debug("Logging initialized using '" + implClass + "' adapter.");
      }
      logConstructor = candidate;
    } catch (Throwable t) {
      throw new LogException("Error setting Log implementation.  Cause: " + t, t);
    }
  }

}
