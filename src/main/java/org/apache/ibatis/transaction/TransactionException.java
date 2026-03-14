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
package org.apache.ibatis.transaction;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * 事务操作相关异常的基类
 *
 * @author Clinton Begin
 */
public class TransactionException extends PersistenceException {

  private static final long serialVersionUID = -433589569461084605L;

  /**
   * 构造无详细信息的 TransactionException
   */
  public TransactionException() {
    super();
  }

  /**
   * 构造带指定错误信息的 TransactionException
   *
   * @param message 错误详情
   */
  public TransactionException(String message) {
    super(message);
  }

  /**
   * 构造带错误信息和原始异常的 TransactionException
   *
   * @param message 错误详情
   * @param cause 原始异常
   */
  public TransactionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 构造带原始异常的 TransactionException
   *
   * @param cause 原始异常
   */
  public TransactionException(Throwable cause) {
    super(cause);
  }

}
