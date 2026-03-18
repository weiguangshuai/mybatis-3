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

import org.apache.ibatis.cache.Cache;

/**
 * @author Clinton Begin
 */
public class CacheRefResolver {
  /** Mapper构建助手，用于执行缓存引用解析 */
  private final MapperBuilderAssistant assistant;
  /** 引用的缓存命名空间，指定使用哪个Mapper的二级缓存 */
  private final String cacheRefNamespace;

  /**
   * 创建缓存引用解析器。
   *
   * @param assistant Mapper构建助手
   * @param cacheRefNamespace 引用的缓存命名空间
   */
  public CacheRefResolver(MapperBuilderAssistant assistant, String cacheRefNamespace) {
    this.assistant = assistant;
    this.cacheRefNamespace = cacheRefNamespace;
  }

  /**
   * 解析并返回缓存引用。
   *
   * @return 引用命名空间对应的二级缓存实例
   */
  public Cache resolveCacheRef() {
    return assistant.useCacheRef(cacheRefNamespace);
  }
}