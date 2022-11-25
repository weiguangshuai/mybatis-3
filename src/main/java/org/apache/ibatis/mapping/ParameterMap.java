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

import java.util.Collections;
import java.util.List;

import org.apache.ibatis.session.Configuration;

/**
 * 封装 SQL 参数映射配置，包含参数类型和参数列表信息。
 *
 * @author Clinton Begin
 */
public class ParameterMap {

  /** ParameterMap 的唯一标识符，用于关联 SQL 语句 */
  private String id;
  /** 参数对应的 Java 类型 */
  private Class<?> type;
  /** 参数映射列表，描述每个参数的位置和属性 */
  private List<ParameterMapping> parameterMappings;

  private ParameterMap() {
  }

  public static class Builder {
    private ParameterMap parameterMap = new ParameterMap();

    /**
     * 创建 ParameterMap 构建器。
     *
     * @param configuration MyBatis 配置对象
     * @param id ParameterMap 唯一标识
     * @param type 参数对应的 Java 类型
     * @param parameterMappings 参数映射列表
     */
    public Builder(Configuration configuration, String id, Class<?> type, List<ParameterMapping> parameterMappings) {
      parameterMap.id = id;
      parameterMap.type = type;
      parameterMap.parameterMappings = parameterMappings;
    }

    /**
     * 获取参数类型。
     *
     * @return 参数的 Java 类型
     */
    public Class<?> type() {
      return parameterMap.type;
    }

    /**
     * 构建不可变的 ParameterMap 对象。
     *
     * @return 配置完成的 ParameterMap 实例
     */
    public ParameterMap build() {
      // 将参数列表设为只读，防止后续修改
      parameterMap.parameterMappings = Collections.unmodifiableList(parameterMap.parameterMappings);
      return parameterMap;
    }
  }

  /**
   * 获取 ParameterMap 的唯一标识符。
   *
   * @return ParameterMap ID
   */
  public String getId() {
    return id;
  }

  /**
   * 获取参数对应的 Java 类型。
   *
   * @return 参数类型
   */
  public Class<?> getType() {
    return type;
  }

  /**
   * 获取参数映射列表。
   *
   * @return 参数映射列表（只读）
   */
  public List<ParameterMapping> getParameterMappings() {
    return parameterMappings;
  }

}
