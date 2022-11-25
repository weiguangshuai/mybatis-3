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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.reflection.ParamNameUtil;
import org.apache.ibatis.session.Configuration;

/**
 * ResultMap 配置类，封装结果映射信息，将数据库查询结果映射到 Java 对象。
 *
 * @author Clinton Begin
 */
public class ResultMap {
  /** MyBatis 全局配置对象 */
  private Configuration configuration;

  /** ResultMap 的唯一标识，用于引用 */
  private String id;

  /** 映射的目标 Java 类型 */
  private Class<?> type;

  /** 所有结果映射的列表 */
  private List<ResultMapping> resultMappings;

  /** ID 字段的映射列表，用于对象标识 */
  private List<ResultMapping> idResultMappings;

  /** 构造函数映射列表，用于对象实例化 */
  private List<ResultMapping> constructorResultMappings;

  /** 普通属性映射列表 */
  private List<ResultMapping> propertyResultMappings;

  /** 已映射的数据库列名集合 */
  private Set<String> mappedColumns;

  /** 已映射的 Java 属性名集合 */
  private Set<String> mappedProperties;

  /** 鉴别器，处理继承映射 */
  private Discriminator discriminator;

  /** 是否包含嵌套的结果映射 */
  private boolean hasNestedResultMaps;

  /** 是否包含嵌套的查询 */
  private boolean hasNestedQueries;

  /** 是否启用自动映射，null 表示使用全局配置 */
  private Boolean autoMapping;

  private ResultMap() {
  }

  /**
   * ResultMap 构建器，负责创建 ResultMap 实例并初始化其配置。
   */
  public static class Builder {
    private static final Log log = LogFactory.getLog(Builder.class);

    private ResultMap resultMap = new ResultMap();

    public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings) {
      this(configuration, id, type, resultMappings, null);
    }

    /**
     * 构造 ResultMap 构建器。
     *
     * @param configuration  MyBatis 配置对象
     * @param id             ResultMap 标识
     * @param type           映射的目标类型
     * @param resultMappings 结果映射列表
     * @param autoMapping    自动映射配置，null 表示使用全局设置
     */
    public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings, Boolean autoMapping) {
      resultMap.configuration = configuration;
      resultMap.id = id;
      resultMap.type = type;
      resultMap.resultMappings = resultMappings;
      resultMap.autoMapping = autoMapping;
    }

    /**
     * 设置鉴别器。
     *
     * @param discriminator 鉴别器实例
     * @return 当前 Builder 实例
     */
    public Builder discriminator(Discriminator discriminator) {
      resultMap.discriminator = discriminator;
      return this;
    }

    /**
     * 获取映射的目标类型。
     *
     * @return 目标 Java 类型
     */
    public Class<?> type() {
      return resultMap.type;
    }

    /**
     * 构建 ResultMap 实例，初始化所有映射集合并完成验证。
     *
     * @return 完整的 ResultMap 对象
     */
    public ResultMap build() {
      // 验证 id 必须存在
      if (resultMap.id == null) {
        throw new IllegalArgumentException("ResultMaps must have an id");
      }
      // 初始化各类型映射集合
      resultMap.mappedColumns = new HashSet<>();
      resultMap.mappedProperties = new HashSet<>();
      resultMap.idResultMappings = new ArrayList<>();
      resultMap.constructorResultMappings = new ArrayList<>();
      resultMap.propertyResultMappings = new ArrayList<>();
      final List<String> constructorArgNames = new ArrayList<>();
      // 遍历所有映射，进行分类和标记
      for (ResultMapping resultMapping : resultMap.resultMappings) {
        // 检查是否存在嵌套查询或嵌套结果映射
        resultMap.hasNestedQueries = resultMap.hasNestedQueries || resultMapping.getNestedQueryId() != null;
        resultMap.hasNestedResultMaps = resultMap.hasNestedResultMaps || (resultMapping.getNestedResultMapId() != null && resultMapping.getResultSet() == null);
        final String column = resultMapping.getColumn();
        if (column != null) {
          resultMap.mappedColumns.add(column.toUpperCase(Locale.ENGLISH));
        } else if (resultMapping.isCompositeResult()) {
          // 处理组合结果映射中的列
          for (ResultMapping compositeResultMapping : resultMapping.getComposites()) {
            final String compositeColumn = compositeResultMapping.getColumn();
            if (compositeColumn != null) {
              resultMap.mappedColumns.add(compositeColumn.toUpperCase(Locale.ENGLISH));
            }
          }
        }
        final String property = resultMapping.getProperty();
        if (property != null) {
          resultMap.mappedProperties.add(property);
        }
        // 根据标记区分构造函数映射和属性映射
        if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
          resultMap.constructorResultMappings.add(resultMapping);
          if (resultMapping.getProperty() != null) {
            constructorArgNames.add(resultMapping.getProperty());
          }
        } else {
          resultMap.propertyResultMappings.add(resultMapping);
        }
        // 收集 ID 映射
        if (resultMapping.getFlags().contains(ResultFlag.ID)) {
          resultMap.idResultMappings.add(resultMapping);
        }
      }
      // 若无显式 ID 映射，则使用所有映射作为 ID 映射
      if (resultMap.idResultMappings.isEmpty()) {
        resultMap.idResultMappings.addAll(resultMap.resultMappings);
      }
      // 验证并排序构造函数参数
      if (!constructorArgNames.isEmpty()) {
        final List<String> actualArgNames = argNamesOfMatchingConstructor(constructorArgNames);
        if (actualArgNames == null) {
          throw new BuilderException("Error in result map '" + resultMap.id
              + "'. Failed to find a constructor in '"
              + resultMap.getType().getName() + "' by arg names " + constructorArgNames
              + ". There might be more info in debug log.");
        }
        // 按构造函数参数顺序排序
        resultMap.constructorResultMappings.sort((o1, o2) -> {
          int paramIdx1 = actualArgNames.indexOf(o1.getProperty());
          int paramIdx2 = actualArgNames.indexOf(o2.getProperty());
          return paramIdx1 - paramIdx2;
        });
      }
      // 锁定集合，防止后续修改
      resultMap.resultMappings = Collections.unmodifiableList(resultMap.resultMappings);
      resultMap.idResultMappings = Collections.unmodifiableList(resultMap.idResultMappings);
      resultMap.constructorResultMappings = Collections.unmodifiableList(resultMap.constructorResultMappings);
      resultMap.propertyResultMappings = Collections.unmodifiableList(resultMap.propertyResultMappings);
      resultMap.mappedColumns = Collections.unmodifiableSet(resultMap.mappedColumns);
      return resultMap;
    }

    /**
     * 查找匹配的构造函数。
     *
     * @param constructorArgNames 构造函数参数名列表
     * @return 匹配构造函数的参数名列表，未找到返回 null
     */
    private List<String> argNamesOfMatchingConstructor(List<String> constructorArgNames) {
      Constructor<?>[] constructors = resultMap.type.getDeclaredConstructors();
      for (Constructor<?> constructor : constructors) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        if (constructorArgNames.size() == paramTypes.length) {
          List<String> paramNames = getArgNames(constructor);
          if (constructorArgNames.containsAll(paramNames)
              && argTypesMatch(constructorArgNames, paramTypes, paramNames)) {
            return paramNames;
          }
        }
      }
      return null;
    }

    /**
     * 验证构造函数参数类型是否匹配。
     *
     * @param constructorArgNames 构造函数参数名列表
     * @param paramTypes           构造函数的参数类型数组
     * @param paramNames           构造函数参数名列表
     * @return 类型匹配返回 true，否则返回 false
     */
    private boolean argTypesMatch(final List<String> constructorArgNames,
        Class<?>[] paramTypes, List<String> paramNames) {
      for (int i = 0; i < constructorArgNames.size(); i++) {
        Class<?> actualType = paramTypes[paramNames.indexOf(constructorArgNames.get(i))];
        Class<?> specifiedType = resultMap.constructorResultMappings.get(i).getJavaType();
        if (!actualType.equals(specifiedType)) {
          if (log.isDebugEnabled()) {
            log.debug("While building result map '" + resultMap.id
                + "', found a constructor with arg names " + constructorArgNames
                + ", but the type of '" + constructorArgNames.get(i)
                + "' did not match. Specified: [" + specifiedType.getName() + "] Declared: ["
                + actualType.getName() + "]");
          }
          return false;
        }
      }
      return true;
    }

    /**
     * 获取构造函数的参数名列表。
     *
     * @param constructor 构造函数对象
     * @return 参数名列表
     */
    private List<String> getArgNames(Constructor<?> constructor) {
      List<String> paramNames = new ArrayList<>();
      List<String> actualParamNames = null;
      final Annotation[][] paramAnnotations = constructor.getParameterAnnotations();
      int paramCount = paramAnnotations.length;
      for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
        String name = null;
        // 优先查找 @Param 注解指定的参数名
        for (Annotation annotation : paramAnnotations[paramIndex]) {
          if (annotation instanceof Param) {
            name = ((Param) annotation).value();
            break;
          }
        }
        // 若未指定参数名，尝试使用实际参数名
        if (name == null && resultMap.configuration.isUseActualParamName()) {
          if (actualParamNames == null) {
            actualParamNames = ParamNameUtil.getParamNames(constructor);
          }
          if (actualParamNames.size() > paramIndex) {
            name = actualParamNames.get(paramIndex);
          }
        }
        paramNames.add(name != null ? name : "arg" + paramIndex);
      }
      return paramNames;
    }
  }

  /**
   * 获取 ResultMap 的唯一标识。
   *
   * @return ResultMap ID
   */
  public String getId() {
    return id;
  }

  /**
   * 判断是否存在嵌套的结果映射。
   *
   * @return 存在返回 true
   */
  public boolean hasNestedResultMaps() {
    return hasNestedResultMaps;
  }

  /**
   * 判断是否存在嵌套的查询。
   *
   * @return 存在返回 true
   */
  public boolean hasNestedQueries() {
    return hasNestedQueries;
  }

  /**
   * 获取映射的目标类型。
   *
   * @return 目标 Java 类型
   */
  public Class<?> getType() {
    return type;
  }

  /**
   * 获取所有结果映射列表。
   *
   * @return 结果映射列表
   */
  public List<ResultMapping> getResultMappings() {
    return resultMappings;
  }

  /**
   * 获取构造函数结果映射列表。
   *
   * @return 构造函数映射列表
   */
  public List<ResultMapping> getConstructorResultMappings() {
    return constructorResultMappings;
  }

  /**
   * 获取属性结果映射列表。
   *
   * @return 属性映射列表
   */
  public List<ResultMapping> getPropertyResultMappings() {
    return propertyResultMappings;
  }

  /**
   * 获取 ID 结果映射列表。
   *
   * @return ID 映射列表
   */
  public List<ResultMapping> getIdResultMappings() {
    return idResultMappings;
  }

  /**
   * 获取已映射的数据库列名集合。
   *
   * @return 列名集合
   */
  public Set<String> getMappedColumns() {
    return mappedColumns;
  }

  /**
   * 获取已映射的 Java 属性名集合。
   *
   * @return 属性名集合
   */
  public Set<String> getMappedProperties() {
    return mappedProperties;
  }

  /**
   * 获取鉴别器实例。
   *
   * @return 鉴别器对象
   */
  public Discriminator getDiscriminator() {
    return discriminator;
  }

  /**
   * 强制启用嵌套结果映射。
   */
  public void forceNestedResultMaps() {
    hasNestedResultMaps = true;
  }

  /**
   * 获取自动映射配置。
   *
   * @return 自动映射配置，null 表示使用全局设置
   */
  public Boolean getAutoMapping() {
    return autoMapping;
  }

}
