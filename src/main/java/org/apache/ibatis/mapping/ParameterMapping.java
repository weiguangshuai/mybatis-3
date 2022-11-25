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

import java.sql.ResultSet;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * SQL 参数映射配置类，用于描述 SQL 语句中单个参数的属性信息。
 *
 * @author Clinton Begin
 */
public class ParameterMapping {

  /** MyBatis 全局配置对象 */
  private Configuration configuration;

  /** 参数属性名，对应映射语句中的参数占位符名称 */
  private String property;
  /** 参数模式：IN（输入）、OUT（输出）、INOUT（输入输出） */
  private ParameterMode mode;
  /** Java 类型，默认 Object.class */
  private Class<?> javaType = Object.class;
  /** JDBC 数据类型 */
  private JdbcType jdbcType;
  /** 数值精度，用于 DECIMAL/NUMBER 类型 */
  private Integer numericScale;
  /** 类型转换处理器，负责 Java 与 JDBC 类型间的相互转换 */
  private TypeHandler<?> typeHandler;
  /** 关联的结果映射 ID，用于 ResultSet 参数 */
  private String resultMapId;
  /** JDBC 类型名称，用于特殊类型声明 */
  private String jdbcTypeName;
  /** 表达式（当前未使用） */
  private String expression;

  private ParameterMapping() {
  }

  /** 参数映射构建器，用于创建 ParameterMapping 实例 */
  public static class Builder {
    private ParameterMapping parameterMapping = new ParameterMapping();

    /**
     * 构造方法，使用指定的 TypeHandler 创建构建器。
     *
     * @param configuration MyBatis 配置对象
     * @param property 参数属性名
     * @param typeHandler 类型转换处理器
     */
    public Builder(Configuration configuration, String property, TypeHandler<?> typeHandler) {
      parameterMapping.configuration = configuration;
      parameterMapping.property = property;
      parameterMapping.typeHandler = typeHandler;
      parameterMapping.mode = ParameterMode.IN;
    }

    /**
     * 构造方法，使用指定的 Java 类型创建构建器。
     *
     * @param configuration MyBatis 配置对象
     * @param property 参数属性名
     * @param javaType Java 类型
     */
    public Builder(Configuration configuration, String property, Class<?> javaType) {
      parameterMapping.configuration = configuration;
      parameterMapping.property = property;
      parameterMapping.javaType = javaType;
      parameterMapping.mode = ParameterMode.IN;
    }

    /**
     * 设置参数模式。
     *
     * @param mode 参数模式
     * @return 当前构建器
     */
    public Builder mode(ParameterMode mode) {
      parameterMapping.mode = mode;
      return this;
    }

    /**
     * 设置 Java 类型。
     *
     * @param javaType Java 类型
     * @return 当前构建器
     */
    public Builder javaType(Class<?> javaType) {
      parameterMapping.javaType = javaType;
      return this;
    }

    /**
     * 设置 JDBC 类型。
     *
     * @param jdbcType JDBC 类型
     * @return 当前构建器
     */
    public Builder jdbcType(JdbcType jdbcType) {
      parameterMapping.jdbcType = jdbcType;
      return this;
    }

    /**
     * 设置数值精度。
     *
     * @param numericScale 数值精度
     * @return 当前构建器
     */
    public Builder numericScale(Integer numericScale) {
      parameterMapping.numericScale = numericScale;
      return this;
    }

    /**
     * 设置结果映射 ID。
     *
     * @param resultMapId 结果映射 ID
     * @return 当前构建器
     */
    public Builder resultMapId(String resultMapId) {
      parameterMapping.resultMapId = resultMapId;
      return this;
    }

    /**
     * 设置类型转换处理器。
     *
     * @param typeHandler 类型转换处理器
     * @return 当前构建器
     */
    public Builder typeHandler(TypeHandler<?> typeHandler) {
      parameterMapping.typeHandler = typeHandler;
      return this;
    }

    /**
     * 设置 JDBC 类型名称。
     *
     * @param jdbcTypeName JDBC 类型名称
     * @return 当前构建器
     */
    public Builder jdbcTypeName(String jdbcTypeName) {
      parameterMapping.jdbcTypeName = jdbcTypeName;
      return this;
    }

    /**
     * 设置表达式。
     *
     * @param expression 表达式
     * @return 当前构建器
     */
    public Builder expression(String expression) {
      parameterMapping.expression = expression;
      return this;
    }

    /**
     * 构建 ParameterMapping 实例。
     * 先解析类型处理器，再进行校验，最后返回构建好的对象。
     *
     * @return 参数映射配置对象
     */
    public ParameterMapping build() {
      resolveTypeHandler();
      validate();
      return parameterMapping;
    }

    /**
     * 校验参数映射配置的完整性。
     * 检查 ResultSet 类型必须指定 resultMap，及其他类型必须有对应的 typeHandler。
     */
    private void validate() {
      // ResultSet 类型必须指定 resultMap
      if (ResultSet.class.equals(parameterMapping.javaType)) {
        if (parameterMapping.resultMapId == null) {
          throw new IllegalStateException("Missing resultmap in property '"
              + parameterMapping.property + "'.  "
              + "Parameters of type java.sql.ResultSet require a resultmap.");
        }
      } else {
        // 其他类型必须有对应的 typeHandler
        if (parameterMapping.typeHandler == null) {
          throw new IllegalStateException("Type handler was null on parameter mapping for property '"
            + parameterMapping.property + "'. It was either not specified and/or could not be found for the javaType ("
            + parameterMapping.javaType.getName() + ") : jdbcType (" + parameterMapping.jdbcType + ") combination.");
        }
      }
    }

    /**
     * 解析并设置类型转换处理器。
     * 当未显式指定 typeHandler 时，根据 javaType 和 jdbcType 从注册表中查找对应的处理器。
     */
    private void resolveTypeHandler() {
      // 仅当未设置 typeHandler 且已指定 javaType 时才进行解析
      if (parameterMapping.typeHandler == null && parameterMapping.javaType != null) {
        Configuration configuration = parameterMapping.configuration;
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        // 根据 Java 类型和 JDBC 类型查找对应的处理器
        parameterMapping.typeHandler = typeHandlerRegistry.getTypeHandler(parameterMapping.javaType, parameterMapping.jdbcType);
      }
    }

  }

  /**
   * 获取参数属性名。
   *
   * @return 参数属性名
   */
  public String getProperty() {
    return property;
  }

  /**
   * Used for handling output of callable statements.
   *
   * @return the mode
   */
  public ParameterMode getMode() {
    return mode;
  }

  /**
   * Used for handling output of callable statements.
   *
   * @return the java type
   */
  public Class<?> getJavaType() {
    return javaType;
  }

  /**
   * Used in the UnknownTypeHandler in case there is no handler for the property type.
   *
   * @return the jdbc type
   */
  public JdbcType getJdbcType() {
    return jdbcType;
  }

  /**
   * Used for handling output of callable statements.
   *
   * @return the numeric scale
   */
  public Integer getNumericScale() {
    return numericScale;
  }

  /**
   * Used when setting parameters to the PreparedStatement.
   *
   * @return the type handler
   */
  public TypeHandler<?> getTypeHandler() {
    return typeHandler;
  }

  /**
   * Used for handling output of callable statements.
   *
   * @return the result map id
   */
  public String getResultMapId() {
    return resultMapId;
  }

  /**
   * Used for handling output of callable statements.
   *
   * @return the jdbc type name
   */
  public String getJdbcTypeName() {
    return jdbcTypeName;
  }

  /**
   * Expression 'Not used'.
   *
   * @return the expression
   */
  public String getExpression() {
    return expression;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ParameterMapping{");
    //sb.append("configuration=").append(configuration); // configuration doesn't have a useful .toString()
    sb.append("property='").append(property).append('\'');
    sb.append(", mode=").append(mode);
    sb.append(", javaType=").append(javaType);
    sb.append(", jdbcType=").append(jdbcType);
    sb.append(", numericScale=").append(numericScale);
    //sb.append(", typeHandler=").append(typeHandler); // typeHandler also doesn't have a useful .toString()
    sb.append(", resultMapId='").append(resultMapId).append('\'');
    sb.append(", jdbcTypeName='").append(jdbcTypeName).append('\'');
    sb.append(", expression='").append(expression).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
