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

import java.util.Properties;

import javax.sql.DataSource;

/**
 * 数据源工厂接口，用于创建和配置 DataSource 实例。
 *
 * @author Clinton Begin
 */
public interface DataSourceFactory {

  /**
   * 设置数据源配置属性。
   *
   * @param props 包含数据源配置键值对的属性对象
   */
  void setProperties(Properties props);

  /**
   * 获取已配置的数据源实例。
   *
   * @return 配置好的 DataSource 对象
   */
  DataSource getDataSource();

}
