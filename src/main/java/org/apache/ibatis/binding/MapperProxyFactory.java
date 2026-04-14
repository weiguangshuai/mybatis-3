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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.binding.MapperProxy.MapperMethodInvoker;
import org.apache.ibatis.session.SqlSession;

/**
 * Mapper 接口代理工厂，负责创建 Mapper 接口的动态代理对象
 *
 * @author Lasse Voss
 */
public class MapperProxyFactory<T> {

  /** Mapper 接口 Class 对象 */
  private final Class<T> mapperInterface;
  /** 方法调用器缓存，避免重复创建 MapperMethodInvoker */
  private final Map<Method, MapperMethodInvoker> methodCache = new ConcurrentHashMap<>();

  /**
   * 构造Mapper代理工厂
   *
   * @param mapperInterface Mapper接口Class
   */
  public MapperProxyFactory(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  /**
   * 获取Mapper接口Class
   *
   * @return Mapper接口Class
   */
  public Class<T> getMapperInterface() {
    return mapperInterface;
  }

  /**
   * 获取方法缓存Map
   *
   * @return 方法调用器缓存
   */
  public Map<Method, MapperMethodInvoker> getMethodCache() {
    return methodCache;
  }

  /**
   * 使用JDK动态代理创建Mapper接口代理实例
   *
   * @param mapperProxy Mapper代理对象，包含执行逻辑
   * @return Mapper接口的代理实例
   */
  @SuppressWarnings("unchecked")
  protected T newInstance(MapperProxy<T> mapperProxy) {
    // 使用JDK动态代理机制生成代理对象
    return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
  }

  /**
   * 创建Mapper接口代理实例
   *
   * @param sqlSession SqlSession会话实例，用于执行SQL
   * @return Mapper接口的代理实例
   */
  public T newInstance(SqlSession sqlSession) {
    // 创建MapperProxy，包含SqlSession和必要的上下文信息
    final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
    return newInstance(mapperProxy);
  }

}
