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

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.util.MapUtil;

/**
 * LanguageDriverRegistry - 负责管理和维护 {@link LanguageDriver} 实例的注册与默认 LanguageDriver 配置
 *
 * @author Frank D. Martinez [mnesarco]
 */
public class LanguageDriverRegistry {

  /**
   * 已注册的 LanguageDriver 实例 Map，键为 LanguageDriver 类型，值为对应实例
   */
  private final Map<Class<? extends LanguageDriver>, LanguageDriver> LANGUAGE_DRIVER_MAP = new HashMap<>();

  /**
   * 默认 LanguageDriver 类型
   */
  private Class<? extends LanguageDriver> defaultDriverClass;

  /**
   * 注册指定类型的 LanguageDriver，若尚未注册则通过反射实例化
   *
   * @param cls LanguageDriver 类型
   */
  public void register(Class<? extends LanguageDriver> cls) {
    if (cls == null) {
      throw new IllegalArgumentException("null is not a valid Language Driver");
    }
    // 延迟实例化：仅在 LanguageDriver 尚未注册时才创建实例
    MapUtil.computeIfAbsent(LANGUAGE_DRIVER_MAP, cls, k -> {
      try {
        return k.getDeclaredConstructor().newInstance();
      } catch (Exception ex) {
        throw new ScriptingException("Failed to load language driver for " + cls.getName(), ex);
      }
    });
  }

  /**
   * 注册指定的 LanguageDriver 实例，若该类型已注册则忽略
   *
   * @param instance LanguageDriver 实例
   */
  public void register(LanguageDriver instance) {
    if (instance == null) {
      throw new IllegalArgumentException("null is not a valid Language Driver");
    }
    Class<? extends LanguageDriver> cls = instance.getClass();
    // 避免重复注册：仅在对应类型尚未存在时才放入实例
    if (!LANGUAGE_DRIVER_MAP.containsKey(cls)) {
      LANGUAGE_DRIVER_MAP.put(cls, instance);
    }
  }

  /**
   * 获取指定类型的 LanguageDriver 实例
   *
   * @param cls LanguageDriver 类型
   * @return 对应的 LanguageDriver 实例，若未注册则返回 null
   */
  public LanguageDriver getDriver(Class<? extends LanguageDriver> cls) {
    return LANGUAGE_DRIVER_MAP.get(cls);
  }

  /**
   * 获取当前默认的 LanguageDriver 实例
   *
   * @return 默认的 LanguageDriver 实例，若未设置则返回 null
   */
  public LanguageDriver getDefaultDriver() {
    return getDriver(getDefaultDriverClass());
  }

  /**
   * 获取当前默认的 LanguageDriver 类型
   *
   * @return 默认的 LanguageDriver 类型
   */
  public Class<? extends LanguageDriver> getDefaultDriverClass() {
    return defaultDriverClass;
  }

  /**
   * 设置默认 LanguageDriver 类型，同时确保该 LanguageDriver 已被注册
   *
   * @param defaultDriverClass 默认 LanguageDriver 类型
   */
  public void setDefaultDriverClass(Class<? extends LanguageDriver> defaultDriverClass) {
    register(defaultDriverClass);
    this.defaultDriverClass = defaultDriverClass;
  }

}
