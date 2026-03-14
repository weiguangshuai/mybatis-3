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

/**
 * Reflector 工厂接口，负责创建和管理 Reflector 实例。
 *
 * Reflector 用于缓存类的元信息（属性、getter/setter 等），提升反射性能。
 */
public interface ReflectorFactory {

  /**
   * 判断类缓存是否启用。
   * @return 已启用返回 true，否则返回 false
   */
  boolean isClassCacheEnabled();

  /**
   * 设置类缓存是否启用。
   * @param classCacheEnabled true 启用缓存，false 禁用缓存
   */
  void setClassCacheEnabled(boolean classCacheEnabled);

  /**
   * 获取指定类的 Reflector 实例。
   * 根据配置可能从缓存获取或创建新的 Reflector。
   * @param type 目标类
   * @return 该类对应的 Reflector 实例
   */
  Reflector findForClass(Class<?> type);
}