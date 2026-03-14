/**
 *    Copyright 2009-2016 the original author or authors.
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
 * JNDI 数据源工厂，从应用服务器提供的 JNDI 上下文获取 DataSource 实例。
 *
 * @author Clinton Begin
 */
public class JndiDataSourceFactory implements DataSourceFactory {

  /** JNDI 初始上下文的环境配置属性名 */
  public static final String INITIAL_CONTEXT = "initial_context";
  /** JNDI 数据源名称属性名 */
  public static final String DATA_SOURCE = "data_source";
  /** 环境属性前缀，用于区分 JNDI 环境属性与 MyBatis 自身配置 */
  public static final String ENV_PREFIX = "env.";

  /** 缓存从 JNDI 获取的 DataSource 实例 */
  private DataSource dataSource;

  /**
   * 根据配置属性初始化 JNDI 上下文并获取 DataSource。
   *
   * @param properties 包含 JNDI 配置的属性集，应包含 data_source 名称，可选 initial_context 和 env.* 前缀的属性
   */
  @Override
  public void setProperties(Properties properties) {
    try {
      InitialContext initCtx;
      // 提取 JNDI 环境属性（以 env. 开头的配置项）
      Properties env = getEnvProperties(properties);
      // 根据是否有环境属性创建不同的 InitialContext
      if (env == null) {
        initCtx = new InitialContext();
      } else {
        initCtx = new InitialContext(env);
      }

      // 优先使用 initial_context + data_source 的组合查找方式
      if (properties.containsKey(INITIAL_CONTEXT)
          && properties.containsKey(DATA_SOURCE)) {
        Context ctx = (Context) initCtx.lookup(properties.getProperty(INITIAL_CONTEXT));
        dataSource = (DataSource) ctx.lookup(properties.getProperty(DATA_SOURCE));
      } else if (properties.containsKey(DATA_SOURCE)) {
        // 直接从根上下文查找 DataSource
        dataSource = (DataSource) initCtx.lookup(properties.getProperty(DATA_SOURCE));
      }

    } catch (NamingException e) {
      throw new DataSourceException("There was an error configuring JndiDataSourceTransactionPool. Cause: " + e, e);
    }
  }

  /**
   * 获取通过 JNDI 配置的 DataSource 实例。
   *
   * @return 从 JNDI 上下文获取的 DataSource
   */
  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  /**
   * 从配置属性中提取 JNDI 环境属性。
   * 将以 "env." 前缀开头的属性转换为 JNDI 上下文环境属性。
   *
   * @param allProps 完整的配置属性集
   * @return 提取出的 JNDI 环境属性，如果不存在则返回 null
   */
  private static Properties getEnvProperties(Properties allProps) {
    final String PREFIX = ENV_PREFIX;
    Properties contextProperties = null;
    // 遍历所有属性，筛选出以 env. 开头的配置项
    for (Entry<Object, Object> entry : allProps.entrySet()) {
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();
      if (key.startsWith(PREFIX)) {
        if (contextProperties == null) {
          contextProperties = new Properties();
        }
        // 移除前缀后存入环境属性
        contextProperties.put(key.substring(PREFIX.length()), value);
      }
    }
    return contextProperties;
  }

}
