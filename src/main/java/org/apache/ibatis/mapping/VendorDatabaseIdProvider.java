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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * Vendor DatabaseId provider.
 *
 * It returns database product name as a databaseId.
 * If the user provides a properties it uses it to translate database product name
 * key="Microsoft SQL Server", value="ms" will return "ms".
 * It can return null, if no database product name or
 * a properties was specified and no translation was found.
 *
 * @author Eduardo Macarron
 */
public class VendorDatabaseIdProvider implements DatabaseIdProvider {

  /** 用户配置的数据库产品名称映射表，用于将数据库产品名转换为简写 ID */
  private Properties properties;

  /**
   * 获取数据库产品名称对应的数据库 ID。
   *
   * @param dataSource 数据源，不能为空
   * @return 数据库 ID，若获取失败则返回 null
   */
  @Override
  public String getDatabaseId(DataSource dataSource) {
    if (dataSource == null) {
      throw new NullPointerException("dataSource cannot be null");
    }
    try {
      return getDatabaseName(dataSource);
    } catch (Exception e) {
      LogHolder.log.error("Could not get a databaseId from dataSource", e);
    }
    return null;
  }

  /**
   * 设置数据库产品名称到 ID 的映射配置。
   *
   * @param p 映射属性，key 为数据库产品名，value 为简写 ID
   */
  @Override
  public void setProperties(Properties p) {
    this.properties = p;
  }

  /**
   * 获取数据库名称或映射后的 ID。
   * 若配置了 properties，则尝试将数据库产品名映射为简写 ID。
   *
   * @param dataSource 数据源
   * @return 数据库名称或映射后的 ID，若无匹配则返回 null
   * @throws SQLException 获取数据库元数据失败时抛出
   */
  private String getDatabaseName(DataSource dataSource) throws SQLException {
    String productName = getDatabaseProductName(dataSource);
    // 若配置了映射表，则查找匹配项
    if (this.properties != null) {
      for (Map.Entry<Object, Object> property : properties.entrySet()) {
        if (productName.contains((String) property.getKey())) {
          return (String) property.getValue();
        }
      }
      // 无匹配，返回 null
      return null;
    }
    return productName;
  }

  /**
   * 从数据源获取数据库产品名称。
   *
   * @param dataSource 数据源
   * @return 数据库产品名称（如 "MySQL"、"Oracle" 等）
   * @throws SQLException 获取数据库连接或元数据失败时抛出
   */
  private String getDatabaseProductName(DataSource dataSource) throws SQLException {
    try (Connection con = dataSource.getConnection()) {
      DatabaseMetaData metaData = con.getMetaData();
      return metaData.getDatabaseProductName();
    }

  }

  /** 日志持有类，用于延迟加载日志实例 */
  private static class LogHolder {
    private static final Log log = LogFactory.getLog(VendorDatabaseIdProvider.class);
  }

}
