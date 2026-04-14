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
package org.apache.ibatis.builder.annotation;

import java.lang.reflect.Method;

/**
 * 负责解析 Mapper 接口中单个方法的注解并构建对应的 SQL 语句。
 *
 * @author Eduardo Macarron
 */
public class MethodResolver {
  /** 用于解析方法注解的构建器 */
  private final MapperAnnotationBuilder annotationBuilder;
  /** 要解析的 Mapper 方法 */
  private final Method method;

  /**
   * 构造方法。
   *
   * @param annotationBuilder 用于解析方法注解的构建器
   * @param method 要解析的 Mapper 方法
   */
  public MethodResolver(MapperAnnotationBuilder annotationBuilder, Method method) {
    this.annotationBuilder = annotationBuilder;
    this.method = method;
  }

  /**
   * 解析方法上的注解并生成对应的 SQL 语句。
   */
  public void resolve() {
    annotationBuilder.parseStatement(method);
  }

}