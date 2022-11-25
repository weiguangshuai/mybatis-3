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
package org.apache.ibatis.reflection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.ibatis.util.MapUtil;

/**
 * Reflector 的默认实现工厂，负责创建和管理 Reflector 实例。
 */
public class DefaultReflectorFactory implements ReflectorFactory {
  /** 是否启用类级别的 Reflector 缓存 */
  private boolean classCacheEnabled = true;
  /** 缓存 Class 到 Reflector 的映射，使用并发 Map 保证线程安全 */
  private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<>();

  public DefaultReflectorFactory() {
  }

  /**
   * 判断是否启用了类级别缓存。
   * @return 启用返回 true，否则返回 false
   */
  @Override
  public boolean isClassCacheEnabled() {
    return classCacheEnabled;
  }

  /**
   * 设置是否启用类级别缓存。
   * @param classCacheEnabled 设为 true 启用缓存，false 禁用缓存
   */
  @Override
  public void setClassCacheEnabled(boolean classCacheEnabled) {
    this.classCacheEnabled = classCacheEnabled;
  }

  /**
   * 为指定类查找或创建 Reflector 实例。
   * 当缓存启用时，从映射中获取或创建后缓存；禁用时每次创建新实例。
   * @param type 要反射的目标类
   * @return 对应类的 Reflector 实例
   */
  @Override
  public Reflector findForClass(Class<?> type) {
    if (classCacheEnabled) {
      // synchronized (type) removed see issue #461
      return MapUtil.computeIfAbsent(reflectorMap, type, Reflector::new);
    } else {
      return new Reflector(type);
    }
  }

}
