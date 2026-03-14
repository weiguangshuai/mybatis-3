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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.SqlSession;

/**
 * Mapper 接口的代理工厂类，负责创建 Mapper 接口的动态代理实例
 *
 * @author Lasse Voss
 */
public class MapperProxyFactory<T> {

  /** Mapper 接口类型 */
  private final Class<T> mapperInterface;
  /** 方法缓存，存储 Method 与 MapperMethod 的映射关系 */
  private final Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<Method, MapperMethod>();

  /**
   * 构造 MapperProxyFactory
   *
   * @param mapperInterface Mapper 接口类型
   */
  public MapperProxyFactory(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  /**
   * 获取 Mapper 接口类型
   *
   * @return Mapper 接口的 Class 对象
   */
  public Class<T> getMapperInterface() {
    return mapperInterface;
  }

  /**
   * 获取方法缓存
   *
   * @return Method 与 MapperMethod 的映射缓存
   */
  public Map<Method, MapperMethod> getMethodCache() {
    return methodCache;
  }

  /**
   * 使用指定的 MapperProxy 创建代理实例
   *
   * @param mapperProxy Mapper 代理对象
   * @return Mapper 接口的动态代理实例
   */
  @SuppressWarnings("unchecked")
  protected T newInstance(MapperProxy<T> mapperProxy) {
    // 使用 JDK 动态代理创建 Mapper 接口的代理对象
    return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
  }

  /**
   * 创建 Mapper 接口的代理实例
   *
   * @param sqlSession SqlSession 会话
   * @return Mapper 接口的代理实例
   */
  public T newInstance(SqlSession sqlSession) {
    // 创建 MapperProxy，传入 SqlSession、接口类型和方法缓存
    final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterface, methodCache);
    return newInstance(mapperProxy);
  }

}
