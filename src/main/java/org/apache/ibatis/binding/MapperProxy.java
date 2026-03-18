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
package org.apache.ibatis.binding;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.util.MapUtil;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Mapper 接口的动态代理实现，负责拦截方法调用并转发到 SqlSession 执行。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

  private static final long serialVersionUID = -4724728412955527868L;
  /** MethodHandles.Lookup 构造函数允许的访问模式 */
  private static final int ALLOWED_MODES = MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
      | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC;
  /** JDK 1.8 版本的 Lookup 构造函数，用于获取方法句柄 */
  private static final Constructor<Lookup> lookupConstructor;
  /** JDK 9+ 的 privateLookupIn 方法，用于获取私有方法句柄 */
  private static final Method privateLookupInMethod;
  /** SQL 会话，用于执行 SQL 语句 */
  private final SqlSession sqlSession;
  /** Mapper 接口类型 */
  private final Class<T> mapperInterface;
  /** 方法缓存，存储每个 Method 对应的调用器 */
  private final Map<Method, MapperMethodInvoker> methodCache;

  /**
   * 创建 MapperProxy 实例。
   *
   * @param sqlSession    SQL 会话
   * @param mapperInterface Mapper 接口类型
   * @param methodCache 方法调用器缓存
   */
  public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethodInvoker> methodCache) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.methodCache = methodCache;
  }

  static {
    Method privateLookupIn;
    try {
      privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
    } catch (NoSuchMethodException e) {
      privateLookupIn = null;
    }
    privateLookupInMethod = privateLookupIn;

    Constructor<Lookup> lookup = null;
    if (privateLookupInMethod == null) {
      // JDK 1.8
      try {
        lookup = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
        lookup.setAccessible(true);
      } catch (NoSuchMethodException e) {
        throw new IllegalStateException(
            "There is neither 'privateLookupIn(Class, Lookup)' nor 'Lookup(Class, int)' method in java.lang.invoke.MethodHandles.",
            e);
      } catch (Exception e) {
        lookup = null;
      }
    }
    lookupConstructor = lookup;
  }

  /**
   * 拦截方法调用，将请求转发到对应的 MapperMethodInvoker 执行。
   *
   * @param proxy  代理对象
   * @param method 被调用的方法
   * @param args   方法参数
   * @return 方法执行结果
   * @throws Throwable 执行过程中的异常
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      // Object 类的方法直接调用，不走代理逻辑
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, args);
      } else {
        // 从缓存获取或创建方法调用器，执行 Mapper 方法
        return cachedInvoker(method).invoke(proxy, method, args, sqlSession);
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }

  /**
   * 获取方法调用器，优先使用缓存，对于 default 方法和普通方法采用不同策略。
   *
   * @param method 被调用的方法
   * @return 方法调用器
   * @throws Throwable 可能抛出的异常
   */
  private MapperMethodInvoker cachedInvoker(Method method) throws Throwable {
    try {
      return MapUtil.computeIfAbsent(methodCache, method, m -> {
        // 判断是否为 default 方法（接口中的默认方法）
        if (m.isDefault()) {
          try {
            // 根据 JDK 版本选择获取方法句柄的方式
            if (privateLookupInMethod == null) {
              // JDK 1.8 使用反射方式获取
              return new DefaultMethodInvoker(getMethodHandleJava8(method));
            } else {
              // JDK 9+ 使用 MethodHandles API
              return new DefaultMethodInvoker(getMethodHandleJava9(method));
            }
          } catch (IllegalAccessException | InstantiationException | InvocationTargetException
              | NoSuchMethodException e) {
            throw new RuntimeException(e);
          }
        } else {
          // 普通 Mapper 方法创建 PlainMethodInvoker
          return new PlainMethodInvoker(new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
        }
      });
    } catch (RuntimeException re) {
      Throwable cause = re.getCause();
      throw cause == null ? re : cause;
    }
  }

  /**
   * 使用 JDK 9+ 的 MethodHandles API 获取 default 方法的方法句柄。
   *
   * @param method 要获取句柄的方法
   * @return 方法句柄
   * @throws NoSuchMethodException    方法不存在
   * @throws IllegalAccessException    非法访问
   * @throws InvocationTargetException 调用目标异常
   */
  private MethodHandle getMethodHandleJava9(Method method)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    final Class<?> declaringClass = method.getDeclaringClass();
    return ((Lookup) privateLookupInMethod.invoke(null, declaringClass, MethodHandles.lookup())).findSpecial(
        declaringClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
        declaringClass);
  }

  /**
   * 使用 JDK 1.8 的反射 API 获取 default 方法的方法句柄。
   *
   * @param method 要获取句柄的方法
   * @return 方法句柄
   * @throws IllegalAccessException    非法访问
   * @throws InstantiationException     实例化异常
   * @throws InvocationTargetException 调用目标异常
   */
  private MethodHandle getMethodHandleJava8(Method method)
      throws IllegalAccessException, InstantiationException, InvocationTargetException {
    final Class<?> declaringClass = method.getDeclaringClass();
    return lookupConstructor.newInstance(declaringClass, ALLOWED_MODES).unreflectSpecial(method, declaringClass);
  }

  /** 方法调用器接口，用于执行 Mapper 方法 */
  interface MapperMethodInvoker {
    /**
     * 执行方法调用。
     *
     * @param proxy     代理对象
     * @param method    被调用的方法
     * @param args      方法参数
     * @param sqlSession SQL 会话
     * @return 方法执行结果
     * @throws Throwable 执行过程中的异常
     */
    Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable;
  }

  /** 普通 Mapper 方法的调用器实现 */
  private static class PlainMethodInvoker implements MapperMethodInvoker {
    private final MapperMethod mapperMethod;

    public PlainMethodInvoker(MapperMethod mapperMethod) {
      super();
      this.mapperMethod = mapperMethod;
    }

    /**
     * 通过 MapperMethod 执行 SQL。
     *
     * @param proxy     代理对象
     * @param method    被调用的方法
     * @param args      方法参数
     * @param sqlSession SQL 会话
     * @return 方法执行结果
     * @throws Throwable 执行过程中的异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
      return mapperMethod.execute(sqlSession, args);
    }
  }

  /** JDK default 方法的调用器实现，使用 MethodHandle 执行 */
  private static class DefaultMethodInvoker implements MapperMethodInvoker {
    private final MethodHandle methodHandle;

    public DefaultMethodInvoker(MethodHandle methodHandle) {
      super();
      this.methodHandle = methodHandle;
    }

    /**
     * 通过 MethodHandle 执行 default 方法。
     *
     * @param proxy     代理对象
     * @param method    被调用的方法
     * @param args      方法参数
     * @param sqlSession SQL 会话（此实现中未使用）
     * @return 方法执行结果
     * @throws Throwable 执行过程中的异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
      return methodHandle.bindTo(proxy).invokeWithArguments(args);
    }
  }
}
