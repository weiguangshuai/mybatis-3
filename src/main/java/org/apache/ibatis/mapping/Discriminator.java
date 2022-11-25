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
import java.util.Map;

import org.apache.ibatis.session.Configuration;

/**
 * 鉴别器，用于在 MyBatis 映射中根据列值确定具体的子类映射结果。
 *
 * @author Clinton Begin
 */
public class Discriminator {

  /** 鉴别器对应的结果映射，通常为某个列的映射 */
  private ResultMapping resultMapping;
  /** 列值到结果映射 ID 的映射表，键为列值，值为对应的 resultMap ID */
  private Map<String, String> discriminatorMap;

  Discriminator() {
  }

  /** 鉴别器构建器，用于创建不可变的 Discriminator 实例 */
  public static class Builder {
    private Discriminator discriminator = new Discriminator();

    /**
     * 构建器构造函数，初始化鉴别器的结果映射和映射表。
     *
     * @param configuration 全局配置对象
     * @param resultMapping 鉴别器对应的结果映射
     * @param discriminatorMap 列值到 resultMap ID 的映射表
     */
    public Builder(Configuration configuration, ResultMapping resultMapping, Map<String, String> discriminatorMap) {
      discriminator.resultMapping = resultMapping;
      discriminator.discriminatorMap = discriminatorMap;
    }

    /**
     * 构建并返回不可变的 Discriminator 实例。
     * 构建前会验证 resultMapping 和 discriminatorMap 是否有效。
     *
     * @return 构造完成的 Discriminator 对象
     */
    public Discriminator build() {
      assert discriminator.resultMapping != null;
      assert discriminator.discriminatorMap != null;
      assert !discriminator.discriminatorMap.isEmpty();
      // 将映射表转换为不可变Map，防止后续被修改
      discriminator.discriminatorMap = Collections.unmodifiableMap(discriminator.discriminatorMap);
      return discriminator;
    }
  }

  /**
   * 获取鉴别器对应的结果映射。
   *
   * @return 鉴别器的结果映射对象
   */
  public ResultMapping getResultMapping() {
    return resultMapping;
  }

  /**
   * 获取列值到 resultMap ID 的映射表（不可变）。
   *
   * @return 不可变的鉴别器映射表
   */
  public Map<String, String> getDiscriminatorMap() {
    return discriminatorMap;
  }

  /**
   * 根据列值查找对应的 resultMap ID。
   *
   * @param s 数据库列的实际值
   * @return 对应的 resultMap ID，如果不存在则返回 null
   */
  public String getMapIdFor(String s) {
    return discriminatorMap.get(s);
  }

}
