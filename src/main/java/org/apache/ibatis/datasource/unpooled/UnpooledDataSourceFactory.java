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
package org.apache.ibatis.datasource.unpooled;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * 无池化数据源工厂，负责创建 {@link UnpooledDataSource} 实例。
 *
 * @author Clinton Begin
 */
public class UnpooledDataSourceFactory implements DataSourceFactory {

  /** 驱动属性前缀，用于识别需要传递给驱动的配置项 */
  private static final String DRIVER_PROPERTY_PREFIX = "driver.";
  /** 驱动属性前缀的长度，用于截取属性名 */
  private static final int DRIVER_PROPERTY_PREFIX_LENGTH = DRIVER_PROPERTY_PREFIX.length();

  /** 非池化数据源实例 */
  protected DataSource dataSource;

  /** 构造方法，初始化一个非池化数据源 */
  public UnpooledDataSourceFactory() {
    this.dataSource = new UnpooledDataSource();
  }

  /**
   * 设置数据源配置属性。
   * <p>
   * 支持两种属性格式：
   * <ul>
   *   <li>直接属性：如 {@code url}、{@code username} 等，会映射到数据源的 setter 方法</li>
   *   <li>驱动属性：以 {@code driver.} 前缀开头，会传递给底层驱动</li>
   * </ul>
   *
   * @param properties 数据源配置属性
   * @throws DataSourceException 如果存在未知的数据源属性
   */
  @Override
  public void setProperties(Properties properties) {
    // 存储驱动相关的配置属性
    Properties driverProperties = new Properties();
    // 通过 MetaObject 反射访问数据源的属性
    MetaObject metaDataSource = SystemMetaObject.forObject(dataSource);
    for (Object key : properties.keySet()) {
      String propertyName = (String) key;
      // 处理驱动属性（以 driver. 开头）
      if (propertyName.startsWith(DRIVER_PROPERTY_PREFIX)) {
        String value = properties.getProperty(propertyName);
        // 截取去掉前缀后的属性名
        driverProperties.setProperty(propertyName.substring(DRIVER_PROPERTY_PREFIX_LENGTH), value);
      } else if (metaDataSource.hasSetter(propertyName)) {
        // 处理数据源直接属性，进行类型转换后设置
        String value = (String) properties.get(propertyName);
        Object convertedValue = convertValue(metaDataSource, propertyName, value);
        metaDataSource.setValue(propertyName, convertedValue);
      } else {
        // 属性名既不是驱动属性，数据源也没有对应的 setter
        throw new DataSourceException("Unknown DataSource property: " + propertyName);
      }
    }
    // 将驱动属性设置到数据源
    if (driverProperties.size() > 0) {
      metaDataSource.setValue("driverProperties", driverProperties);
    }
  }

  /**
   * 获取已配置的数据源实例。
   *
   * @return 非池化的 JDBC 数据源
   */
  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  /**
   * 将字符串属性值转换为目标类型。
   * <p>
   * 支持 Integer、Long、Boolean 类型的自动转换，其他类型保持字符串原值。
   *
   * @param metaDataSource 数据源的元对象
   * @param propertyName   属性名
   * @param value          字符串类型的属性值
   * @return 转换后的值
   */
  private Object convertValue(MetaObject metaDataSource, String propertyName, String value) {
    Object convertedValue = value;
    // 获取属性目标类型
    Class<?> targetType = metaDataSource.getSetterType(propertyName);
    if (targetType == Integer.class || targetType == int.class) {
      convertedValue = Integer.valueOf(value);
    } else if (targetType == Long.class || targetType == long.class) {
      convertedValue = Long.valueOf(value);
    } else if (targetType == Boolean.class || targetType == boolean.class) {
      convertedValue = Boolean.valueOf(value);
    }
    return convertedValue;
  }

}
