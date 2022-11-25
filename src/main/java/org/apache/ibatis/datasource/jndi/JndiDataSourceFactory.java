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
package org.apache.ibatis.datasource.jndi;

import java.util.Map.Entry;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceException;
import org.apache.ibatis.datasource.DataSourceFactory;

/**
 * 通过 JNDI 方式获取 DataSource 的工厂实现。
 *
 * @author Clinton Begin
 */
public class JndiDataSourceFactory implements DataSourceFactory {

  /** JNDI 初始上下文的环境属性键 */
  public static final String INITIAL_CONTEXT = "initial_context";
  /** JNDI 数据源的资源引用名键 */
  public static final String DATA_SOURCE = "data_source";
  /** 环境属性的前缀，用于从配置中提取 JNDI 环境变量 */
  public static final String ENV_PREFIX = "env.";

  /** 缓存获取到的 JNDI DataSource 实例 */
  private DataSource dataSource;

  /**
   * 根据配置属性初始化 JNDI DataSource。
   *
   * @param properties 包含 JNDI 配置的属性集
   */
  @Override
  public void setProperties(Properties properties) {
    try {
      InitialContext initCtx;
      // 提取以 env. 前缀开头的属性作为 JNDI 环境变量
      Properties env = getEnvProperties(properties);
      if (env == null) {
        initCtx = new InitialContext();
      } else {
        initCtx = new InitialContext(env);
      }

      // 支持两种查找方式：带初始上下文或不带
      if (properties.containsKey(INITIAL_CONTEXT) && properties.containsKey(DATA_SOURCE)) {
        // 先查找初始上下文，再从中获取 DataSource
        Context ctx = (Context) initCtx.lookup(properties.getProperty(INITIAL_CONTEXT));
        dataSource = (DataSource) ctx.lookup(properties.getProperty(DATA_SOURCE));
      } else if (properties.containsKey(DATA_SOURCE)) {
        // 直接从初始上下文查找 DataSource
        dataSource = (DataSource) initCtx.lookup(properties.getProperty(DATA_SOURCE));
      }

    } catch (NamingException e) {
      throw new DataSourceException("There was an error configuring JndiDataSourceTransactionPool. Cause: " + e, e);
    }
  }

  /**
   * 获取已配置的 JNDI DataSource 实例。
   *
   * @return JNDI DataSource
   */
  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  /**
   * 从全部属性中提取以 env. 前缀开头的属性，作为 JNDI 上下文环境变量。
   *
   * @param allProps 全部配置属性
   * @return 提取出的环境变量属性集，若无则返回 null
   */
  private static Properties getEnvProperties(Properties allProps) {
    final String PREFIX = ENV_PREFIX;
    Properties contextProperties = null;
    for (Entry<Object, Object> entry : allProps.entrySet()) {
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();
      // 过滤出以 env. 前缀开头的属性
      if (key.startsWith(PREFIX)) {
        if (contextProperties == null) {
          contextProperties = new Properties();
        }
        // 去掉前缀后存入环境变量
        contextProperties.put(key.substring(PREFIX.length()), value);
      }
    }
    return contextProperties;
  }

}
