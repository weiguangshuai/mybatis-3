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
/**
 * MyBatis 默认的 XML 动态 SQL 语言支持包。
 *
 * <p>该包包含解析和生成动态 SQL 的核心组件，支持 {@code <if>}、{@code <choose>}、{@code <foreach>}、
 * {@code <where>}、{@code <set>}、{@code <trim>}、{@code <bind>} 等 XML 标签，
 * 并提供了 OGNL 表达式引擎以支持运行时条件判断和动态内容构建。</p>
 *
 * <p>主要组件包括：</p>
 * <ul>
 *   <li>{@link org.apache.ibatis.scripting.xmltags.XMLLanguageDriver} - 默认 XML LanguageDriver</li>
 *   <li>{@link org.apache.ibatis.scripting.xmltags.XMLScriptBuilder} - XMLScriptBuilder</li>
 *   <li>{@link org.apache.ibatis.scripting.xmltags.OgnlCache} - OgnlCache</li>
 *   <li>{@link org.apache.ibatis.scripting.xmltags.DynamicSqlSource} - DynamicSqlSource</li>
 *   <li>{@link org.apache.ibatis.scripting.xmltags.RawSqlSource} - RawSqlSource</li>
 *   <li>{@link org.apache.ibatis.scripting.xmltags.SqlNode} 及其子类 - 各类 SqlNode</li>
 * </ul>
 */
package org.apache.ibatis.scripting.xmltags;
