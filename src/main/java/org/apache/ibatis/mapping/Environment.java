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
package org.apache.ibatis.mapping;

import javax.sql.DataSource;

import org.apache.ibatis.transaction.TransactionFactory;

/**
 * 封装数据库环境配置，包含事务工厂和数据源。
 *
 * @author Clinton Begin
 */
public final class Environment {
  /** 环境标识符，用于区分不同的数据库配置 */
  private final String id;
  /** 事务工厂，负责创建事务对象 */
  private final TransactionFactory transactionFactory;
  /** 数据源，负责提供数据库连接 */
  private final DataSource dataSource;

  /**
   * 构造环境配置。
   *
   * @param id 环境标识符
   * @param transactionFactory 事务工厂
   * @param dataSource 数据源
   * @throws IllegalArgumentException 如果任一参数为 null
   */
  public Environment(String id, TransactionFactory transactionFactory, DataSource dataSource) {
    // 校验环境标识符
    if (id == null) {
      throw new IllegalArgumentException("Parameter 'id' must not be null");
    }
    // 校验事务工厂
    if (transactionFactory == null) {
      throw new IllegalArgumentException("Parameter 'transactionFactory' must not be null");
    }
    this.id = id;
    // 校验数据源
    if (dataSource == null) {
      throw new IllegalArgumentException("Parameter 'dataSource' must not be null");
    }
    this.transactionFactory = transactionFactory;
    this.dataSource = dataSource;
  }

  /**
   * 构建器模式，用于创建 Environment 实例。
   */
  public static class Builder {
    /** 环境标识符 */
    private final String id;
    /** 事务工厂 */
    private TransactionFactory transactionFactory;
    /** 数据源 */
    private DataSource dataSource;

    /**
     * 创建构建器实例。
     *
     * @param id 环境标识符
     */
    public Builder(String id) {
      this.id = id;
    }

    /**
     * 设置事务工厂。
     *
     * @param transactionFactory 事务工厂
     * @return 当前构建器实例
     */
    public Builder transactionFactory(TransactionFactory transactionFactory) {
      this.transactionFactory = transactionFactory;
      return this;
    }

    /**
     * 设置数据源。
     *
     * @param dataSource 数据源
     * @return 当前构建器实例
     */
    public Builder dataSource(DataSource dataSource) {
      this.dataSource = dataSource;
      return this;
    }

    /**
     * 获取环境标识符。
     *
     * @return 环境标识符
     */
    public String id() {
      return this.id;
    }

    /**
     * 构建 Environment 实例。
     *
     * @return 新的 Environment 对象
     */
    public Environment build() {
      return new Environment(this.id, this.transactionFactory, this.dataSource);
    }

  }

  /**
   * 获取环境标识符。
   *
   * @return 环境标识符
   */
  public String getId() {
    return this.id;
  }

  /**
   * 获取事务工厂。
   *
   * @return 事务工厂
   */
  public TransactionFactory getTransactionFactory() {
    return this.transactionFactory;
  }

  /**
   * 获取数据源。
   *
   * @return 数据源
   */
  public DataSource getDataSource() {
    return this.dataSource;
  }

}
