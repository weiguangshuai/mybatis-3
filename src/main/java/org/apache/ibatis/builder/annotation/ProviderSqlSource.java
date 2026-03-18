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
package org.apache.ibatis.builder.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * 处理 @SelectProvider、@InsertProvider 等注解提供的动态 SQL 构建。
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class ProviderSqlSource implements SqlSource {

  /** MyBatis 全局配置 */
  private final Configuration configuration;
  /** SQL 提供者类（如 XXXDaoProvider） */
  private final Class<?> providerType;
  /** SQL 脚本语言驱动 */
  private final LanguageDriver languageDriver;
  /** Mapper 接口中被 @SelectProvider 等注解标记的方法 */
  private final Method mapperMethod;
  /** SQL 提供者类中实际执行的方法（生成 SQL 的方法） */
  private final Method providerMethod;
  /** providerMethod 的参数名称数组 */
  private final String[] providerMethodArgumentNames;
  /** providerMethod 的参数类型数组 */
  private final Class<?>[] providerMethodParameterTypes;
  /** 提供者上下文，包含 Mapper 类型、方法名、数据库 ID 等信息 */
  private final ProviderContext providerContext;
  /** ProviderContext 参数在 providerMethod 参数列表中的索引位置 */
  private final Integer providerContextIndex;

  /**
   * This constructor will remove at a future version.
   *
   * @param configuration
   *          the configuration
   * @param provider
   *          the provider
   * @deprecated Since 3.5.3, Please use the {@link #ProviderSqlSource(Configuration, Annotation, Class, Method)}
   *             instead of this.
   */
  @Deprecated
  public ProviderSqlSource(Configuration configuration, Object provider) {
    this(configuration, provider, null, null);
  }

  /**
   * This constructor will remove at a future version.
   *
   * @param configuration
   *          the configuration
   * @param provider
   *          the provider
   * @param mapperType
   *          the mapper type
   * @param mapperMethod
   *          the mapper method
   * @since 3.4.5
   * @deprecated Since 3.5.3, Please use the {@link #ProviderSqlSource(Configuration, Annotation, Class, Method)} instead of this.
   */
  @Deprecated
  public ProviderSqlSource(Configuration configuration, Object provider, Class<?> mapperType, Method mapperMethod) {
    this(configuration, (Annotation) provider, mapperType, mapperMethod);
  }

  /**
   * Instantiates a new provider sql source.
   *
   * @param configuration
   *          the configuration
   * @param provider
   *          the provider
   * @param mapperType
   *          the mapper type
   * @param mapperMethod
   *          the mapper method
   * @since 3.5.3
   */
  public ProviderSqlSource(Configuration configuration, Annotation provider, Class<?> mapperType, Method mapperMethod) {
    String candidateProviderMethodName;
    Method candidateProviderMethod = null;
    try {
      this.configuration = configuration;
      this.mapperMethod = mapperMethod;
      Lang lang = mapperMethod == null ? null : mapperMethod.getAnnotation(Lang.class);
      this.languageDriver = configuration.getLanguageDriver(lang == null ? null : lang.value());
      this.providerType = getProviderType(configuration, provider, mapperMethod);
      candidateProviderMethodName = (String) provider.annotationType().getMethod("method").invoke(provider);

      if (candidateProviderMethodName.length() == 0 && ProviderMethodResolver.class.isAssignableFrom(this.providerType)) {
        candidateProviderMethod = ((ProviderMethodResolver) this.providerType.getDeclaredConstructor().newInstance())
            .resolveMethod(new ProviderContext(mapperType, mapperMethod, configuration.getDatabaseId()));
      }
      if (candidateProviderMethod == null) {
        candidateProviderMethodName = candidateProviderMethodName.length() == 0 ? "provideSql" : candidateProviderMethodName;
        for (Method m : this.providerType.getMethods()) {
          if (candidateProviderMethodName.equals(m.getName()) && CharSequence.class.isAssignableFrom(m.getReturnType())) {
            if (candidateProviderMethod != null) {
              throw new BuilderException("Error creating SqlSource for SqlProvider. Method '"
                  + candidateProviderMethodName + "' is found multiple in SqlProvider '" + this.providerType.getName()
                  + "'. Sql provider method can not overload.");
            }
            candidateProviderMethod = m;
          }
        }
      }
    } catch (BuilderException e) {
      throw e;
    } catch (Exception e) {
      throw new BuilderException("Error creating SqlSource for SqlProvider.  Cause: " + e, e);
    }
    if (candidateProviderMethod == null) {
      throw new BuilderException("Error creating SqlSource for SqlProvider. Method '"
          + candidateProviderMethodName + "' not found in SqlProvider '" + this.providerType.getName() + "'.");
    }
    this.providerMethod = candidateProviderMethod;
    this.providerMethodArgumentNames = new ParamNameResolver(configuration, this.providerMethod).getNames();
    this.providerMethodParameterTypes = this.providerMethod.getParameterTypes();

    ProviderContext candidateProviderContext = null;
    Integer candidateProviderContextIndex = null;
    for (int i = 0; i < this.providerMethodParameterTypes.length; i++) {
      Class<?> parameterType = this.providerMethodParameterTypes[i];
      if (parameterType == ProviderContext.class) {
        if (candidateProviderContext != null) {
          throw new BuilderException("Error creating SqlSource for SqlProvider. ProviderContext found multiple in SqlProvider method ("
              + this.providerType.getName() + "." + providerMethod.getName()
              + "). ProviderContext can not define multiple in SqlProvider method argument.");
        }
        candidateProviderContext = new ProviderContext(mapperType, mapperMethod, configuration.getDatabaseId());
        candidateProviderContextIndex = i;
      }
    }
    this.providerContext = candidateProviderContext;
    this.providerContextIndex = candidateProviderContextIndex;
  }

  /**
   * 根据参数对象获取绑定的 SQL。
   *
   * @param parameterObject 方法参数对象
   * @return 包含最终 SQL 及参数的 BoundSql 对象
   */
  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    SqlSource sqlSource = createSqlSource(parameterObject);
    return sqlSource.getBoundSql(parameterObject);
  }

  /**
   * 创建 SqlSource 对象，调用提供者方法获取 SQL 字符串。
   *
   * @param parameterObject 方法参数对象
   * @return SqlSource 对象
   */
  private SqlSource createSqlSource(Object parameterObject) {
    try {
      String sql;
      if (parameterObject instanceof Map) {
        // 计算需要绑定的参数数量（排除 ProviderContext）
        int bindParameterCount = providerMethodParameterTypes.length - (providerContext == null ? 0 : 1);
        // 单参数绑定：参数类型与提供者方法参数匹配
        if (bindParameterCount == 1
            && providerMethodParameterTypes[Integer.valueOf(0).equals(providerContextIndex) ? 1 : 0].isAssignableFrom(parameterObject.getClass())) {
          sql = invokeProviderMethod(extractProviderMethodArguments(parameterObject));
        } else {
          // 多参数绑定：从 Map 中按名称提取参数
          @SuppressWarnings("unchecked")
          Map<String, Object> params = (Map<String, Object>) parameterObject;
          sql = invokeProviderMethod(extractProviderMethodArguments(params, providerMethodArgumentNames));
        }
      } else if (providerMethodParameterTypes.length == 0) {
        // 无参方法
        sql = invokeProviderMethod();
      } else if (providerMethodParameterTypes.length == 1) {
        // 单参数：根据是否有 ProviderContext 决定传递什么
        if (providerContext == null) {
          sql = invokeProviderMethod(parameterObject);
        } else {
          sql = invokeProviderMethod(providerContext);
        }
      } else if (providerMethodParameterTypes.length == 2) {
        sql = invokeProviderMethod(extractProviderMethodArguments(parameterObject));
      } else {
        throw new BuilderException("Cannot invoke SqlProvider method '" + providerMethod
          + "' with specify parameter '" + (parameterObject == null ? null : parameterObject.getClass())
          + "' because SqlProvider method arguments for '" + mapperMethod + "' is an invalid combination.");
      }
      Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
      return languageDriver.createSqlSource(configuration, sql, parameterType);
    } catch (BuilderException e) {
      throw e;
    } catch (Exception e) {
      throw new BuilderException("Error invoking SqlProvider method '" + providerMethod
          + "' with specify parameter '" + (parameterObject == null ? null : parameterObject.getClass()) + "'.  Cause: " + extractRootCause(e), e);
    }
  }

  /**
   * 从异常链中获取最根本的异常原因。
   *
   * @param e 异常对象
   * @return 最根本的异常原因
   */
  private Throwable extractRootCause(Exception e) {
    Throwable cause = e;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause;
  }

  /**
   * 将单个参数对象转换为提供者方法所需的参数数组。
   * 处理 ProviderContext 参数的插入位置。
   *
   * @param parameterObject 方法参数对象
   * @return 提供者方法所需的参数数组
   */
  private Object[] extractProviderMethodArguments(Object parameterObject) {
    if (providerContext != null) {
      Object[] args = new Object[2];
      args[providerContextIndex == 0 ? 1 : 0] = parameterObject;
      args[providerContextIndex] = providerContext;
      return args;
    } else {
      return new Object[] { parameterObject };
    }
  }

  /**
   * 从参数 Map 中提取提供者方法所需的参数数组。
   *
   * @param params 参数名称到值的映射
   * @param argumentNames 参数名称数组
   * @return 提供者方法所需的参数数组
   */
  private Object[] extractProviderMethodArguments(Map<String, Object> params, String[] argumentNames) {
    Object[] args = new Object[argumentNames.length];
    for (int i = 0; i < args.length; i++) {
      if (providerContextIndex != null && providerContextIndex == i) {
        args[i] = providerContext;
      } else {
        args[i] = params.get(argumentNames[i]);
      }
    }
    return args;
  }

  /**
   * 调用提供者方法获取 SQL 字符串。
   * 如果提供者方法不是静态的，则创建提供者类实例后再调用。
   *
   * @param args 提供者方法参数
   * @return SQL 字符串
   * @throws Exception 调用提供者方法时可能抛出的异常
   */
  private String invokeProviderMethod(Object... args) throws Exception {
    Object targetObject = null;
    // 非静态方法需要创建实例，静态方法则传入 null
    if (!Modifier.isStatic(providerMethod.getModifiers())) {
      targetObject = providerType.getDeclaredConstructor().newInstance();
    }
    CharSequence sql = (CharSequence) providerMethod.invoke(targetObject, args);
    return sql != null ? sql.toString() : null;
  }

  /**
   * 从注解中解析出 SQL 提供者类型。
   * 处理 @SelectProvider 等注解的 type 和 value 属性。
   *
   * @param configuration MyBatis 配置
   * @param providerAnnotation 提供者注解（如 @SelectProvider）
   * @param mapperMethod Mapper 方法
   * @return SQL 提供者类
   */
  private Class<?> getProviderType(Configuration configuration, Annotation providerAnnotation, Method mapperMethod)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> type = (Class<?>) providerAnnotation.annotationType().getMethod("type").invoke(providerAnnotation);
    Class<?> value = (Class<?>) providerAnnotation.annotationType().getMethod("value").invoke(providerAnnotation);
    // 两者都未指定，尝试使用默认提供者类型
    if (value == void.class && type == void.class) {
      if (configuration.getDefaultSqlProviderType() != null) {
        return configuration.getDefaultSqlProviderType();
      }
      throw new BuilderException("Please specify either 'value' or 'type' attribute of @"
          + providerAnnotation.annotationType().getSimpleName()
          + " at the '" + mapperMethod.toString() + "'.");
    }
    // 两者都指定但不一致，报错
    if (value != void.class && type != void.class && value != type) {
      throw new BuilderException("Cannot specify different class on 'value' and 'type' attribute of @"
          + providerAnnotation.annotationType().getSimpleName()
          + " at the '" + mapperMethod.toString() + "'.");
    }
    // 返回其中一个有效的值
    return value == void.class ? type : value;
  }

}
