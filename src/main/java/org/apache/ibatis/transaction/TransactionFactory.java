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

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;

/**
 * 创建 {@link Transaction} 实例的工厂接口。
 *
 * @author Clinton Begin
 */
public interface TransactionFactory {

  /**
   * 设置事务工厂的自定义属性。
   * @param props 新的配置属性
   */
  default void setProperties(Properties props) {
    // 空实现，供子类覆盖
  }

  /**
   * 基于现有数据库连接创建事务。
   * @param conn 现有的数据库连接
   * @return 事务实例
   * @since 3.1.0
   */
  Transaction newTransaction(Connection conn);

  /**
   * 基于数据源创建事务。
   * @param dataSource 数据源，用于获取数据库连接
   * @param level 事务隔离级别
   * @param autoCommit 是否自动提交
   * @return 事务实例
   * @since 3.1.0
   */
  Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit);

}
