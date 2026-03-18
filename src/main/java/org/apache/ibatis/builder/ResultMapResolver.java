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

import java.util.List;

import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;

/**
 * ResultMap 解析器，负责构建并注册 ResultMap 对象。
 *
 * @author Eduardo Macarron
 */
public class ResultMapResolver {
  /** Mapper 构建助手，用于执行 ResultMap 的添加操作 */
  private final MapperBuilderAssistant assistant;
  /** ResultMap 的唯一标识符 */
  private final String id;
  /** 映射的目标 Java 类型 */
  private final Class<?> type;
  /** 继承的父 ResultMap id */
  private final String extend;
  /** 鉴别器，用于多结果映射 */
  private final Discriminator discriminator;
  /** 结果属性与列的映射列表 */
  private final List<ResultMapping> resultMappings;
  /** 是否启用自动映射 */
  private final Boolean autoMapping;

  /**
   * 构造 ResultMap 解析器。
   *
   * @param assistant     Mapper 构建助手
   * @param id            ResultMap 标识符
   * @param type          映射的目标类型
   * @param extend        继承的父 ResultMap
   * @param discriminator 鉴别器
   * @param resultMappings 结果映射列表
   * @param autoMapping   自动映射开关
   */
  public ResultMapResolver(MapperBuilderAssistant assistant, String id, Class<?> type, String extend, Discriminator discriminator, List<ResultMapping> resultMappings, Boolean autoMapping) {
    this.assistant = assistant;
    this.id = id;
    this.type = type;
    this.extend = extend;
    this.discriminator = discriminator;
    this.resultMappings = resultMappings;
    this.autoMapping = autoMapping;
  }

  /**
   * 解析并添加 ResultMap 到 MapperBuilderAssistant。
   *
   * @return 构建完成的 ResultMap 对象
   */
  public ResultMap resolve() {
    // 委托给助手完成 ResultMap 的注册
    return assistant.addResultMap(this.id, this.type, this.extend, this.discriminator, this.resultMappings, this.autoMapping);
  }

}
