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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.session.Configuration;

/**
 * 表示已解析的最终SQL语句及其参数绑定信息。
 * 包含处理完动态内容后的实际SQL字符串（含占位符?）以及对应的参数映射列表，
 * 同时支持动态语言产生的额外参数（如for循环、bind等）。
 *
 * @author Clinton Begin
 */
public class BoundSql {

  /** 解析后的SQL语句，包含?占位符 */
  private final String sql;
  /** 参数映射列表，按顺序对应SQL中的占位符 */
  private final List<ParameterMapping> parameterMappings;
  /** 用户传入的原始参数对象 */
  private final Object parameterObject;
  /** 动态SQL产生的额外参数集合（如foreach循环的循环变量） */
  private final Map<String, Object> additionalParameters;
  /** 额外参数的元对象，用于动态设置/获取参数值 */
  private final MetaObject metaParameters;

  /**
   * 构造BoundSql实例，初始化SQL语句和参数映射信息。
   *
   * @param configuration 全局配置对象
   * @param sql 解析后的SQL语句
   * @param parameterMappings 参数映射列表
   * @param parameterObject 用户传入的参数对象
   */
  public BoundSql(Configuration configuration, String sql, List<ParameterMapping> parameterMappings, Object parameterObject) {
    this.sql = sql;
    this.parameterMappings = parameterMappings;
    this.parameterObject = parameterObject;
    this.additionalParameters = new HashMap<>();
    this.metaParameters = configuration.newMetaObject(additionalParameters);
  }

  /**
   * 获取解析后的SQL语句。
   *
   * @return 包含占位符?的SQL字符串
   */
  public String getSql() {
    return sql;
  }

  /**
   * 获取参数映射列表。
   *
   * @return 按顺序排列的参数映射
   */
  public List<ParameterMapping> getParameterMappings() {
    return parameterMappings;
  }

  /**
   * 获取用户传入的原始参数对象。
   *
   * @return 参数对象
   */
  public Object getParameterObject() {
    return parameterObject;
  }

  /**
   * 判断是否存在指定的额外参数。
   *
   * @param name 参数名称，支持嵌套属性（如"user.name"）
   * @return 存在返回true，否则返回false
   */
  public boolean hasAdditionalParameter(String name) {
    String paramName = new PropertyTokenizer(name).getName();
    return additionalParameters.containsKey(paramName);
  }

  /**
   * 设置额外的参数值，用于动态SQL产生的参数。
   *
   * @param name 参数名称
   * @param value 参数值
   */
  public void setAdditionalParameter(String name, Object value) {
    metaParameters.setValue(name, value);
  }

  /**
   * 获取额外参数的值。
   *
   * @param name 参数名称，支持嵌套属性
   * @return 参数值，不存在返回null
   */
  public Object getAdditionalParameter(String name) {
    return metaParameters.getValue(name);
  }

  /**
   * 获取所有额外参数的映射。
   *
   * @return 额外参数集合
   */
  public Map<String, Object> getAdditionalParameters() {
    return additionalParameters;
  }
}
