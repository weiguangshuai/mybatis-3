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
package org.apache.ibatis.binding;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * MyBatis Mapper 注册中心，负责管理所有 Mapper 接口及其代理工厂的注册与获取。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 */
public class MapperRegistry {

  /** MyBatis 全局配置对象 */
  private final Configuration config;
  /** 已注册的 Mapper 接口及其代理工厂的映射 */
  private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<Class<?>, MapperProxyFactory<?>>();

  /**
   * 创建 MapperRegistry 实例。
   *
   * @param config MyBatis 全局配置对象
   */
  public MapperRegistry(Configuration config) {
    this.config = config;
  }

  /**
   * 获取指定类型的 Mapper 代理对象。
   *
   * @param type      Mapper 接口类型
   * @param sqlSession SqlSession 实例
   * @return Mapper 代理对象实例
   */
  @SuppressWarnings("unchecked")
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
    // 未注册的 Mapper 类型抛出异常
    if (mapperProxyFactory == null) {
      throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
    }
    try {
      // 创建 Mapper 代理实例
      return mapperProxyFactory.newInstance(sqlSession);
    } catch (Exception e) {
      throw new BindingException("Error getting mapper instance. Cause: " + e, e);
    }
  }
  
  /**
   * 检查指定类型的 Mapper 是否已注册。
   *
   * @param type Mapper 接口类型
   * @return 已注册返回 true，否则返回 false
   */
  public <T> boolean hasMapper(Class<T> type) {
    return knownMappers.containsKey(type);
  }

  /**
   * 注册 Mapper 接口到注册中心，同时解析该 Mapper 的注解配置。
   *
   * @param type Mapper 接口类型
   * @throws BindingException 如果类型不是接口或已重复注册
   */
  public <T> void addMapper(Class<T> type) {
    if (type.isInterface()) {
      // 检查是否重复注册
      if (hasMapper(type)) {
        throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
      }
      boolean loadCompleted = false;
      try {
        knownMappers.put(type, new MapperProxyFactory<T>(type));
        // It's important that the type is added before the parser is run
        // otherwise the binding may automatically be attempted by the
        // mapper parser. If the type is already known, it won't try.
        MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
        parser.parse();
        loadCompleted = true;
      } finally {
        // 解析失败时回滚注册，防止内存泄漏
        if (!loadCompleted) {
          knownMappers.remove(type);
        }
      }
    }
  }

  /**
   * 获取所有已注册的 Mapper 接口类型集合。
   *
   * @return 不可变的 Mapper 接口类型集合
   * @since 3.2.2
   */
  public Collection<Class<?>> getMappers() {
    return Collections.unmodifiableCollection(knownMappers.keySet());
  }

  /**
   * 扫描指定包下指定超类型的所有类，并注册为 Mapper。
   *
   * @param packageName 要扫描的包名
   * @param superType   类的超类型限定，只注册该类型的子类
   * @since 3.2.2
   */
  public void addMappers(String packageName, Class<?> superType) {
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
    resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
    Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
    for (Class<?> mapperClass : mapperSet) {
      addMapper(mapperClass);
    }
  }

  /**
   * 扫描指定包下所有类，并注册为 Mapper。
   *
   * @param packageName 要扫描的包名
   * @since 3.2.2
   */
  public void addMappers(String packageName) {
    addMappers(packageName, Object.class);
  }
  
}
