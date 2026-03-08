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
package org.apache.ibatis.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.Resources;

/**
 * 类型别名注册表。
 * 负责维护别名与 Java 类型之间的映射关系，并支持包扫描批量注册，
 * 供配置解析阶段将短名称解析为具体 Class。
 *
 * @author Clinton Begin
 */
public class TypeAliasRegistry {

  /**
   * 别名到类型的映射表。
   * key 统一使用英文小写存储，保证别名匹配大小写不敏感。
   * 该实例随注册表对象生命周期存在，默认非线程安全，需要由上层保证并发访问策略。
   */
  private final Map<String, Class<?>> TYPE_ALIASES = new HashMap<String, Class<?>>();

  /**
   * 创建注册表并初始化内置常用类型别名。
   */
  public TypeAliasRegistry() {
    registerAlias("string", String.class);

    registerAlias("byte", Byte.class);
    registerAlias("long", Long.class);
    registerAlias("short", Short.class);
    registerAlias("int", Integer.class);
    registerAlias("integer", Integer.class);
    registerAlias("double", Double.class);
    registerAlias("float", Float.class);
    registerAlias("boolean", Boolean.class);

    registerAlias("byte[]", Byte[].class);
    registerAlias("long[]", Long[].class);
    registerAlias("short[]", Short[].class);
    registerAlias("int[]", Integer[].class);
    registerAlias("integer[]", Integer[].class);
    registerAlias("double[]", Double[].class);
    registerAlias("float[]", Float[].class);
    registerAlias("boolean[]", Boolean[].class);

    registerAlias("_byte", byte.class);
    registerAlias("_long", long.class);
    registerAlias("_short", short.class);
    registerAlias("_int", int.class);
    registerAlias("_integer", int.class);
    registerAlias("_double", double.class);
    registerAlias("_float", float.class);
    registerAlias("_boolean", boolean.class);

    registerAlias("_byte[]", byte[].class);
    registerAlias("_long[]", long[].class);
    registerAlias("_short[]", short[].class);
    registerAlias("_int[]", int[].class);
    registerAlias("_integer[]", int[].class);
    registerAlias("_double[]", double[].class);
    registerAlias("_float[]", float[].class);
    registerAlias("_boolean[]", boolean[].class);

    registerAlias("date", Date.class);
    registerAlias("decimal", BigDecimal.class);
    registerAlias("bigdecimal", BigDecimal.class);
    registerAlias("biginteger", BigInteger.class);
    registerAlias("object", Object.class);

    registerAlias("date[]", Date[].class);
    registerAlias("decimal[]", BigDecimal[].class);
    registerAlias("bigdecimal[]", BigDecimal[].class);
    registerAlias("biginteger[]", BigInteger[].class);
    registerAlias("object[]", Object[].class);

    registerAlias("map", Map.class);
    registerAlias("hashmap", HashMap.class);
    registerAlias("list", List.class);
    registerAlias("arraylist", ArrayList.class);
    registerAlias("collection", Collection.class);
    registerAlias("iterator", Iterator.class);

    registerAlias("ResultSet", ResultSet.class);
  }

  /**
   * 将别名或全限定类名解析为 {@link Class}。
   * 先按别名表查找，找不到时再尝试按类名加载。
   *
   * @param string 类型别名或全限定类名
   * @param <T> 目标类型参数
   * @return 解析得到的类型；当参数为 {@code null} 时返回 {@code null}
   * @throws TypeException 当别名和类名都无法解析时抛出
   */
  @SuppressWarnings("unchecked")
  // 当调用方泛型与实际类型不兼容时，仍可能抛出 ClassCastException。
  public <T> Class<T> resolveAlias(String string) {
    try {
      // 显式支持空入参，保持调用方空值语义。
      if (string == null) {
        return null;
      }
      // issue #748: 使用固定 Locale，避免土耳其语等区域设置导致大小写转换异常。
      String key = string.toLowerCase(Locale.ENGLISH);
      Class<T> value;
      if (TYPE_ALIASES.containsKey(key)) {
        // 优先从注册表按别名查找。
        value = (Class<T>) TYPE_ALIASES.get(key);
      } else {
        // 未注册别名时，退化为按全限定类名加载。
        value = (Class<T>) Resources.classForName(string);
      }
      return value;
    } catch (ClassNotFoundException e) {
      throw new TypeException("Could not resolve type alias '" + string + "'.  Cause: " + e, e);
    }
  }

  /**
   * 扫描指定包并注册其中所有可用类型，默认不过滤父类型。
   *
   * @param packageName 要扫描的包名
   */
  public void registerAliases(String packageName){
    registerAliases(packageName, Object.class);
  }

  /**
   * 扫描指定包并注册可赋值给给定父类型的类。
   *
   * @param packageName 要扫描的包名
   * @param superType 父类型过滤条件
   */
  public void registerAliases(String packageName, Class<?> superType){
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
    // 先按父类型过滤候选类，减少后续注册噪音。
    resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
    Set<Class<? extends Class<?>>> typeSet = resolverUtil.getClasses();
    for(Class<?> type : typeSet){
      // 仅注册可实例化的顶级类，跳过匿名类、接口与成员内部类（含 package-info 等场景）。
      if (!type.isAnonymousClass() && !type.isInterface() && !type.isMemberClass()) {
        registerAlias(type);
      }
    }
  }

  /**
   * 基于类型注册别名。
   * 默认使用简单类名；若存在 {@link Alias} 注解则优先使用注解值。
   *
   * @param type 要注册的类型
   */
  public void registerAlias(Class<?> type) {
    String alias = type.getSimpleName();
    Alias aliasAnnotation = type.getAnnotation(Alias.class);
    if (aliasAnnotation != null) {
      // 注解显式定义的别名优先级高于默认简单类名。
      alias = aliasAnnotation.value();
    } 
    registerAlias(alias, type);
  }

  /**
   * 显式注册别名与类型映射。
   * 别名按英文小写归一化存储；若同名别名已映射到不同类型，则拒绝覆盖并抛错。
   *
   * @param alias 别名，不可为 {@code null}
   * @param value 类型
   * @throws TypeException 当别名为空或与既有映射冲突时抛出
   */
  public void registerAlias(String alias, Class<?> value) {
    if (alias == null) {
      throw new TypeException("The parameter alias cannot be null");
    }
    // issue #748: 使用固定 Locale，避免区域化大小写规则带来的键不一致。
    String key = alias.toLowerCase(Locale.ENGLISH);
    // 同名别名只允许重复映射到同一类型，防止配置被静默覆盖。
    if (TYPE_ALIASES.containsKey(key) && TYPE_ALIASES.get(key) != null && !TYPE_ALIASES.get(key).equals(value)) {
      throw new TypeException("The alias '" + alias + "' is already mapped to the value '" + TYPE_ALIASES.get(key).getName() + "'.");
    }
    TYPE_ALIASES.put(key, value);
  }

  /**
   * 通过类名字符串注册别名。
   *
   * @param alias 别名
   * @param value 类型全限定类名
   * @throws TypeException 当类名无法加载时抛出
   */
  public void registerAlias(String alias, String value) {
    try {
      registerAlias(alias, Resources.classForName(value));
    } catch (ClassNotFoundException e) {
      throw new TypeException("Error registering type alias "+alias+" for "+value+". Cause: " + e, e);
    }
  }
  
  /**
   * 获取当前注册表的只读视图。
   *
   * @return 别名映射的不可变视图
   * @since 3.2.2
   */
  public Map<String, Class<?>> getTypeAliases() {
    return Collections.unmodifiableMap(TYPE_ALIASES);
  }

}
