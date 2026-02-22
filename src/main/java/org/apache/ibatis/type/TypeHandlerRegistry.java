/**
 *    Copyright 2009-2026 the original author or authors.
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

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.JapaneseDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.reflection.Jdk;

/**
 * TypeHandler 注册中心，负责维护 Java 类型、JDBC 类型与 {@link TypeHandler} 之间的映射关系。
 * <p>
 * 该类在 MyBatis 参数设置与结果映射阶段提供统一的处理器查找入口，并支持注解驱动、包扫描及按需实例化。
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public final class TypeHandlerRegistry {

  /**
   * JDBC 类型到处理器的直接映射，主要用于仅根据 JDBC 类型查找处理器的场景。
   */
  private final Map<JdbcType, TypeHandler<?>> JDBC_TYPE_HANDLER_MAP = new EnumMap<JdbcType, TypeHandler<?>>(JdbcType.class);
  /**
   * Java 类型到（JDBC 类型 -> 处理器）的二级映射，是类型匹配的核心索引。
   */
  private final Map<Type, Map<JdbcType, TypeHandler<?>>> TYPE_HANDLER_MAP = new ConcurrentHashMap<Type, Map<JdbcType, TypeHandler<?>>>();
  /**
   * 无法精确匹配时使用的兜底处理器。
   */
  private final TypeHandler<Object> UNKNOWN_TYPE_HANDLER = new UnknownTypeHandler(this);
  /**
   * 所有已注册处理器实例，键为处理器实现类，用于去重与对外只读暴露。
   */
  private final Map<Class<?>, TypeHandler<?>> ALL_TYPE_HANDLERS_MAP = new HashMap<Class<?>, TypeHandler<?>>();

  /**
   * 表示“该 Java 类型没有可用处理器”的哨兵映射，避免重复递归查找。
   */
  private static final Map<JdbcType, TypeHandler<?>> NULL_TYPE_HANDLER_MAP = Collections.emptyMap();

  /**
   * 枚举类型默认处理器，未显式指定时使用 {@link EnumTypeHandler}。
   */
  private Class<? extends TypeHandler> defaultEnumTypeHandler = EnumTypeHandler.class;

  /**
   * 初始化默认内建 TypeHandler 映射。
   */
  public TypeHandlerRegistry() {
    // 基本布尔类型映射。
    register(Boolean.class, new BooleanTypeHandler());
    register(boolean.class, new BooleanTypeHandler());
    register(JdbcType.BOOLEAN, new BooleanTypeHandler());
    register(JdbcType.BIT, new BooleanTypeHandler());

    // 基本数值类型映射。
    register(Byte.class, new ByteTypeHandler());
    register(byte.class, new ByteTypeHandler());
    register(JdbcType.TINYINT, new ByteTypeHandler());

    register(Short.class, new ShortTypeHandler());
    register(short.class, new ShortTypeHandler());
    register(JdbcType.SMALLINT, new ShortTypeHandler());

    register(Integer.class, new IntegerTypeHandler());
    register(int.class, new IntegerTypeHandler());
    register(JdbcType.INTEGER, new IntegerTypeHandler());

    register(Long.class, new LongTypeHandler());
    register(long.class, new LongTypeHandler());

    register(Float.class, new FloatTypeHandler());
    register(float.class, new FloatTypeHandler());
    register(JdbcType.FLOAT, new FloatTypeHandler());

    register(Double.class, new DoubleTypeHandler());
    register(double.class, new DoubleTypeHandler());
    register(JdbcType.DOUBLE, new DoubleTypeHandler());

    // 字符串与字符流相关映射。
    register(Reader.class, new ClobReaderTypeHandler());
    register(String.class, new StringTypeHandler());
    register(String.class, JdbcType.CHAR, new StringTypeHandler());
    register(String.class, JdbcType.CLOB, new ClobTypeHandler());
    register(String.class, JdbcType.VARCHAR, new StringTypeHandler());
    register(String.class, JdbcType.LONGVARCHAR, new ClobTypeHandler());
    register(String.class, JdbcType.NVARCHAR, new NStringTypeHandler());
    register(String.class, JdbcType.NCHAR, new NStringTypeHandler());
    register(String.class, JdbcType.NCLOB, new NClobTypeHandler());
    register(JdbcType.CHAR, new StringTypeHandler());
    register(JdbcType.VARCHAR, new StringTypeHandler());
    register(JdbcType.CLOB, new ClobTypeHandler());
    register(JdbcType.LONGVARCHAR, new ClobTypeHandler());
    register(JdbcType.NVARCHAR, new NStringTypeHandler());
    register(JdbcType.NCHAR, new NStringTypeHandler());
    register(JdbcType.NCLOB, new NClobTypeHandler());

    // 数组类型映射。
    register(Object.class, JdbcType.ARRAY, new ArrayTypeHandler());
    register(JdbcType.ARRAY, new ArrayTypeHandler());

    register(BigInteger.class, new BigIntegerTypeHandler());
    register(JdbcType.BIGINT, new LongTypeHandler());

    register(BigDecimal.class, new BigDecimalTypeHandler());
    register(JdbcType.REAL, new BigDecimalTypeHandler());
    register(JdbcType.DECIMAL, new BigDecimalTypeHandler());
    register(JdbcType.NUMERIC, new BigDecimalTypeHandler());

    // 二进制与大对象映射。
    register(InputStream.class, new BlobInputStreamTypeHandler());
    register(Byte[].class, new ByteObjectArrayTypeHandler());
    register(Byte[].class, JdbcType.BLOB, new BlobByteObjectArrayTypeHandler());
    register(Byte[].class, JdbcType.LONGVARBINARY, new BlobByteObjectArrayTypeHandler());
    register(byte[].class, new ByteArrayTypeHandler());
    register(byte[].class, JdbcType.BLOB, new BlobTypeHandler());
    register(byte[].class, JdbcType.LONGVARBINARY, new BlobTypeHandler());
    register(JdbcType.LONGVARBINARY, new BlobTypeHandler());
    register(JdbcType.BLOB, new BlobTypeHandler());

    // 兜底类型映射。
    register(Object.class, UNKNOWN_TYPE_HANDLER);
    register(Object.class, JdbcType.OTHER, UNKNOWN_TYPE_HANDLER);
    register(JdbcType.OTHER, UNKNOWN_TYPE_HANDLER);

    // 日期时间（java.util.Date 及 java.sql.*）映射。
    register(Date.class, new DateTypeHandler());
    register(Date.class, JdbcType.DATE, new DateOnlyTypeHandler());
    register(Date.class, JdbcType.TIME, new TimeOnlyTypeHandler());
    register(JdbcType.TIMESTAMP, new DateTypeHandler());
    register(JdbcType.DATE, new DateOnlyTypeHandler());
    register(JdbcType.TIME, new TimeOnlyTypeHandler());

    register(java.sql.Date.class, new SqlDateTypeHandler());
    register(java.sql.Time.class, new SqlTimeTypeHandler());
    register(java.sql.Timestamp.class, new SqlTimestampTypeHandler());

    // 在 JDK 支持 java.time 时注册 JSR-310 类型处理器。
    if (Jdk.dateAndTimeApiExists) {
      this.register(Instant.class, InstantTypeHandler.class);
      this.register(LocalDateTime.class, LocalDateTimeTypeHandler.class);
      this.register(LocalDate.class, LocalDateTypeHandler.class);
      this.register(LocalTime.class, LocalTimeTypeHandler.class);
      this.register(OffsetDateTime.class, OffsetDateTimeTypeHandler.class);
      this.register(OffsetTime.class, OffsetTimeTypeHandler.class);
      this.register(ZonedDateTime.class, ZonedDateTimeTypeHandler.class);
      this.register(Month.class, MonthTypeHandler.class);
      this.register(Year.class, YearTypeHandler.class);
      this.register(YearMonth.class, YearMonthTypeHandler.class);
      this.register(JapaneseDate.class, JapaneseDateTypeHandler.class);
    }

    // issue #273: 补充字符类型处理器。
    register(Character.class, new CharacterTypeHandler());
    register(char.class, new CharacterTypeHandler());
  }

  /**
   * 设置枚举类型默认处理器实现。
   *
   * @param typeHandler 枚举默认处理器类型
   * @since 3.4.5
   */
  public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
    this.defaultEnumTypeHandler = typeHandler;
  }

  /**
   * 判断指定 Java 类型是否存在可用 TypeHandler。
   *
   * @param javaType Java 类型
   * @return 存在可用处理器则为 true
   */
  public boolean hasTypeHandler(Class<?> javaType) {
    return hasTypeHandler(javaType, null);
  }

  /**
   * 判断类型引用是否存在可用 TypeHandler。
   *
   * @param javaTypeReference 类型引用
   * @return 存在可用处理器则为 true
   */
  public boolean hasTypeHandler(TypeReference<?> javaTypeReference) {
    return hasTypeHandler(javaTypeReference, null);
  }

  /**
   * 判断 Java 类型与 JDBC 类型组合是否存在可用 TypeHandler。
   *
   * @param javaType Java 类型
   * @param jdbcType JDBC 类型，可为空
   * @return 存在可用处理器则为 true
   */
  public boolean hasTypeHandler(Class<?> javaType, JdbcType jdbcType) {
    return javaType != null && getTypeHandler((Type) javaType, jdbcType) != null;
  }

  /**
   * 判断类型引用与 JDBC 类型组合是否存在可用 TypeHandler。
   *
   * @param javaTypeReference 类型引用
   * @param jdbcType JDBC 类型，可为空
   * @return 存在可用处理器则为 true
   */
  public boolean hasTypeHandler(TypeReference<?> javaTypeReference, JdbcType jdbcType) {
    return javaTypeReference != null && getTypeHandler(javaTypeReference, jdbcType) != null;
  }

  /**
   * 按处理器类型获取已注册实例。
   *
   * @param handlerType 处理器实现类
   * @return 已注册处理器实例；若不存在则返回 null
   */
  public TypeHandler<?> getMappingTypeHandler(Class<? extends TypeHandler<?>> handlerType) {
    return ALL_TYPE_HANDLERS_MAP.get(handlerType);
  }

  /**
   * 按 Java 类型获取默认 TypeHandler。
   *
   * @param type Java 类型
   * @param <T> 类型参数
   * @return 匹配的处理器；若不存在则返回 null
   */
  public <T> TypeHandler<T> getTypeHandler(Class<T> type) {
    return getTypeHandler((Type) type, null);
  }

  /**
   * 按类型引用获取默认 TypeHandler。
   *
   * @param javaTypeReference 类型引用
   * @param <T> 类型参数
   * @return 匹配的处理器；若不存在则返回 null
   */
  public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference) {
    return getTypeHandler(javaTypeReference, null);
  }

  /**
   * 按 JDBC 类型获取处理器。
   *
   * @param jdbcType JDBC 类型
   * @return 匹配的处理器；若不存在则返回 null
   */
  public TypeHandler<?> getTypeHandler(JdbcType jdbcType) {
    return JDBC_TYPE_HANDLER_MAP.get(jdbcType);
  }

  /**
   * 按 Java 类型与 JDBC 类型获取处理器。
   *
   * @param type Java 类型
   * @param jdbcType JDBC 类型，可为空
   * @param <T> 类型参数
   * @return 匹配的处理器；若不存在则返回 null
   */
  public <T> TypeHandler<T> getTypeHandler(Class<T> type, JdbcType jdbcType) {
    return getTypeHandler((Type) type, jdbcType);
  }

  /**
   * 按类型引用与 JDBC 类型获取处理器。
   *
   * @param javaTypeReference 类型引用
   * @param jdbcType JDBC 类型，可为空
   * @param <T> 类型参数
   * @return 匹配的处理器；若不存在则返回 null
   */
  public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference, JdbcType jdbcType) {
    return getTypeHandler(javaTypeReference.getRawType(), jdbcType);
  }

  /**
   * 内部统一的处理器查找逻辑。
   *
   * @param type Java 类型
   * @param jdbcType JDBC 类型，可为空
   * @param <T> 类型参数
   * @return 匹配的处理器；若不存在则返回 null
   */
  @SuppressWarnings("unchecked")
  private <T> TypeHandler<T> getTypeHandler(Type type, JdbcType jdbcType) {
    // ParamMap 使用动态参数键，不参与 TypeHandler 推断。
    if (ParamMap.class.equals(type)) {
      return null;
    }
    Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = getJdbcHandlerMap(type);
    TypeHandler<?> handler = null;
    if (jdbcHandlerMap != null) {
      // 优先使用精确 JDBC 类型匹配。
      handler = jdbcHandlerMap.get(jdbcType);
      if (handler == null) {
        // 其次尝试未指定 JDBC 类型（null）的默认处理器。
        handler = jdbcHandlerMap.get(null);
      }
      if (handler == null) {
        // #591: 若映射内所有处理器类型一致，则可安全选取唯一处理器。
        handler = pickSoleHandler(jdbcHandlerMap);
      }
    }
    // 由调用方的 type 约束泛型边界。
    return (TypeHandler<T>) handler;
  }

  /**
   * 获取 Java 类型对应的 JDBC 处理器映射，并带有缓存与继承链查找能力。
   *
   * @param type Java 类型
   * @return JDBC 类型到处理器映射；若不存在则返回 null
   */
  private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMap(Type type) {
    Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(type);
    // 命中空映射哨兵，表示此前已确认不存在处理器。
    if (NULL_TYPE_HANDLER_MAP.equals(jdbcHandlerMap)) {
      return null;
    }
    if (jdbcHandlerMap == null && type instanceof Class) {
      Class<?> clazz = (Class<?>) type;
      if (clazz.isEnum()) {
        // 枚举优先从其接口继承处理器；找不到时注册默认枚举处理器。
        jdbcHandlerMap = getJdbcHandlerMapForEnumInterfaces(clazz, clazz);
        if (jdbcHandlerMap == null) {
          register(clazz, getInstance(clazz, defaultEnumTypeHandler));
          return TYPE_HANDLER_MAP.get(clazz);
        }
      } else {
        // 非枚举类型沿父类链向上查找。
        jdbcHandlerMap = getJdbcHandlerMapForSuperclass(clazz);
      }
    }
    // 将“未命中”结果也写入缓存，减少后续反射/递归成本。
    TYPE_HANDLER_MAP.put(type, jdbcHandlerMap == null ? NULL_TYPE_HANDLER_MAP : jdbcHandlerMap);
    return jdbcHandlerMap;
  }

  /**
   * 为枚举类型查找其接口上注册的处理器映射。
   *
   * @param clazz 当前待查找接口来源类型
   * @param enumClazz 最终枚举类型
   * @return 复制并绑定到枚举类型后的处理器映射；若不存在则返回 null
   */
  private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMapForEnumInterfaces(Class<?> clazz, Class<?> enumClazz) {
    for (Class<?> iface : clazz.getInterfaces()) {
      Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(iface);
      if (jdbcHandlerMap == null) {
        jdbcHandlerMap = getJdbcHandlerMapForEnumInterfaces(iface, enumClazz);
      }
      if (jdbcHandlerMap != null) {
        // 找到接口级处理器后，为具体枚举类型创建新实例，避免共享状态混淆。
        HashMap<JdbcType, TypeHandler<?>> newMap = new HashMap<JdbcType, TypeHandler<?>>();
        for (Entry<JdbcType, TypeHandler<?>> entry : jdbcHandlerMap.entrySet()) {
          // 枚举处理器通常需要接收“具体枚举类型”作为构造参数。
          newMap.put(entry.getKey(), getInstance(enumClazz, entry.getValue().getClass()));
        }
        return newMap;
      }
    }
    return null;
  }

  /**
   * 递归查找父类链上的处理器映射。
   *
   * @param clazz 当前类型
   * @return 首个命中的父类处理器映射；若不存在则返回 null
   */
  private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMapForSuperclass(Class<?> clazz) {
    Class<?> superclass =  clazz.getSuperclass();
    if (superclass == null || Object.class.equals(superclass)) {
      return null;
    }
    Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(superclass);
    if (jdbcHandlerMap != null) {
      return jdbcHandlerMap;
    } else {
      return getJdbcHandlerMapForSuperclass(superclass);
    }
  }

  /**
   * 当映射内仅注册了同一种处理器实现时，返回该唯一处理器。
   *
   * @param jdbcHandlerMap 待检查映射
   * @return 唯一处理器；若存在多种实现则返回 null
   */
  private TypeHandler<?> pickSoleHandler(Map<JdbcType, TypeHandler<?>> jdbcHandlerMap) {
    TypeHandler<?> soleHandler = null;
    for (TypeHandler<?> handler : jdbcHandlerMap.values()) {
      if (soleHandler == null) {
        soleHandler = handler;
      } else if (!handler.getClass().equals(soleHandler.getClass())) {
        // 存在多个不同处理器实现，无法推断唯一默认值。
        return null;
      }
    }
    return soleHandler;
  }

  /**
   * 获取未知类型兜底处理器。
   *
   * @return UnknownTypeHandler 实例
   */
  public TypeHandler<Object> getUnknownTypeHandler() {
    return UNKNOWN_TYPE_HANDLER;
  }

  /**
   * 注册 JDBC 类型到处理器的映射。
   *
   * @param jdbcType JDBC 类型
   * @param handler 处理器实例
   */
  public void register(JdbcType jdbcType, TypeHandler<?> handler) {
    JDBC_TYPE_HANDLER_MAP.put(jdbcType, handler);
  }

  //
  // REGISTER INSTANCE
  //

  /**
   * 仅按处理器实例注册。
   * <p>
   * 优先读取 {@link MappedTypes}，其次尝试从 {@link TypeReference} 推断 Java 类型，
   * 若均失败则按“未指定 Java 类型”的方式注册。
   *
   * @param typeHandler 处理器实例
   * @param <T> Java 类型参数
   */
  @SuppressWarnings("unchecked")
  public <T> void register(TypeHandler<T> typeHandler) {
    boolean mappedTypeFound = false;
    MappedTypes mappedTypes = typeHandler.getClass().getAnnotation(MappedTypes.class);
    if (mappedTypes != null) {
      // 明确声明了可处理 Java 类型，按声明逐一注册。
      for (Class<?> handledType : mappedTypes.value()) {
        register(handledType, typeHandler);
        mappedTypeFound = true;
      }
    }
    // @since 3.1.0：若处理器本身是 TypeReference，则尝试自动推断泛型原始类型。
    if (!mappedTypeFound && typeHandler instanceof TypeReference) {
      try {
        TypeReference<T> typeReference = (TypeReference<T>) typeHandler;
        register(typeReference.getRawType(), typeHandler);
        mappedTypeFound = true;
      } catch (Throwable t) {
        // 用户自定义 TypeReference 可能存在不可赋值关系，忽略并退回兜底注册。
      }
    }
    if (!mappedTypeFound) {
      register((Class<T>) null, typeHandler);
    }
  }

  /**
   * 按 Java 类型 + 处理器实例注册。
   *
   * @param javaType Java 类型
   * @param typeHandler 处理器实例
   * @param <T> Java 类型参数
   */
  public <T> void register(Class<T> javaType, TypeHandler<? extends T> typeHandler) {
    register((Type) javaType, typeHandler);
  }

  /**
   * 按通用 Type + 处理器实例注册。
   * <p>
   * 若处理器声明了 {@link MappedJdbcTypes}，将按 JDBC 类型逐一登记；否则登记到 null 键作为默认处理器。
   *
   * @param javaType Java 类型（可为空）
   * @param typeHandler 处理器实例
   * @param <T> Java 类型参数
   */
  private <T> void register(Type javaType, TypeHandler<? extends T> typeHandler) {
    MappedJdbcTypes mappedJdbcTypes = typeHandler.getClass().getAnnotation(MappedJdbcTypes.class);
    if (mappedJdbcTypes != null) {
      for (JdbcType handledJdbcType : mappedJdbcTypes.value()) {
        register(javaType, handledJdbcType, typeHandler);
      }
      if (mappedJdbcTypes.includeNullJdbcType()) {
        register(javaType, null, typeHandler);
      }
    } else {
      register(javaType, null, typeHandler);
    }
  }

  /**
   * 按类型引用 + 处理器实例注册。
   *
   * @param javaTypeReference 类型引用
   * @param handler 处理器实例
   * @param <T> Java 类型参数
   */
  public <T> void register(TypeReference<T> javaTypeReference, TypeHandler<? extends T> handler) {
    register(javaTypeReference.getRawType(), handler);
  }

  /**
   * 按 Java 类型 + JDBC 类型 + 处理器实例注册。
   *
   * @param type Java 类型
   * @param jdbcType JDBC 类型
   * @param handler 处理器实例
   * @param <T> Java 类型参数
   */
  public <T> void register(Class<T> type, JdbcType jdbcType, TypeHandler<? extends T> handler) {
    register((Type) type, jdbcType, handler);
  }

  /**
   * 按通用 Type + JDBC 类型 + 处理器实例注册。
   *
   * @param javaType Java 类型（可为空）
   * @param jdbcType JDBC 类型（可为空，表示默认）
   * @param handler 处理器实例
   */
  private void register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) {
    if (javaType != null) {
      Map<JdbcType, TypeHandler<?>> map = TYPE_HANDLER_MAP.get(javaType);
      if (map == null || map == NULL_TYPE_HANDLER_MAP) {
        // 首次注册或此前被标记为“空映射”时，重新创建可写映射。
        map = new HashMap<JdbcType, TypeHandler<?>>();
        TYPE_HANDLER_MAP.put(javaType, map);
      }
      map.put(jdbcType, handler);
    }
    // 记录全局处理器实例索引，便于后续按处理器类型查询。
    ALL_TYPE_HANDLERS_MAP.put(handler.getClass(), handler);
  }

  //
  // REGISTER CLASS
  //

  /**
   * 仅按处理器类型注册。
   * <p>
   * 若处理器类型声明了 {@link MappedTypes}，则对每个 Java 类型分别注册；
   * 否则直接实例化处理器并交由实例注册流程处理。
   *
   * @param typeHandlerClass 处理器类型
   */
  public void register(Class<?> typeHandlerClass) {
    boolean mappedTypeFound = false;
    MappedTypes mappedTypes = typeHandlerClass.getAnnotation(MappedTypes.class);
    if (mappedTypes != null) {
      for (Class<?> javaTypeClass : mappedTypes.value()) {
        register(javaTypeClass, typeHandlerClass);
        mappedTypeFound = true;
      }
    }
    if (!mappedTypeFound) {
      register(getInstance(null, typeHandlerClass));
    }
  }

  /**
   * 通过类名注册 Java 类型与处理器类型。
   *
   * @param javaTypeClassName Java 类型全限定名
   * @param typeHandlerClassName 处理器类型全限定名
   * @throws ClassNotFoundException 类加载失败时抛出
   */
  public void register(String javaTypeClassName, String typeHandlerClassName) throws ClassNotFoundException {
    register(Resources.classForName(javaTypeClassName), Resources.classForName(typeHandlerClassName));
  }

  /**
   * 按 Java 类型 + 处理器类型注册。
   *
   * @param javaTypeClass Java 类型
   * @param typeHandlerClass 处理器类型
   */
  public void register(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
    register(javaTypeClass, getInstance(javaTypeClass, typeHandlerClass));
  }

  /**
   * 按 Java 类型 + JDBC 类型 + 处理器类型注册。
   *
   * @param javaTypeClass Java 类型
   * @param jdbcType JDBC 类型
   * @param typeHandlerClass 处理器类型
   */
  public void register(Class<?> javaTypeClass, JdbcType jdbcType, Class<?> typeHandlerClass) {
    register(javaTypeClass, jdbcType, getInstance(javaTypeClass, typeHandlerClass));
  }

  /**
   * 创建 TypeHandler 实例（也被构建器流程复用）。
   * <p>
   * 优先尝试使用 {@code (Class)} 构造器，以便向枚举等处理器传递目标 Java 类型；
   * 若不存在则退回无参构造器。
   *
   * @param javaTypeClass 目标 Java 类型，可为空
   * @param typeHandlerClass 处理器类型
   * @param <T> Java 类型参数
   * @return 处理器实例
   */
  @SuppressWarnings("unchecked")
  public <T> TypeHandler<T> getInstance(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
    if (javaTypeClass != null) {
      try {
        Constructor<?> c = typeHandlerClass.getConstructor(Class.class);
        return (TypeHandler<T>) c.newInstance(javaTypeClass);
      } catch (NoSuchMethodException ignored) {
        // 忽略并继续尝试无参构造器。
      } catch (Exception e) {
        throw new TypeException("Failed invoking constructor for handler " + typeHandlerClass, e);
      }
    }
    try {
      Constructor<?> c = typeHandlerClass.getConstructor();
      return (TypeHandler<T>) c.newInstance();
    } catch (Exception e) {
      throw new TypeException("Unable to find a usable constructor for " + typeHandlerClass, e);
    }
  }

  /**
   * 扫描指定包并注册其中的 TypeHandler 实现类。
   *
   * @param packageName 包名
   */
  public void register(String packageName) {
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
    resolverUtil.find(new ResolverUtil.IsA(TypeHandler.class), packageName);
    Set<Class<? extends Class<?>>> handlerSet = resolverUtil.getClasses();
    for (Class<?> type : handlerSet) {
      // 忽略匿名类、接口（含 package-info）及抽象类，仅注册可实例化实现。
      if (!type.isAnonymousClass() && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
        register(type);
      }
    }
  }

  /**
   * 获取当前已注册处理器集合（只读视图）。
   *
   * @since 3.2.2
   * @return 已注册处理器集合
   */
  public Collection<TypeHandler<?>> getTypeHandlers() {
    return Collections.unmodifiableCollection(ALL_TYPE_HANDLERS_MAP.values());
  }

}
