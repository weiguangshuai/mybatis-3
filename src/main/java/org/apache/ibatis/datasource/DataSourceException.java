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
package org.apache.ibatis.datasource;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * 数据源相关的异常基类，封装数据源操作中可能出现的错误。
 *
 * @author Clinton Begin
 */
public class DataSourceException extends PersistenceException {

  /** 序列化版本标识 */
  private static final long serialVersionUID = -5251396250407091334L;

  /** 构造无详细信息的异常 */
  public DataSourceException() {
    super();
  }

  /** 构造带指定消息的异常 */
  public DataSourceException(String message) {
    super(message);
  }

  /** 构造带详细消息和根因的异常 */
  public DataSourceException(String message, Throwable cause) {
    super(message, cause);
  }

  /** 构造带根因的异常 */
  public DataSourceException(Throwable cause) {
    super(cause);
  }

}
