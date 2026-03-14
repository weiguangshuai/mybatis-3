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
package org.apache.ibatis.datasource;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * 数据源层异常基类，封装数据源操作过程中的错误信息。
 *
 * @author Clinton Begin
 */
public class DataSourceException extends PersistenceException {

  /** 序列化版本号，确保反序列化兼容 */
  private static final long serialVersionUID = -5251396250407091334L;

  /** 默认构造函数 */
  public DataSourceException() {
    super();
  }

  /**
   * 使用指定消息创建异常。
   *
   * @param message 异常消息
   */
  public DataSourceException(String message) {
    super(message);
  }

  /**
   * 使用指定消息和根因创建异常。
   *
   * @param message 异常消息
   * @param cause   原始异常
   */
  public DataSourceException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 使用指定根因创建异常。
   *
   * @param cause 原始异常
   */
  public DataSourceException(Throwable cause) {
    super(cause);
  }

}
