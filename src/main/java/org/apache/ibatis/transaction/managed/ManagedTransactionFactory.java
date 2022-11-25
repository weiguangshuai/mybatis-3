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
package org.apache.ibatis.transaction.managed;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;

/**
 * 用于创建 {@link ManagedTransaction} 实例的工厂类。
 * ManagedTransaction 将事务管理委托给外部容器（如 Spring），自身不执行 commit/rollback 操作。
 *
 * @author Clinton Begin
 *
 * @see ManagedTransaction
 */
public class ManagedTransactionFactory implements TransactionFactory {

  /** 是否在事务关闭时关闭底层数据库连接，默认为 true */
  private boolean closeConnection = true;

  /**
   * 根据配置属性初始化工厂。
   * 支持设置 closeConnection 参数控制是否在事务结束时关闭连接。
   *
   * @param props 配置属性，包含 closeConnection 选项
   */
  @Override
  public void setProperties(Properties props) {
    // 仅在属性非空时处理，支持动态配置 closeConnection 参数
    if (props != null) {
      String closeConnectionProperty = props.getProperty("closeConnection");
      if (closeConnectionProperty != null) {
        // 将字符串转换为布尔值，控制连接关闭行为
        closeConnection = Boolean.parseBoolean(closeConnectionProperty);
      }
    }
  }

  /**
   * 使用已有的数据库连接创建托管事务。
   *
   * @param conn 已存在的数据库连接
   * @return ManagedTransaction 实例
   */
  @Override
  public Transaction newTransaction(Connection conn) {
    return new ManagedTransaction(conn, closeConnection);
  }

  /**
   * 从数据源创建托管事务，忽略自动提交和隔离级别设置。
   * 托管事务由外部容器管理，这些参数由容器负责处理。
   *
   * @param ds 数据源
   * @param level 事务隔离级别（被忽略，由外部管理器控制）
   * @param autoCommit 自动提交标志（被忽略，由外部管理器控制）
   * @return ManagedTransaction 实例
   */
  @Override
  public Transaction newTransaction(DataSource ds, TransactionIsolationLevel level, boolean autoCommit) {
    // 托管事务完全由外部事务管理器控制，静默忽略 autoCommit 和 isolationLevel 参数
    // 这样代码在托管和非托管配置间可移植
    return new ManagedTransaction(ds, level, closeConnection);
  }
}
