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
package org.apache.ibatis.transaction;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * 事务操作异常基类，封装事务处理过程中发生的各种错误。
 *
 * @author Clinton Begin
 */
public class TransactionException extends PersistenceException {

  /** 序列化版本号，确保反序列化兼容性 */
  private static final long serialVersionUID = -433589569461084605L;

  /** 无参构造器 */
  public TransactionException() {
    super();
  }

  /** 指定异常消息 */
  public TransactionException(String message) {
    super(message);
  }

  /** 指定异常消息和根本原因 */
  public TransactionException(String message, Throwable cause) {
    super(message, cause);
  }

  /** 指定根本原因 */
  public TransactionException(Throwable cause) {
    super(cause);
  }

}
