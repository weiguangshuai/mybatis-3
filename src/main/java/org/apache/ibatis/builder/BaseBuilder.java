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
package org.apache.ibatis.builder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * 构建器基类，提供类型解析、别名解析和实例创建等基础功能。
 *
 * @author Clinton Begin
 */
public abstract class BaseBuilder {
  /** MyBatis 全局配置对象 */
  protected final Configuration configuration;
  /** 类型别名注册表，用于解析类型别名 */
  protected final TypeAliasRegistry typeAliasRegistry;
  /** 类型处理器注册表，用于获取类型处理器 */
  protected final TypeHandlerRegistry typeHandlerRegistry;

  /**
   * 构造方法，初始化配置及类型注册表。
   *
   * @param configuration MyBatis 全局配置对象
   */
  public BaseBuilder(Configuration configuration) {
    this.configuration = configuration;
    this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
    this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
  }

  /**
   * 获取全局配置对象。
   *
   * @return MyBatis 配置对象
   */
  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * 解析正则表达式字符串为 Pattern 对象。
   *
   * @param regex       正则表达式字符串，为空时使用默认值
   * @param defaultValue 默认正则表达式
   * @return 编译后的 Pattern 对象
   */
  protected Pattern parseExpression(String regex, String defaultValue) {
    return Pattern.compile(regex == null ? defaultValue : regex);
  }

  /**
   * 将字符串转换为 Boolean 值。
   *
   * @param value       字符串值
   * @param defaultValue 默认值
   * @return 转换后的 Boolean 值
   */
  protected Boolean booleanValueOf(String value, Boolean defaultValue) {
    return value == null ? defaultValue : Boolean.valueOf(value);
  }

  /**
   * 将字符串转换为 Integer 值。
   *
   * @param value       字符串值
   * @param defaultValue 默认值
   * @return 转换后的 Integer 值
   */
  protected Integer integerValueOf(String value, Integer defaultValue) {
    return value == null ? defaultValue : Integer.valueOf(value);
  }

  /**
   * 将逗号分隔的字符串转换为 Set 集合。
   *
   * @param value       逗号分隔的字符串
   * @param defaultValue 默认值
   * @return 字符串集合
   */
  protected Set<String> stringSetValueOf(String value, String defaultValue) {
    value = value == null ? defaultValue : value;
    return new HashSet<>(Arrays.asList(value.split(",")));
  }

  /**
   * 根据别名解析 JDBC 类型。
   *
   * @param alias JDBC 类型别名
   * @return JdbcType 枚举值，无法解析时返回 null
   */
  protected JdbcType resolveJdbcType(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return JdbcType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving JdbcType. Cause: " + e, e);
    }
  }

  /**
   * 根据别名解析 ResultSet 类型。
   *
   * @param alias ResultSet 类型别名
   * @return ResultSetType 枚举值，无法解析时返回 null
   */
  protected ResultSetType resolveResultSetType(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return ResultSetType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ResultSetType. Cause: " + e, e);
    }
  }

  /**
   * 根据别名解析参数模式。
   *
   * @param alias 参数模式别名
   * @return ParameterMode 枚举值，无法解析时返回 null
   */
  protected ParameterMode resolveParameterMode(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return ParameterMode.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
    }
  }

  /**
   * 根据类型别名创建实例对象。
   *
   * @param alias 类型别名
   * @return 创建的实例对象，无法创建时返回 null
   */
  protected Object createInstance(String alias) {
    Class<?> clazz = resolveClass(alias);
    if (clazz == null) {
      return null;
    }
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new BuilderException("Error creating instance. Cause: " + e, e);
    }
  }

  /**
   * 根据别名解析对应的 Class 对象。
   *
   * @param alias 类型别名
   * @param <T>  类型参数
   * @return 解析得到的 Class 对象，无法解析时返回 null
   */
  protected <T> Class<? extends T> resolveClass(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return resolveAlias(alias);
    } catch (Exception e) {
      throw new BuilderException("Error resolving class. Cause: " + e, e);
    }
  }

  /**
   * 根据别名解析类型处理器。
   *
   * @param javaType           Java 类型
   * @param typeHandlerAlias   类型处理器别名
   * @return 解析得到的 TypeHandler，无法解析时返回 null
   */
  protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, String typeHandlerAlias) {
    if (typeHandlerAlias == null) {
      return null;
    }
    Class<?> type = resolveClass(typeHandlerAlias);
    // 验证该类实现了 TypeHandler 接口
    if (type != null && !TypeHandler.class.isAssignableFrom(type)) {
      throw new BuilderException("Type " + type.getName() + " is not a valid TypeHandler because it does not implement TypeHandler interface");
    }
    @SuppressWarnings("unchecked") // already verified it is a TypeHandler
    Class<? extends TypeHandler<?>> typeHandlerType = (Class<? extends TypeHandler<?>>) type;
    return resolveTypeHandler(javaType, typeHandlerType);
  }

  /**
   * 根据 Class 对象解析类型处理器。
   *
   * @param javaType         Java 类型
   * @param typeHandlerType  类型处理器 Class
   * @return 解析得到的 TypeHandler，无法解析时返回 null
   */
  protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, Class<? extends TypeHandler<?>> typeHandlerType) {
    if (typeHandlerType == null) {
      return null;
    }
    // javaType ignored for injected handlers see issue #746 for full detail
    TypeHandler<?> handler = typeHandlerRegistry.getMappingTypeHandler(typeHandlerType);
    if (handler == null) {
      // 注册表中不存在，则创建新实例
      handler = typeHandlerRegistry.getInstance(javaType, typeHandlerType);
    }
    return handler;
  }

  /**
   * 根据别名解析类型别名对应的 Class。
   *
   * @param alias 类型别名
   * @param <T>  类型参数
   * @return 解析得到的 Class 对象
   */
  protected <T> Class<? extends T> resolveAlias(String alias) {
    return typeAliasRegistry.resolveAlias(alias);
  }
}
