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
package org.apache.ibatis.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

/**
 * MyBatis Mapper 注册中心，负责管理所有 Mapper 接口的注册与获取。
 * 维护 Mapper 接口与其代理工厂的映射关系，支持按包批量注册和运行时动态注册。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 */
public class MapperRegistry {

  /** MyBatis 全局配置对象，提供配置信息和运行时上下文 */
  private final Configuration config;

  /** 已注册的 Mapper 接口及其代理工厂的映射缓存 */
  private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();

  /**
   * 创建 MapperRegistry 实例。
   *
   * @param config MyBatis 全局配置对象
   */
  public MapperRegistry(Configuration config) {
    this.config = config;
  }

  /**
   * 获取指定类型的 Mapper 代理实例。
   *
   * @param type       Mapper 接口类型
   * @param sqlSession SqlSession 会话，用于创建代理对象
   * @return Mapper 代理实例
   * @throws BindingException 如果类型未注册或实例化失败
   */
  @SuppressWarnings("unchecked")
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    // 从缓存中获取对应的代理工厂
    final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
    // 未注册则抛出异常
    if (mapperProxyFactory == null) {
      throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
    }
    try {
      // 通过代理工厂创建新的 Mapper 代理实例
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
   * 注册 Mapper 接口到注册中心。
   * 只处理接口类型，会同时创建代理工厂并解析其中的注解配置。
   *
   * @param type Mapper 接口类型
   * @throws BindingException 如果类型不是接口或已重复注册
   */
  public <T> void addMapper(Class<T> type) {
    // 仅处理接口类型，忽略非接口类型（如普通类）
    if (type.isInterface()) {
      // 检查是否已重复注册
      if (hasMapper(type)) {
        throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
      }
      boolean loadCompleted = false;
      try {
        // 先注册到缓存，再进行注解解析
        // 顺序很关键：提前注册可避免解析过程中重复绑定导致的循环引用
        knownMappers.put(type, new MapperProxyFactory<>(type));
        // 解析 Mapper 接口中的注解（如 @Select、@Insert 等）
        MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
        parser.parse();
        loadCompleted = true;
      } finally {
        // 解析失败时回滚注册，确保缓存状态一致
        if (!loadCompleted) {
          knownMappers.remove(type);
        }
      }
    }
  }

  /**
   * 获取所有已注册的 Mapper 接口集合。
   *
   * @return 不可修改的 Mapper 接口集合
   * @since 3.2.2
   */
  public Collection<Class<?>> getMappers() {
    // 返回不可修改的集合，防止外部直接修改缓存
    return Collections.unmodifiableCollection(knownMappers.keySet());
  }

  /**
   * 批量注册指定包下所有继承或实现指定超类型的类为 Mapper。
   * 扫描包内所有符合类型的接口并逐个注册。
   *
   * @param packageName 包名，用于扫描 Mapper 接口
   * @param superType   超类型过滤器，只注册继承自该类型的接口
   * @since 3.2.2
   */
  public void addMappers(String packageName, Class<?> superType) {
    // 使用 ResolverUtil 扫描包下所有符合超类型条件的类
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
    resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
    Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
    // 逐个注册符合条件的 Mapper 接口
    for (Class<?> mapperClass : mapperSet) {
      addMapper(mapperClass);
    }
  }

  /**
   * 批量注册指定包下所有接口为 Mapper。
   * 相当于扫描包内所有接口并注册，等同于 addMappers(packageName, Object.class)。
   *
   * @param packageName 包名，用于扫描 Mapper 接口
   * @since 3.2.2
   */
  public void addMappers(String packageName) {
    addMappers(packageName, Object.class);
  }

}
