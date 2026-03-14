/**
 *    Copyright 2009-2017 the original author or authors.
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

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.ibatis.lang.UsesJava7;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

/**
 * Mapper接口的动态代理实现，负责将接口方法调用分派到SqlSession执行SQL语句。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

  private static final long serialVersionUID = -6424540398559729838L;
  /** SqlSession实例，用于执行SQL语句 */
  private final SqlSession sqlSession;
  /** Mapper接口的Class对象 */
  private final Class<T> mapperInterface;
  /** MapperMethod缓存，避免重复创建 */
  private final Map<Method, MapperMethod> methodCache;

  /**
   * 构造MapperProxy实例。
   *
   * @param sqlSession SqlSession实例
   * @param mapperInterface Mapper接口Class
   * @param methodCache MapperMethod缓存映射
   */
  public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.methodCache = methodCache;
  }

  /**
   * 处理Mapper接口方法调用的入口方法。
   * 根据方法类型分别处理：Object方法、默认接口方法、Mapper方法。
   *
   * @param proxy 代理对象
   * @param method 被调用的方法
   * @param args 方法参数
   * @return 方法执行结果
   * @throws Throwable 执行过程中的异常
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      // Object类的方法直接转发
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, args);
      } else if (isDefaultMethod(method)) {
        // Java 8默认接口方法使用MethodHandles调用
        return invokeDefaultMethod(proxy, method, args);
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
    // 获取或创建MapperMethod并执行SQL
    final MapperMethod mapperMethod = cachedMapperMethod(method);
    return mapperMethod.execute(sqlSession, args);
  }

  /**
   * 获取缓存的MapperMethod，如不存在则创建并缓存。
   *
   * @param method Mapper接口方法
   * @return MapperMethod实例
   */
  private MapperMethod cachedMapperMethod(Method method) {
    MapperMethod mapperMethod = methodCache.get(method);
    if (mapperMethod == null) {
      // 根据方法信息创建MapperMethod并缓存
      mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
      methodCache.put(method, mapperMethod);
    }
    return mapperMethod;
  }

  /**
   * 使用MethodHandles调用Java 8默认接口方法。
   *
   * @param proxy 代理对象
   * @param method 默认方法
   * @param args 方法参数
   * @return 方法执行结果
   * @throws Throwable 执行过程中的异常
   */
  @UsesJava7
  private Object invokeDefaultMethod(Object proxy, Method method, Object[] args)
      throws Throwable {
    // 获取MethodHandles.Lookup构造器
    final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
        .getDeclaredConstructor(Class.class, int.class);
    if (!constructor.isAccessible()) {
      constructor.setAccessible(true);
    }
    final Class<?> declaringClass = method.getDeclaringClass();
    // 创建Lookup实例并调用默认方法
    return constructor
        .newInstance(declaringClass,
            MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
                | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC)
        .unreflectSpecial(method, declaringClass).bindTo(proxy).invokeWithArguments(args);
  }

  /**
   * Backport of java.lang.reflect.Method#isDefault()
   * 判断方法是否为Java 8默认接口方法。
   *
   * @param method 要检查的方法
   * @return 是否为默认方法
   */
  private boolean isDefaultMethod(Method method) {
    return (method.getModifiers()
        & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC
        && method.getDeclaringClass().isInterface();
  }
}
