/*
 *    Copyright 2009-2026 the original author or authors.
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
package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

/**
 * SQL 源码构建器，负责解析包含 #{} 占位符的 SQL 语句并构建 ParameterMapping。
 *
 * @author Clinton Begin
 */
public class SqlSourceBuilder extends BaseBuilder {

  /** SQL 参数映射的有效属性列表 */
  private static final String PARAMETER_PROPERTIES = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

  /**
   * 创建 SqlSourceBuilder 实例。
   *
   * @param configuration MyBatis 配置对象
   */
  public SqlSourceBuilder(Configuration configuration) {
    super(configuration);
  }

  /**
   * 解析原始 SQL，将 #{} 占位符转换为 ? 并收集参数映射信息。
   *
   * @param originalSql       包含 #{} 占位符的原始 SQL
   * @param parameterType     参数对象的类型
   * @param additionalParameters 额外的参数映射
   * @return 解析后的 StaticSqlSource 对象
   */
  public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
    ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
    GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
    String sql;
    // 根据配置决定是否移除多余空白字符
    if (configuration.isShrinkWhitespacesInSql()) {
      sql = parser.parse(removeExtraWhitespaces(originalSql));
    } else {
      sql = parser.parse(originalSql);
    }
    return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
  }

  /**
   * 移除 SQL 语句中的多余空白字符，只保留单词间的单个空格。
   *
   * @param original 原始 SQL 字符串
   * @return 移除多余空白后的 SQL
   */
  public static String removeExtraWhitespaces(String original) {
    StringTokenizer tokenizer = new StringTokenizer(original);
    StringBuilder builder = new StringBuilder();
    boolean hasMoreTokens = tokenizer.hasMoreTokens();
    while (hasMoreTokens) {
      builder.append(tokenizer.nextToken());
      hasMoreTokens = tokenizer.hasMoreTokens();
      // 非最后一个 token 时添加空格分隔
      if (hasMoreTokens) {
        builder.append(' ');
      }
    }
    return builder.toString();
  }

  /**
   * 参数映射令牌处理器，负责解析 #{} 中的参数信息并构建 ParameterMapping。
   */
  private static class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {

    /** 解析出的参数映射列表 */
    private final List<ParameterMapping> parameterMappings = new ArrayList<>();
    /** 参数对象的类型 */
    private final Class<?> parameterType;
    /** 额外参数的元对象 */
    private final MetaObject metaParameters;

    /**
     * 创建参数映射令牌处理器。
     *
     * @param configuration      MyBatis 配置对象
     * @param parameterType      参数类型
     * @param additionalParameters 额外参数映射
     */
    public ParameterMappingTokenHandler(Configuration configuration, Class<?> parameterType, Map<String, Object> additionalParameters) {
      super(configuration);
      this.parameterType = parameterType;
      this.metaParameters = configuration.newMetaObject(additionalParameters);
    }

    /**
     * 获取解析出的参数映射列表。
     *
     * @return 参数映射列表
     */
    public List<ParameterMapping> getParameterMappings() {
      return parameterMappings;
    }

    /**
     * 处理 #{} 中的令牌内容，构建参数映射并返回 ? 占位符。
     *
     * @param content #{} 中的内容，如 "id" 或 "id, javaType=int"
     * @return SQL 中的 ? 占位符
     */
    @Override
    public String handleToken(String content) {
      parameterMappings.add(buildParameterMapping(content));
      return "?";
    }

    /**
     * 根据 #{} 中的内容构建参数映射对象。
     *
     * @param content #{} 中的参数内容
     * @return 参数映射对象
     */
    private ParameterMapping buildParameterMapping(String content) {
      Map<String, String> propertiesMap = parseParameterMapping(content);
      String property = propertiesMap.get("property");
      Class<?> propertyType;
      // 优先从额外参数中获取类型
      if (metaParameters.hasGetter(property)) {
        propertyType = metaParameters.getGetterType(property);
      } else if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
        // 参数类型有对应的 TypeHandler，直接使用
        propertyType = parameterType;
      } else if (JdbcType.CURSOR.name().equals(propertiesMap.get("jdbcType"))) {
        // JDBC 类型为 CURSOR 时，返回结果集类型
        propertyType = java.sql.ResultSet.class;
      } else if (property == null || Map.class.isAssignableFrom(parameterType)) {
        // 属性为空或参数为 Map 类型时使用 Object
        propertyType = Object.class;
      } else {
        // 从参数类的 getter 方法推断类型
        MetaClass metaClass = MetaClass.forClass(parameterType, configuration.getReflectorFactory());
        if (metaClass.hasGetter(property)) {
          propertyType = metaClass.getGetterType(property);
        } else {
          propertyType = Object.class;
        }
      }
      ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
      Class<?> javaType = propertyType;
      String typeHandlerAlias = null;
      // 遍历所有属性配置，构建 ParameterMapping
      for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
        String name = entry.getKey();
        String value = entry.getValue();
        if ("javaType".equals(name)) {
          javaType = resolveClass(value);
          builder.javaType(javaType);
        } else if ("jdbcType".equals(name)) {
          builder.jdbcType(resolveJdbcType(value));
        } else if ("mode".equals(name)) {
          builder.mode(resolveParameterMode(value));
        } else if ("numericScale".equals(name)) {
          builder.numericScale(Integer.valueOf(value));
        } else if ("resultMap".equals(name)) {
          builder.resultMapId(value);
        } else if ("typeHandler".equals(name)) {
          typeHandlerAlias = value;
        } else if ("jdbcTypeName".equals(name)) {
          builder.jdbcTypeName(value);
        } else if ("property".equals(name)) {
          // 属性名已在前面处理
        } else if ("expression".equals(name)) {
          // 暂不支持表达式参数
          throw new BuilderException("Expression based parameters are not supported yet");
        } else {
          throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content + "}.  Valid properties are " + PARAMETER_PROPERTIES);
        }
      }
      // 处理 TypeHandler 别名
      if (typeHandlerAlias != null) {
        builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
      }
      return builder.build();
    }

    /**
     * 解析参数映射内容为属性键值对。
     *
     * @param content #{} 中的参数内容
     * @return 属性名到属性值的映射
     */
    private Map<String, String> parseParameterMapping(String content) {
      try {
        return new ParameterExpression(content);
      } catch (BuilderException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new BuilderException("Parsing error was found in mapping #{" + content + "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
      }
    }
  }

}
