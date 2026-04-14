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
package org.apache.ibatis.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 方法参数名称解析器，负责将方法参数映射为可被 SQL provider 使用的名称。
 */
public class ParamNameResolver {

  /** 通用参数名称前缀，用于生成 param1, param2 等参数名 */
  public static final String GENERIC_NAME_PREFIX = "param";

  /** 是否使用方法的实际参数名（需要 JDK 8+ -parameters 编译参数支持） */
  private final boolean useActualParamName;

  /**
   * <p>
   * 参数索引与参数名称的映射（按索引排序）。<br />
   * 参数名取自 @Param 注解值；若未指定，则使用索引值。注意当方法包含特殊参数
   *（RowBounds 或 ResultHandler）时，索引可能与实际位置不一致。
   * </p>
   * <ul>
   * <li>aMethod(@Param("M") int a, @Param("N") int b) -&gt; {{0, "M"}, {1, "N"}}</li>
   * <li>aMethod(int a, int b) -&gt; {{0, "0"}, {1, "1"}}</li>
   * <li>aMethod(int a, RowBounds rb, int b) -&gt; {{0, "0"}, {2, "1"}}</li>
   * </ul>
   */
  private final SortedMap<Integer, String> names;

  /** 标记是否存在 @Param 注解，用于优化单参数场景的处理逻辑 */
  private boolean hasParamAnnotation;

  /**
   * 构造方法，解析方法参数并生成参数名到索引的映射。
   * @param config MyBatis 配置对象
   * @param method 要解析的 Mapper 方法
   */
  public ParamNameResolver(Configuration config, Method method) {
    this.useActualParamName = config.isUseActualParamName();
    final Class<?>[] paramTypes = method.getParameterTypes();
    final Annotation[][] paramAnnotations = method.getParameterAnnotations();
    final SortedMap<Integer, String> map = new TreeMap<>();
    int paramCount = paramAnnotations.length;
    // get names from @Param annotations
    for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
      if (isSpecialParameter(paramTypes[paramIndex])) {
        // skip special parameters
        continue;
      }
      String name = null;
      for (Annotation annotation : paramAnnotations[paramIndex]) {
        if (annotation instanceof Param) {
          hasParamAnnotation = true;
          name = ((Param) annotation).value();
          break;
        }
      }
      if (name == null) {
        // @Param was not specified.
        if (useActualParamName) {
          name = getActualParamName(method, paramIndex);
        }
        if (name == null) {
          // use the parameter index as the name ("0", "1", ...)
          // gcode issue #71
          name = String.valueOf(map.size());
        }
      }
      map.put(paramIndex, name);
    }
    names = Collections.unmodifiableSortedMap(map);
  }

  /**
   * 通过 JDK 编译参数获取方法的实际参数名
   *
   * @param method 方法
   * @param paramIndex 参数索引
   * @return 实际参数名，获取失败返回 null
   */
  private String getActualParamName(Method method, int paramIndex) {
    return ParamNameUtil.getParamNames(method).get(paramIndex);
  }

  /**
   * 判断参数类型是否为特殊参数（RowBounds 或 ResultHandler）
   * 特殊参数不参与名称解析，会在构造方法中被跳过
   *
   * @param clazz 参数类型
   * @return 是否为特殊参数
   */
  private static boolean isSpecialParameter(Class<?> clazz) {
    return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
  }

  /**
   * 获取方法参数名称列表，供 SQL provider 使用
   *
   * @return 参数名称数组
   */
  public String[] getNames() {
    return names.values().toArray(new String[0]);
  }

  /**
   * <p>
   * 将方法参数转换为命名参数。单参数无特殊参数时直接返回，多参数时构建 ParamMap。
   * 除了原始参数名，还会添加 param1, param2 等通用参数名。
   * </p>
   *
   * @param args 方法参数数组
   * @return 命名参数对象，单参数可能返回集合/数组本身，多参数返回 ParamMap
   */
  public Object getNamedParams(Object[] args) {
    final int paramCount = names.size();
    if (args == null || paramCount == 0) {
      return null;
    } else if (!hasParamAnnotation && paramCount == 1) {
      Object value = args[names.firstKey()];
      return wrapToMapIfCollection(value, useActualParamName ? names.get(names.firstKey()) : null);
    } else {
      final Map<String, Object> param = new ParamMap<>();
      int i = 0;
      for (Map.Entry<Integer, String> entry : names.entrySet()) {
        param.put(entry.getValue(), args[entry.getKey()]);
        // add generic param names (param1, param2, ...)
        final String genericParamName = GENERIC_NAME_PREFIX + (i + 1);
        // ensure not to overwrite parameter named with @Param
        if (!names.containsValue(genericParamName)) {
          param.put(genericParamName, args[entry.getKey()]);
        }
        i++;
      }
      return param;
    }
  }

  /**
   * 如果参数是 Collection 或数组，则包装为 ParamMap；否则直接返回原对象。
   * 包装时会添加 "collection"/"list"/"array" 等标准 key，并可根据实际参数名添加别名。
   *
   * @param object 参数对象
   * @param actualParamName 实际参数名（来自 @Param 或 JDK 参数名）
   * @return 包装后的 ParamMap 或原对象
   * @since 3.5.5
   */
  public static Object wrapToMapIfCollection(Object object, String actualParamName) {
    if (object instanceof Collection) {
      ParamMap<Object> map = new ParamMap<>();
      map.put("collection", object);
      if (object instanceof List) {
        map.put("list", object);
      }
      Optional.ofNullable(actualParamName).ifPresent(name -> map.put(name, object));
      return map;
    } else if (object != null && object.getClass().isArray()) {
      ParamMap<Object> map = new ParamMap<>();
      map.put("array", object);
      Optional.ofNullable(actualParamName).ifPresent(name -> map.put(name, object));
      return map;
    }
    return object;
  }

}
