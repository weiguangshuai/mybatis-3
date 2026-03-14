/**
 *    Copyright 2009-2016 the original author or authors.
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
package org.apache.ibatis.logging;

import java.lang.reflect.Constructor;

/**
 * 日志工厂类，负责日志系统的抽象和底层日志实现的选择。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public final class LogFactory {

  /**
   * 日志标记，用于支持标记的日志实现
   */
  public static final String MARKER = "MYBATIS";

  /** 当前使用的日志实现类的构造函数 */
  private static Constructor<? extends Log> logConstructor;

  static {
    // 按优先级尝试初始化日志实现，成功则后续不再尝试
    tryImplementation(new Runnable() {
      @Override
      public void run() {
        useSlf4jLogging();
      }
    });
    tryImplementation(new Runnable() {
      @Override
      public void run() {
        useCommonsLogging();
      }
    });
    tryImplementation(new Runnable() {
      @Override
      public void run() {
        useLog4J2Logging();
      }
    });
    tryImplementation(new Runnable() {
      @Override
      public void run() {
        useLog4JLogging();
      }
    });
    tryImplementation(new Runnable() {
      @Override
      public void run() {
        useJdkLogging();
      }
    });
    tryImplementation(new Runnable() {
      @Override
      public void run() {
        useNoLogging();
      }
    });
  }

  private LogFactory() {
    // disable construction
  }

  /**
   * 根据类获取日志实例。
   *
   * @param aClass 日志所属的类
   * @return 日志实例
   */
  public static Log getLog(Class<?> aClass) {
    return getLog(aClass.getName());
  }

  /**
   * 根据名称获取日志实例。
   *
   * @param logger 日志名称
   * @return 日志实例
   */
  public static Log getLog(String logger) {
    try {
      return logConstructor.newInstance(logger);
    } catch (Throwable t) {
      // 日志实现初始化失败时抛出异常
      throw new LogException("Error creating logger for logger " + logger + ".  Cause: " + t, t);
    }
  }

  /**
   * 使用自定义日志实现。
   *
   * @param clazz 日志实现类，必须继承自 Log 接口
   */
  public static synchronized void useCustomLogging(Class<? extends Log> clazz) {
    setImplementation(clazz);
  }

  /**
   * 使用 SLF4J 日志框架。
   */
  public static synchronized void useSlf4jLogging() {
    setImplementation(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
  }

  /**
   * 使用 Apache Commons Logging 日志框架。
   */
  public static synchronized void useCommonsLogging() {
    setImplementation(org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl.class);
  }

  /**
   * 使用 Log4j 1.x 日志框架。
   */
  public static synchronized void useLog4JLogging() {
    setImplementation(org.apache.ibatis.logging.log4j.Log4jImpl.class);
  }

  /**
   * 使用 Log4j 2.x 日志框架。
   */
  public static synchronized void useLog4J2Logging() {
    setImplementation(org.apache.ibatis.logging.log4j2.Log4j2Impl.class);
  }

  /**
   * 使用 JDK 内置的日志框架。
   */
  public static synchronized void useJdkLogging() {
    setImplementation(org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl.class);
  }

  /**
   * 使用标准输出打印日志。
   */
  public static synchronized void useStdOutLogging() {
    setImplementation(org.apache.ibatis.logging.stdout.StdOutImpl.class);
  }

  /**
   * 禁用日志功能。
   */
  public static synchronized void useNoLogging() {
    setImplementation(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
  }

  /**
   * 尝试执行日志实现初始化，只有在尚未初始化时才执行。
   * 初始化失败时静默忽略异常。
   *
   * @param runnable 初始化任务
   */
  private static void tryImplementation(Runnable runnable) {
    // 仅当未初始化日志实现时才尝试
    if (logConstructor == null) {
      try {
        runnable.run();
      } catch (Throwable t) {
        // 静默忽略初始化失败，尝试下一个日志框架
      }
    }
  }

  /**
   * 设置日志实现类，验证可用性后保存构造函数。
   *
   * @param implClass 日志实现类
   * @throws LogException 如果设置失败
   */
  private static void setImplementation(Class<? extends Log> implClass) {
    try {
      Constructor<? extends Log> candidate = implClass.getConstructor(String.class);
      Log log = candidate.newInstance(LogFactory.class.getName());
      // 如果调试级别启用，输出初始化日志
      if (log.isDebugEnabled()) {
        log.debug("Logging initialized using '" + implClass + "' adapter.");
      }
      logConstructor = candidate;
    } catch (Throwable t) {
      throw new LogException("Error setting Log implementation.  Cause: " + t, t);
    }
  }

}
