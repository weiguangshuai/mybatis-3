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
package org.apache.ibatis.logging.nologging;

import org.apache.ibatis.logging.Log;

/**
 * 不执行任何日志操作的 Log 接口实现，用于禁用日志记录场景。
 *
 * @author Clinton Begin
 */
public class NoLoggingImpl implements Log {

  /**
   * 记录日志对应的类名。
   */
  private String clazz;

  /**
   * 构造方法。
   *
   * @param clazz 日志所属的类名
   */
  public NoLoggingImpl(String clazz) {
    // 不需要保存类名，因为所有操作都是空实现
    this.clazz = clazz;
  }

  @Override
  public boolean isDebugEnabled() {
    // 禁用日志时，调试级别始终不可用
    return false;
  }

  @Override
  public boolean isTraceEnabled() {
    // 禁用日志时，追踪级别始终不可用
    return false;
  }

  @Override
  public void error(String s, Throwable e) {
    // 禁用日志，不输出任何信息
  }

  @Override
  public void error(String s) {
    // 禁用日志，不输出任何信息
  }

  @Override
  public void debug(String s) {
    // 禁用日志，不输出任何信息
  }

  @Override
  public void trace(String s) {
    // 禁用日志，不输出任何信息
  }

  @Override
  public void warn(String s) {
    // 禁用日志，不输出任何信息
  }

}
