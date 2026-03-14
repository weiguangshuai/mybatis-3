/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * Reflector 工厂的默认实现，负责创建和管理 Reflector 实例的缓存。
 */
public class DefaultReflectorFactory implements ReflectorFactory {
  /** 是否启用 Reflector 类的缓存，默认为 true */
  private boolean classCacheEnabled = true;
  /** Class 到 Reflector 实例的缓存映射表，使用 ConcurrentHashMap 保证线程安全 */
  private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<Class<?>, Reflector>();

  public DefaultReflectorFactory() {
  }

  @Override
  public boolean isClassCacheEnabled() {
    return classCacheEnabled;
  }

  @Override
  public void setClassCacheEnabled(boolean classCacheEnabled) {
    this.classCacheEnabled = classCacheEnabled;
  }

  /**
   * 获取指定类的 Reflector 实例。
   * 当缓存启用时，从缓存中获取或创建新的 Reflector 并缓存；
   * 当缓存禁用时，每次都创建新的 Reflector 实例。
   *
   * @param type 要获取 Reflector 的类
   * @return 对应类的 Reflector 实例
   */
  @Override
  public Reflector findForClass(Class<?> type) {
    if (classCacheEnabled) {
            // synchronized (type) removed see issue #461
      Reflector cached = reflectorMap.get(type);
      if (cached == null) {
        // 缓存未命中，创建新的 Reflector 并放入缓存
        cached = new Reflector(type);
        reflectorMap.put(type, cached);
      }
      return cached;
    } else {
      // 缓存禁用，每次都返回新实例
      return new Reflector(type);
    }
  }

}
