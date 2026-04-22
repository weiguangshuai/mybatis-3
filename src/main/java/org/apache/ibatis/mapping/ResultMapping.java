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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * 结果映射配置类，负责将数据库查询结果列映射到 Java 对象的属性上。
 *
 * @author Clinton Begin
 */
public class ResultMapping {

  /**
   * MyBatis 全局配置对象
   */
  private Configuration configuration;
  /**
   * 映射的 Java 对象属性名
   */
  private String property;
  /**
   * 对应的数据库列名
   */
  private String column;
  /**
   * Java 类型
   */
  private Class<?> javaType;
  /**
   * JDBC 类型
   */
  private JdbcType jdbcType;
  /**
   * 类型转换处理器
   */
  private TypeHandler<?> typeHandler;
  /**
   * 嵌套结果映射 ID，用于关联查询
   */
  private String nestedResultMapId;
  /**
   * 嵌套查询 ID，用于延迟加载
   */
  private String nestedQueryId;
  /**
   * 非空列集合，用于判断是否设置属性值
   */
  private Set<String> notNullColumns;
  /**
   * 列名前缀，用于嵌套查询
   */
  private String columnPrefix;
  /**
   * 结果标志列表，标识主键或构造函数参数等
   */
  private List<ResultFlag> flags;
  /**
   * 复合属性映射列表
   */
  private List<ResultMapping> composites;
  /**
   * 结果集名称，支持多结果集映射
   */
  private String resultSet;
  /**
   * 外键列名，用于关联查询
   */
  private String foreignColumn;
  /**
   * 是否延迟加载
   */
  private boolean lazy;

  ResultMapping() {
  }

  /**
   * ResultMapping 构建器，使用流式 API 构建结果映射实例
   */
  public static class Builder {
    private ResultMapping resultMapping = new ResultMapping();

    public Builder(Configuration configuration, String property, String column, TypeHandler<?> typeHandler) {
      this(configuration, property);
      resultMapping.column = column;
      resultMapping.typeHandler = typeHandler;
    }

    public Builder(Configuration configuration, String property, String column, Class<?> javaType) {
      this(configuration, property);
      resultMapping.column = column;
      resultMapping.javaType = javaType;
    }

    public Builder(Configuration configuration, String property) {
      resultMapping.configuration = configuration;
      resultMapping.property = property;
      resultMapping.flags = new ArrayList<>();
      resultMapping.composites = new ArrayList<>();
      resultMapping.lazy = configuration.isLazyLoadingEnabled();
    }

    public Builder javaType(Class<?> javaType) {
      resultMapping.javaType = javaType;
      return this;
    }

    public Builder jdbcType(JdbcType jdbcType) {
      resultMapping.jdbcType = jdbcType;
      return this;
    }

    public Builder nestedResultMapId(String nestedResultMapId) {
      resultMapping.nestedResultMapId = nestedResultMapId;
      return this;
    }

    public Builder nestedQueryId(String nestedQueryId) {
      resultMapping.nestedQueryId = nestedQueryId;
      return this;
    }

    public Builder resultSet(String resultSet) {
      resultMapping.resultSet = resultSet;
      return this;
    }

    public Builder foreignColumn(String foreignColumn) {
      resultMapping.foreignColumn = foreignColumn;
      return this;
    }

    public Builder notNullColumns(Set<String> notNullColumns) {
      resultMapping.notNullColumns = notNullColumns;
      return this;
    }

    public Builder columnPrefix(String columnPrefix) {
      resultMapping.columnPrefix = columnPrefix;
      return this;
    }

    public Builder flags(List<ResultFlag> flags) {
      resultMapping.flags = flags;
      return this;
    }

    public Builder typeHandler(TypeHandler<?> typeHandler) {
      resultMapping.typeHandler = typeHandler;
      return this;
    }

    public Builder composites(List<ResultMapping> composites) {
      resultMapping.composites = composites;
      return this;
    }

    public Builder lazy(boolean lazy) {
      resultMapping.lazy = lazy;
      return this;
    }

    /**
     * 构建 ResultMapping 实例，完成类型处理器解析和配置验证。
     *
     * @return 完整的 ResultMapping 对象
     */
    public ResultMapping build() {
      // 锁定集合，防止后续修改
      resultMapping.flags = Collections.unmodifiableList(resultMapping.flags);
      resultMapping.composites = Collections.unmodifiableList(resultMapping.composites);
      resolveTypeHandler();
      validate();
      return resultMapping;
    }

    /**
     * 验证 ResultMapping 配置的合法性，检查常见配置错误。
     */
    private void validate() {
      // 不能同时定义嵌套查询和嵌套结果映射
      if (resultMapping.nestedQueryId != null && resultMapping.nestedResultMapId != null) {
        throw new IllegalStateException("Cannot define both nestedQueryId and nestedResultMapId in property " + resultMapping.property);
      }
      // 必须有类型处理器
      if (resultMapping.nestedQueryId == null && resultMapping.nestedResultMapId == null && resultMapping.typeHandler == null) {
        throw new IllegalStateException("No typehandler found for property " + resultMapping.property);
      }
      // 非嵌套结果映射必须指定列名
      if (resultMapping.nestedResultMapId == null && resultMapping.column == null && resultMapping.composites.isEmpty()) {
        throw new IllegalStateException("Mapping is missing column attribute for property " + resultMapping.property);
      }
      // 如果指定了结果集，外键列数量必须与列数量一致
      if (resultMapping.getResultSet() != null) {
        int numColumns = 0;
        if (resultMapping.column != null) {
          numColumns = resultMapping.column.split(",").length;
        }
        int numForeignColumns = 0;
        if (resultMapping.foreignColumn != null) {
          numForeignColumns = resultMapping.foreignColumn.split(",").length;
        }
        if (numColumns != numForeignColumns) {
          throw new IllegalStateException("There should be the same number of columns and foreignColumns in property " + resultMapping.property);
        }
      }
    }

    /**
     * 根据 Java 类型和 JDBC 类型解析对应的类型处理器。
     */
    private void resolveTypeHandler() {
      // 未指定类型处理器时，根据 Java 类型自动匹配
      if (resultMapping.typeHandler == null && resultMapping.javaType != null) {
        Configuration configuration = resultMapping.configuration;
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        resultMapping.typeHandler = typeHandlerRegistry.getTypeHandler(resultMapping.javaType, resultMapping.jdbcType);
      }
    }

    public Builder column(String column) {
      resultMapping.column = column;
      return this;
    }
  }

  /**
   * 获取映射的 Java 属性名。
   */
  public String getProperty() {
    return property;
  }

  /**
   * 获取对应的数据库列名。
   */
  public String getColumn() {
    return column;
  }

  /**
   * 获取 Java 类型。
   */
  public Class<?> getJavaType() {
    return javaType;
  }

  /**
   * 获取 JDBC 类型。
   */
  public JdbcType getJdbcType() {
    return jdbcType;
  }

  /**
   * 获取类型转换处理器。
   */
  public TypeHandler<?> getTypeHandler() {
    return typeHandler;
  }

  /**
   * 获取嵌套结果映射 ID。
   */
  public String getNestedResultMapId() {
    return nestedResultMapId;
  }

  /**
   * 获取嵌套查询 ID。
   */
  public String getNestedQueryId() {
    return nestedQueryId;
  }

  /**
   * 获取非空列集合。
   */
  public Set<String> getNotNullColumns() {
    return notNullColumns;
  }

  /**
   * 获取列名前缀。
   */
  public String getColumnPrefix() {
    return columnPrefix;
  }

  /**
   * 获取结果标志列表。
   */
  public List<ResultFlag> getFlags() {
    return flags;
  }

  /**
   * 获取复合属性映射列表。
   */
  public List<ResultMapping> getComposites() {
    return composites;
  }

  /**
   * 判断是否为复合结果映射。
   */
  public boolean isCompositeResult() {
    return this.composites != null && !this.composites.isEmpty();
  }

  /**
   * 获取结果集名称。
   */
  public String getResultSet() {
    return this.resultSet;
  }

  /**
   * 获取外键列名。
   */
  public String getForeignColumn() {
    return foreignColumn;
  }

  /**
   * 设置外键列名。
   *
   * @param foreignColumn 外键列名
   */
  public void setForeignColumn(String foreignColumn) {
    this.foreignColumn = foreignColumn;
  }

  /**
   * 判断是否延迟加载。
   */
  public boolean isLazy() {
    return lazy;
  }

  /**
   * 设置是否延迟加载。
   *
   * @param lazy 是否延迟加载
   */
  public void setLazy(boolean lazy) {
    this.lazy = lazy;
  }

  /**
   * 判断是否为简单映射（无嵌套结果或查询）。
   */
  public boolean isSimple() {
    return this.nestedResultMapId == null && this.nestedQueryId == null && this.resultSet == null;
  }

  /**
   * 基于属性名比较两个 ResultMapping 是否相等。
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ResultMapping that = (ResultMapping) o;

    return property != null && property.equals(that.property);
  }

  /**
   * 基于属性名或列名生成哈希码。
   */
  @Override
  public int hashCode() {
    if (property != null) {
      return property.hashCode();
    } else if (column != null) {
      return column.hashCode();
    } else {
      return 0;
    }
  }

  /**
   * 返回 ResultMapping 的字符串表示。
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ResultMapping{");
    // 配置对象无有用的 toString 方法，故省略
    sb.append("property='").append(property).append('\'');
    sb.append(", column='").append(column).append('\'');
    sb.append(", javaType=").append(javaType);
    sb.append(", jdbcType=").append(jdbcType);
    // 类型处理器无有用的 toString 方法，故省略
    sb.append(", nestedResultMapId='").append(nestedResultMapId).append('\'');
    sb.append(", nestedQueryId='").append(nestedQueryId).append('\'');
    sb.append(", notNullColumns=").append(notNullColumns);
    sb.append(", columnPrefix='").append(columnPrefix).append('\'');
    sb.append(", flags=").append(flags);
    sb.append(", composites=").append(composites);
    sb.append(", resultSet='").append(resultSet).append('\'');
    sb.append(", foreignColumn='").append(foreignColumn).append('\'');
    sb.append(", lazy=").append(lazy);
    sb.append('}');
    return sb.toString();
  }

}
