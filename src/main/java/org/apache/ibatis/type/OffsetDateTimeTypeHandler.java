/**
 *    Copyright 2009-2017 the original author or authors.
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
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.apache.ibatis.lang.UsesJava8;

/**
 * {@link OffsetDateTime} 的类型处理器。
 * 负责在 JDBC 的 {@link Timestamp} 与 Java 8 时间类型之间做双向转换。
 *
 * @since 3.4.5
 * @author Tomas Rohovsky
 */
@UsesJava8
public class OffsetDateTimeTypeHandler extends BaseTypeHandler<OffsetDateTime> {

  /**
   * 将非空 {@link OffsetDateTime} 参数写入预编译语句。
   * 写入时先转换为 UTC 瞬时点，再交由 {@link Timestamp} 表示。
   *
   * @param ps 预编译语句
   * @param i 参数索引（从 1 开始）
   * @param parameter 待写入的时间值（非空）
   * @param jdbcType JDBC 类型（当前实现未直接使用）
   * @throws SQLException JDBC 写入失败时抛出
   */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, OffsetDateTime parameter, JdbcType jdbcType)
          throws SQLException {
    ps.setTimestamp(i, Timestamp.from(parameter.toInstant()));
  }

  /**
   * 按列名读取可空时间值。
   *
   * @param rs 结果集
   * @param columnName 列名
   * @return 转换后的 {@link OffsetDateTime}；当数据库值为 null 时返回 null
   * @throws SQLException JDBC 读取失败时抛出
   */
  @Override
  public OffsetDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnName);
    return getOffsetDateTime(timestamp);
  }

  /**
   * 按列索引读取可空时间值。
   *
   * @param rs 结果集
   * @param columnIndex 列索引（从 1 开始）
   * @return 转换后的 {@link OffsetDateTime}；当数据库值为 null 时返回 null
   * @throws SQLException JDBC 读取失败时抛出
   */
  @Override
  public OffsetDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnIndex);
    return getOffsetDateTime(timestamp);
  }

  /**
   * 从存储过程返回参数中按列索引读取可空时间值。
   *
   * @param cs 可调用语句
   * @param columnIndex 列索引（从 1 开始）
   * @return 转换后的 {@link OffsetDateTime}；当数据库值为 null 时返回 null
   * @throws SQLException JDBC 读取失败时抛出
   */
  @Override
  public OffsetDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    Timestamp timestamp = cs.getTimestamp(columnIndex);
    return getOffsetDateTime(timestamp);
  }

  /**
   * 将 JDBC 时间戳转换为 {@link OffsetDateTime}。
   *
   * @param timestamp JDBC 时间戳
   * @return 转换后的时间；当入参为 null 时返回 null
   */
  private static OffsetDateTime getOffsetDateTime(Timestamp timestamp) {
    if (timestamp != null) {
      // 读取时使用系统默认时区补齐偏移信息，保持与历史行为兼容。
      return OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
    }
    return null;
  }
}
