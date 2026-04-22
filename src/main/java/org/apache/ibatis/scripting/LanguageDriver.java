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
package org.apache.ibatis.scripting;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;

/**
 * LanguageDriver 接口 - 定义创建 ParameterHandler 和 SqlSource 的方法，支持 XML 和注解两种配置方式
 */
public interface LanguageDriver {

  /**
   * 创建 ParameterHandler，将实际参数传递给 JDBC 语句
   *
   * @param mappedStatement 正在执行的 MappedStatement
   * @param parameterObject 输入参数对象（可为 null）
   * @param boundSql 动态语言执行后的 BoundSql
   * @return ParameterHandler 实例
   * @see DefaultParameterHandler
   */
  ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);

  /**
   * 从 Mapper XML 文件中读取语句并创建 SqlSource
   * 在启动阶段，从类或 XML 文件中读取 MappedStatement 时调用
   *
   * @param configuration MyBatis Configuration
   * @param script 从 XML 文件解析得到的 XNode
   * @param parameterType 从 Mapper 方法或 parameterType XML 属性中获取的输入参数类型，可为 null
   * @return SqlSource 实例
   */
  SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType);

  /**
   * 从注解中读取语句并创建 SqlSource
   * 在启动阶段，从类或 XML 文件中读取 MappedStatement 时调用
   *
   * @param configuration MyBatis Configuration
   * @param script 注解内容
   * @param parameterType 从 Mapper 方法或 parameterType XML 属性中获取的输入参数类型，可为 null
   * @return SqlSource 实例
   */
  SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);

}
