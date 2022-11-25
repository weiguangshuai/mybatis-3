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

/**
 * Reflector 工厂接口，负责创建和管理 Reflector 实例
 */
public interface ReflectorFactory {

  /**
   * @return 是否启用类缓存
   */
  boolean isClassCacheEnabled();

  /**
   * 设置是否启用类缓存
   * @param classCacheEnabled 是否启用
   */
  void setClassCacheEnabled(boolean classCacheEnabled);

  /**
   * 获取指定类的 Reflector 实例
   * @param type 目标类
   * @return Reflector 实例
   */
  Reflector findForClass(Class<?> type);
}