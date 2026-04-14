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

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mapper 方法调用的封装类，负责协调 SQL 命令执行和参数解析。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 * @author Kazuki Shimizu
 */
public class MapperMethod {

  /**
   * SQL 命令信息，包含名称和类型
   */
  private final SqlCommand command;
  /**
   * 方法签名信息，包含返回类型和参数解析器
   */
  private final MethodSignature method;

  /**
   * 构造方法，初始化 SQL 命令和方法签名。
   *
   * @param mapperInterface mapper 接口类
   * @param method          mapper 方法
   * @param config          MyBatis 配置
   */
  public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
    this.command = new SqlCommand(config, mapperInterface, method);
    this.method = new MethodSignature(config, mapperInterface, method);
  }

  /**
   * 执行 mapper 方法，根据 SQL 命令类型分发到对应的处理逻辑。
   *
   * @param sqlSession SqlSession 实例
   * @param args       方法参数
   * @return 执行结果
   */
  public Object execute(SqlSession sqlSession, Object[] args) {
    Object result;
    switch (command.getType()) {
      case INSERT: {
        Object param = method.convertArgsToSqlCommandParam( args);
        result = rowCountResult(sqlSession.insert(command.getName(), param));
        break;
      }
      case UPDATE: {
        Object param = method.convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.update(command.getName(), param));
        break;
      }
      case DELETE: {
        Object param = method.convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.delete(command.getName(), param));
        break;
      }
      case SELECT:
        // 根据返回类型选择不同的处理方式
        if (method.returnsVoid() && method.hasResultHandler()) {
          executeWithResultHandler(sqlSession, args);
          result = null;
        } else if (method.returnsMany()) {
          result = executeForMany(sqlSession, args);
        } else if (method.returnsMap()) {
          result = executeForMap(sqlSession, args);
        } else if (method.returnsCursor()) {
          result = executeForCursor(sqlSession, args);
        } else {
          Object param = method.convertArgsToSqlCommandParam(args);
          result = sqlSession.selectOne(command.getName(), param);
          // 返回 Optional 类型的处理
          if (method.returnsOptional()
            && (result == null || !method.getReturnType().equals(result.getClass()))) {
            result = Optional.ofNullable(result);
          }
        }
        break;
      case FLUSH:
        result = sqlSession.flushStatements();
        break;
      default:
        throw new BindingException("Unknown execution method for: " + command.getName());
    }
    // 基础类型不能返回 null
    if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
      throw new BindingException("Mapper method '" + command.getName()
        + "' attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
    }
    return result;
  }

  /**
   * 将影响的行数转换为方法期望的返回类型。
   *
   * @param rowCount 影响的行数
   * @return 转换后的结果
   */
  private Object rowCountResult(int rowCount) {
    final Object result;
    if (method.returnsVoid()) {
      result = null;
    } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
      result = rowCount;
    } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
      result = (long) rowCount;
    } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
      // 返回 boolean 表示是否有数据被影响
      result = rowCount > 0;
    } else {
      throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
    }
    return result;
  }

  /**
   * 使用 ResultHandler 处理查询结果。
   *
   * @param sqlSession SqlSession 实例
   * @param args       方法参数
   */
  private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
    MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
    // 验证是否配置了结果映射
    if (!StatementType.CALLABLE.equals(ms.getStatementType())
      && void.class.equals(ms.getResultMaps().get(0).getType())) {
      throw new BindingException("method " + command.getName()
        + " needs either a @ResultMap annotation, a @ResultType annotation,"
        + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
    }
    Object param = method.convertArgsToSqlCommandParam(args);
    // 分页参数处理
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
    } else {
      sqlSession.select(command.getName(), param, method.extractResultHandler(args));
    }
  }

  /**
   * 执行返回集合类型的查询。
   *
   * @param sqlSession SqlSession 实例
   * @param args       方法参数
   * @return 查询结果列表或数组
   */
  private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
    List<E> result;
    Object param = method.convertArgsToSqlCommandParam(args);
    // 分页参数处理
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.selectList(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.selectList(command.getName(), param);
    }
    // 返回类型转换：List -> 数组或自定义集合
    if (!method.getReturnType().isAssignableFrom(result.getClass())) {
      if (method.getReturnType().isArray()) {
        return convertToArray(result);
      } else {
        return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
      }
    }
    return result;
  }

  /**
   * 执行返回游标类型的查询。
   *
   * @param sqlSession SqlSession 实例
   * @param args       方法参数
   * @return 游标结果
   */
  private <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
    Cursor<T> result;
    Object param = method.convertArgsToSqlCommandParam(args);
    // 分页参数处理
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.selectCursor(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.selectCursor(command.getName(), param);
    }
    return result;
  }

  /**
   * 将 List 转换为声明的集合类型（如 Set、Collection 实现类）。
   *
   * @param config MyBatis 配置
   * @param list   查询结果列表
   * @return 目标集合类型实例
   */
  private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
    Object collection = config.getObjectFactory().create(method.getReturnType());
    MetaObject metaObject = config.newMetaObject(collection);
    metaObject.addAll(list);
    return collection;
  }

  /**
   * 将 List 转换为数组类型。
   *
   * @param list 查询结果列表
   * @return 目标数组类型实例
   */
  @SuppressWarnings("unchecked")
  private <E> Object convertToArray(List<E> list) {
    Class<?> arrayComponentType = method.getReturnType().getComponentType();
    Object array = Array.newInstance(arrayComponentType, list.size());
    // 基础类型数组需要逐个设置元素
    if (arrayComponentType.isPrimitive()) {
      for (int i = 0; i < list.size(); i++) {
        Array.set(array, i, list.get(i));
      }
      return array;
    } else {
      return list.toArray((E[]) array);
    }
  }

  /**
   * 执行返回 Map 类型的查询。
   *
   * @param sqlSession SqlSession 实例
   * @param args       方法参数
   * @return 查询结果 Map
   */
  private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
    Map<K, V> result;
    Object param = method.convertArgsToSqlCommandParam(args);
    // 分页参数处理
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.selectMap(command.getName(), param, method.getMapKey(), rowBounds);
    } else {
      result = sqlSession.selectMap(command.getName(), param, method.getMapKey());
    }
    return result;
  }

  /**
   * 参数 Map 封装类，提供更友好的参数缺失异常信息。
   */
  public static class ParamMap<V> extends HashMap<String, V> {

    private static final long serialVersionUID = -2212268410512043556L;

    @Override
    public V get(Object key) {
      if (!super.containsKey(key)) {
        throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
      }
      return super.get(key);
    }

  }

  /**
   * SQL 命令封装类，负责解析和存储 mapper 方法对应的 SQL 语句信息。
   */
  public static class SqlCommand {

    /**
     * SQL 语句 ID
     */
    private final String name;
    /**
     * SQL 命令类型（INSERT/UPDATE/DELETE/SELECT/FLUSH）
     */
    private final SqlCommandType type;

    /**
     * 构造方法，解析 mapper 方法对应的 MappedStatement。
     * <p>
     * 初始化sqlCommand本质上是从已经初始化的configuration获取对应的MappedStatement（真正执行sql的类）信息
     *
     * @param configuration   MyBatis 配置
     * @param mapperInterface mapper 接口类
     * @param method          mapper 方法
     */
    public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
      final String methodName = method.getName();
      final Class<?> declaringClass = method.getDeclaringClass();
      MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass,
        configuration);
      if (ms == null) {
        // 检查是否是 Flush 注解标记的方法
        if (method.getAnnotation(Flush.class) != null) {
          name = null;
          type = SqlCommandType.FLUSH;
        } else {
          throw new BindingException("Invalid bound statement (not found): "
            + mapperInterface.getName() + "." + methodName);
        }
      } else {
        name = ms.getId();
        type = ms.getSqlCommandType();
        if (type == SqlCommandType.UNKNOWN) {
          throw new BindingException("Unknown execution method for: " + name);
        }
      }
    }

    /**
     * 获取 SQL 语句 ID。
     *
     * @return SQL 语句 ID
     */
    public String getName() {
      return name;
    }

    /**
     * 获取 SQL 命令类型。
     *
     * @return SQL 命令类型
     */
    public SqlCommandType getType() {
      return type;
    }

    /**
     * 解析 mapper 方法对应的 MappedStatement，支持接口继承查找。
     *
     * @param mapperInterface mapper 接口类
     * @param methodName      方法名
     * @param declaringClass  声明类
     * @param configuration   MyBatis 配置
     * @return MappedStatement，不存在则返回 null
     */
    private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
                                                   Class<?> declaringClass, Configuration configuration) {
      // 在初始化sqlcommand之前configuration就已经初始化，所以这里如果mapper已有的方法在configuration中是能够找到的
      // 所以在判断方法声明是当前接口时，直接返回null，证明这个方法不可用，即没有配置sql
      String statementId = mapperInterface.getName() + "." + methodName;
      if (configuration.hasStatement(statementId)) {
        return configuration.getMappedStatement(statementId);
      } else if (mapperInterface.equals(declaringClass)) {
        return null;
      }
      // 遍历父接口查找 MappedStatement
      for (Class<?> superInterface : mapperInterface.getInterfaces()) {
        if (declaringClass.isAssignableFrom(superInterface)) {
          MappedStatement ms = resolveMappedStatement(superInterface, methodName,
            declaringClass, configuration);
          if (ms != null) {
            return ms;
          }
        }
      }
      return null;
    }
  }

  /**
   * 方法签名封装类，负责解析方法返回类型和特殊参数索引。
   */
  public static class MethodSignature {

    /**
     * 是否返回集合或数组
     */
    private final boolean returnsMany;
    /**
     * 是否返回 Map
     */
    private final boolean returnsMap;
    /**
     * 是否返回 void
     */
    private final boolean returnsVoid;
    /**
     * 是否返回游标
     */
    private final boolean returnsCursor;
    /**
     * 是否返回 Optional
     */
    private final boolean returnsOptional;
    /**
     * 方法返回类型
     */
    private final Class<?> returnType;
    /**
     * Map 的 key 字段名
     */
    private final String mapKey;
    /**
     * ResultHandler 参数索引
     */
    private final Integer resultHandlerIndex;
    /**
     * RowBounds 参数索引
     */
    private final Integer rowBoundsIndex;
    /**
     * 参数名称解析器
     */
    private final ParamNameResolver paramNameResolver;

    /**
     * 构造方法，解析方法返回类型和特殊参数。
     *
     * @param configuration   MyBatis 配置
     * @param mapperInterface mapper 接口类
     * @param method          mapper 方法
     */
    public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
      // 解析泛型返回类型
      Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
      if (resolvedReturnType instanceof Class<?>) {
        this.returnType = (Class<?>) resolvedReturnType;
      } else if (resolvedReturnType instanceof ParameterizedType) {
        this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
      } else {
        this.returnType = method.getReturnType();
      }
      this.returnsVoid = void.class.equals(this.returnType);
      this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
      this.returnsCursor = Cursor.class.equals(this.returnType);
      this.returnsOptional = Optional.class.equals(this.returnType);
      this.mapKey = getMapKey(method);
      this.returnsMap = this.mapKey != null;
      this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
      this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
      this.paramNameResolver = new ParamNameResolver(configuration, method);
    }

    /**
     * 将方法参数转换为 SQL 命令参数。
     *
     * @param args 方法参数数组
     * @return SQL 命令参数
     */
    public Object convertArgsToSqlCommandParam(Object[] args) {
      return paramNameResolver.getNamedParams(args);
    }

    /**
     * 判断方法是否有分页参数。
     *
     * @return 是否有分页参数
     */
    public boolean hasRowBounds() {
      return rowBoundsIndex != null;
    }

    /**
     * 从方法参数中提取分页参数。
     *
     * @param args 方法参数数组
     * @return RowBounds 实例，不存在则返回 null
     */
    public RowBounds extractRowBounds(Object[] args) {
      return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
    }

    /**
     * 判断方法是否有 ResultHandler 参数。
     *
     * @return 是否有 ResultHandler 参数
     */
    public boolean hasResultHandler() {
      return resultHandlerIndex != null;
    }

    /**
     * 从方法参数中提取 ResultHandler。
     *
     * @param args 方法参数数组
     * @return ResultHandler 实例，不存在则返回 null
     */
    public ResultHandler extractResultHandler(Object[] args) {
      return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
    }

    /**
     * 获取方法返回类型。
     *
     * @return 方法返回类型
     */
    public Class<?> getReturnType() {
      return returnType;
    }

    /**
     * 判断是否返回集合或数组。
     *
     * @return 是否返回集合或数组
     */
    public boolean returnsMany() {
      return returnsMany;
    }

    /**
     * 判断是否返回 Map。
     *
     * @return 是否返回 Map
     */
    public boolean returnsMap() {
      return returnsMap;
    }

    /**
     * 判断是否返回 void。
     *
     * @return 是否返回 void
     */
    public boolean returnsVoid() {
      return returnsVoid;
    }

    /**
     * 判断是否返回游标。
     *
     * @return 是否返回游标
     */
    public boolean returnsCursor() {
      return returnsCursor;
    }

    /**
     * return whether return type is {@code java.util.Optional}.
     *
     * @return return {@code true}, if return type is {@code java.util.Optional}
     * @since 3.5.0
     */
    public boolean returnsOptional() {
      return returnsOptional;
    }

    /**
     * 获取指定类型参数的唯一索引。
     *
     * @param method    方法
     * @param paramType 参数类型
     * @return 参数索引，不存在则返回 null
     */
    private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
      Integer index = null;
      final Class<?>[] argTypes = method.getParameterTypes();
      for (int i = 0; i < argTypes.length; i++) {
        if (paramType.isAssignableFrom(argTypes[i])) {
          if (index == null) {
            index = i;
          } else {
            throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
          }
        }
      }
      return index;
    }

    /**
     * 获取 Map 的 key 字段名。
     *
     * @return Map key 字段名
     */
    public String getMapKey() {
      return mapKey;
    }

    /**
     * 从方法注解中解析 Map 的 key 字段名。
     *
     * @param method mapper 方法
     * @return Map key 字段名，不存在则返回 null
     */
    private String getMapKey(Method method) {
      String mapKey = null;
      if (Map.class.isAssignableFrom(method.getReturnType())) {
        final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
        if (mapKeyAnnotation != null) {
          mapKey = mapKeyAnnotation.value();
        }
      }
      return mapKey;
    }
  }

}
