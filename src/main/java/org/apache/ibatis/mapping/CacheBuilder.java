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
package org.apache.ibatis.mapping;

import org.apache.ibatis.builder.InitializingObject;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.cache.decorators.*;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 缓存构建器，用于创建和配置 MyBatis 二级缓存。
 *
 * @author Clinton Begin
 */
public class CacheBuilder {
  /**
   * 缓存的唯一标识符，对应 Mapper 命名空间
   */
  private final String id;
  /**
   * 缓存的具体实现类
   */
  private Class<? extends Cache> implementation;
  /**
   * 缓存装饰器列表，用于增强缓存功能
   */
  private final List<Class<? extends Cache>> decorators;
  /**
   * 缓存容量大小
   */
  private Integer size;
  /**
   * 缓存自动清除间隔（毫秒）
   */
  private Long clearInterval;
  /**
   * 是否支持序列化读写
   */
  private boolean readWrite;
  /**
   * 缓存自定义配置属性
   */
  private Properties properties;
  /**
   * 是否启用阻塞缓存
   */
  private boolean blocking;

  /**
   * 创建 CacheBuilder 实例。
   *
   * @param id 缓存唯一标识
   */
  public CacheBuilder(String id) {
    this.id = id;
    this.decorators = new ArrayList<>();
  }

  /**
   * 设置缓存的具体实现类。
   *
   * @param implementation 缓存实现类
   * @return 当前构建器
   */
  public CacheBuilder implementation(Class<? extends Cache> implementation) {
    this.implementation = implementation;
    return this;
  }

  /**
   * 添加缓存装饰器，用于增强缓存功能。
   *
   * @param decorator 装饰器类
   * @return 当前构建器
   */
  public CacheBuilder addDecorator(Class<? extends Cache> decorator) {
    if (decorator != null) {
      this.decorators.add(decorator);
    }
    return this;
  }

  /**
   * 设置缓存容量大小。
   *
   * @param size 缓存大小
   * @return 当前构建器
   */
  public CacheBuilder size(Integer size) {
    this.size = size;
    return this;
  }

  /**
   * 设置缓存自动清除间隔。
   *
   * @param clearInterval 清除间隔（毫秒）
   * @return 当前构建器
   */
  public CacheBuilder clearInterval(Long clearInterval) {
    this.clearInterval = clearInterval;
    return this;
  }

  /**
   * 设置是否支持序列化读写。
   *
   * @param readWrite true 表示支持序列化读写
   * @return 当前构建器
   */
  public CacheBuilder readWrite(boolean readWrite) {
    this.readWrite = readWrite;
    return this;
  }

  /**
   * 设置是否启用阻塞缓存。
   *
   * @param blocking true 表示启用阻塞
   * @return 当前构建器
   */
  public CacheBuilder blocking(boolean blocking) {
    this.blocking = blocking;
    return this;
  }

  /**
   * 设置缓存自定义配置属性。
   *
   * @param properties 配置属性
   * @return 当前构建器
   */
  public CacheBuilder properties(Properties properties) {
    this.properties = properties;
    return this;
  }

  /**
   * 构建缓存实例，完成所有配置后调用此方法创建最终缓存对象。
   *
   * @return 配置完成的缓存对象
   */
  public Cache build() {
    // 设置默认实现类
    setDefaultImplementations();
    // 创建缓存实例
    Cache cache = newBaseCacheInstance(implementation, id);
    // 设置缓存属性
    setCacheProperties(cache);
    // issue #352, do not apply decorators to custom caches
    // 仅对内置 PerpetualCache 应用装饰器，自定义缓存不应用
    if (PerpetualCache.class.equals(cache.getClass())) {
      // 应用用户自定义装饰器
      for (Class<? extends Cache> decorator : decorators) {
        cache = newCacheDecoratorInstance(decorator, cache);
        setCacheProperties(cache);
      }
      // 应用标准装饰器（LoggingCache、SynchronizedCache 等）
      cache = setStandardDecorators(cache);
    } else if (!LoggingCache.class.isAssignableFrom(cache.getClass())) {
      // 为没有日志功能的自定义缓存添加 LoggingCache
      cache = new LoggingCache(cache);
    }
    return cache;
  }

  /**
   * 设置默认的缓存实现类和装饰器。
   * 若未指定实现类，默认使用 PerpetualCache；
   * 若未指定装饰器，默认添加 LruCache。
   */
  private void setDefaultImplementations() {
    if (implementation == null) {
      implementation = PerpetualCache.class;
      // 未配置装饰器时，默认使用 LRU 策略
      if (decorators.isEmpty()) {
        decorators.add(LruCache.class);
      }
    }
  }

  /**
   * 应用标准缓存装饰器，包括 ScheduledCache、SerializedCache、LoggingCache、SynchronizedCache、BlockingCache。
   *
   * @param cache 基础缓存对象
   * @return 包装后的缓存对象
   */
  private Cache setStandardDecorators(Cache cache) {
    try {
      MetaObject metaCache = SystemMetaObject.forObject(cache);
      // 设置缓存大小
      if (size != null && metaCache.hasSetter("size")) {
        metaCache.setValue("size", size);
      }
      // 设置定时清除
      if (clearInterval != null) {
        cache = new ScheduledCache(cache);
        ((ScheduledCache) cache).setClearInterval(clearInterval);
      }
      // 设置序列化缓存
      if (readWrite) {
        cache = new SerializedCache(cache);
      }
      // 添加日志功能
      cache = new LoggingCache(cache);
      // 添加同步功能
      cache = new SynchronizedCache(cache);
      // 添加阻塞功能
      if (blocking) {
        cache = new BlockingCache(cache);
      }
      return cache;
    } catch (Exception e) {
      throw new CacheException("Error building standard cache decorators.  Cause: " + e, e);
    }
  }

  /**
   * 设置缓存属性，包括自定义属性和初始化。
   *
   * @param cache 缓存对象
   */
  private void setCacheProperties(Cache cache) {
    if (properties != null) {
      MetaObject metaCache = SystemMetaObject.forObject(cache);
      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        String name = (String) entry.getKey();
        String value = (String) entry.getValue();
        if (metaCache.hasSetter(name)) {
          Class<?> type = metaCache.getSetterType(name);
          // 根据属性类型进行类型转换
          if (String.class == type) {
            metaCache.setValue(name, value);
          } else if (int.class == type
            || Integer.class == type) {
            metaCache.setValue(name, Integer.valueOf(value));
          } else if (long.class == type
            || Long.class == type) {
            metaCache.setValue(name, Long.valueOf(value));
          } else if (short.class == type
            || Short.class == type) {
            metaCache.setValue(name, Short.valueOf(value));
          } else if (byte.class == type
            || Byte.class == type) {
            metaCache.setValue(name, Byte.valueOf(value));
          } else if (float.class == type
            || Float.class == type) {
            metaCache.setValue(name, Float.valueOf(value));
          } else if (boolean.class == type
            || Boolean.class == type) {
            metaCache.setValue(name, Boolean.valueOf(value));
          } else if (double.class == type
            || Double.class == type) {
            metaCache.setValue(name, Double.valueOf(value));
          } else {
            throw new CacheException("Unsupported property type for cache: '" + name + "' of type " + type);
          }
        }
      }
    }
    // 调用初始化方法（若缓存实现了 InitializingObject 接口）
    if (InitializingObject.class.isAssignableFrom(cache.getClass())) {
      try {
        ((InitializingObject) cache).initialize();
      } catch (Exception e) {
        throw new CacheException("Failed cache initialization for '"
          + cache.getId() + "' on '" + cache.getClass().getName() + "'", e);
      }
    }
  }

  /**
   * 创建缓存基础实例。
   *
   * @param cacheClass 缓存类
   * @param id         缓存标识
   * @return 缓存实例
   */
  private Cache newBaseCacheInstance(Class<? extends Cache> cacheClass, String id) {
    Constructor<? extends Cache> cacheConstructor = getBaseCacheConstructor(cacheClass);
    try {
      return cacheConstructor.newInstance(id);
    } catch (Exception e) {
      throw new CacheException("Could not instantiate cache implementation (" + cacheClass + "). Cause: " + e, e);
    }
  }

  /**
   * 获取缓存类的构造函数（接受 String id 参数）。
   *
   * @param cacheClass 缓存类
   * @return 构造函数
   */
  private Constructor<? extends Cache> getBaseCacheConstructor(Class<? extends Cache> cacheClass) {
    try {
      return cacheClass.getConstructor(String.class);
    } catch (Exception e) {
      throw new CacheException("Invalid base cache implementation (" + cacheClass + ").  "
        + "Base cache implementations must have a constructor that takes a String id as a parameter.  Cause: " + e, e);
    }
  }

  /**
   * 创建缓存装饰器实例。
   *
   * @param cacheClass 装饰器类
   * @param base       被装饰的缓存对象
   * @return 装饰后的缓存对象
   */
  private Cache newCacheDecoratorInstance(Class<? extends Cache> cacheClass, Cache base) {
    Constructor<? extends Cache> cacheConstructor = getCacheDecoratorConstructor(cacheClass);
    try {
      return cacheConstructor.newInstance(base);
    } catch (Exception e) {
      throw new CacheException("Could not instantiate cache decorator (" + cacheClass + "). Cause: " + e, e);
    }
  }

  /**
   * 获取缓存装饰器类的构造函数（接受 Cache 参数）。
   *
   * @param cacheClass 装饰器类
   * @return 构造函数
   */
  private Constructor<? extends Cache> getCacheDecoratorConstructor(Class<? extends Cache> cacheClass) {
    try {
      return cacheClass.getConstructor(Cache.class);
    } catch (Exception e) {
      throw new CacheException("Invalid cache decorator (" + cacheClass + ").  "
        + "Cache decorators must have a constructor that takes a Cache instance as a parameter.  Cause: " + e, e);
    }
  }
}
