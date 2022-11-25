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
package org.apache.ibatis.logging.stdout;

import org.apache.ibatis.logging.Log;

/**
 * 将日志输出到标准控制台的日志实现类。
 *
 * @author Clinton Begin
 */
public class StdOutImpl implements Log {

  /**
   * 构造方法，接收日志记录者名称。
   *
   * @param clazz 日志记录者对应的类名
   */
  public StdOutImpl(String clazz) {
    // 标准输出实现不保存类名信息，直接忽略
  }

  @Override
  public boolean isDebugEnabled() {
    // 标准输出始终启用调试级别
    return true;
  }

  @Override
  public boolean isTraceEnabled() {
    // 标准输出始终启用跟踪级别
    return true;
  }

  /**
   * 输出错误日志及异常堆栈信息。
   *
   * @param s 错误消息
   * @param e 异常对象
   */
  @Override
  public void error(String s, Throwable e) {
    // 输出错误消息到标准错误流
    System.err.println(s);
    // 输出异常堆栈到标准错误流
    e.printStackTrace(System.err);
  }

  /**
   * 输出错误日志。
   *
   * @param s 错误消息
   */
  @Override
  public void error(String s) {
    // 输出错误消息到标准错误流
    System.err.println(s);
  }

  /**
   * 输出调试日志。
   *
   * @param s 调试消息
   */
  @Override
  public void debug(String s) {
    // 输出调试消息到标准输出流
    System.out.println(s);
  }

  /**
   * 输出跟踪日志。
   *
   * @param s 跟踪消息
   */
  @Override
  public void trace(String s) {
    // 输出跟踪消息到标准输出流
    System.out.println(s);
  }

  /**
   * 输出警告日志。
   *
   * @param s 警告消息
   */
  @Override
  public void warn(String s) {
    // 输出警告消息到标准输出流
    System.out.println(s);
  }
}
