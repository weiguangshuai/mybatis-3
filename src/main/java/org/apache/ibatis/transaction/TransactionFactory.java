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

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;

/**
 * 事务工厂接口，负责创建 {@link Transaction} 事务实例。
 * 根据配置和数据源信息，创建符合需求的事务对象。
 *
 * @author Clinton Begin
 */
public interface TransactionFactory {

  /**
   * 设置事务工厂的自定义属性。
   * 用于配置事务工厂的特定行为，如超时时间、事务隔离级别等。
   *
   * @param props 配置属性集合
   */
  void setProperties(Properties props);

  /**
   * 基于已有的数据库连接创建事务。
   * 适用于需要复用现有连接或在外部管理连接的场景。
   *
   * @param conn 已存在的数据库连接
   * @return 事务对象
   * @since 3.1.0
   */
  Transaction newTransaction(Connection conn);
  
  /**
   * 基于数据源创建事务。
   * 从指定的数据源获取连接，并按照指定的隔离级别和自动提交设置创建事务。
   *
   * @param dataSource 数据源，用于获取数据库连接
   * @param level 事务隔离级别
   * @param autoCommit 是否自动提交
   * @return 事务对象
   * @since 3.1.0
   */
  Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit);

}
