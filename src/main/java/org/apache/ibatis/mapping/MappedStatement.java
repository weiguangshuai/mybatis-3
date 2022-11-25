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
package org.apache.ibatis.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * 表示映射的SQL语句，包含SQL源码、参数映射、结果映射等执行所需的全部配置信息。
 *
 * @author Clinton Begin
 */
public final class MappedStatement {

  /** SQL映射文件或注解的资源路径 */
  private String resource;
  /** 全局配置对象 */
  private Configuration configuration;
  /** SQL语句的唯一标识，通常为 mapper接口方法全限定名 */
  private String id;
  /** JDBC fetch size，设置每次查询抓取的记录数 */
  private Integer fetchSize;
  /** SQL执行超时时间（秒） */
  private Integer timeout;
  /** 语句类型：PREPARED、CALLABLE、STATEMENT */
  private StatementType statementType;
  /** 结果集类型：FORWARD_ONLY、SCROLL_SENSITIVE、SCROLL_INSENSITIVE */
  private ResultSetType resultSetType;
  /** SQL源码对象，包含解析后的SQL和参数绑定信息 */
  private SqlSource sqlSource;
  /** 二级缓存对象 */
  private Cache cache;
  /** 参数映射配置 */
  private ParameterMap parameterMap;
  /** 结果映射配置列表 */
  private List<ResultMap> resultMaps;
  /** 是否在执行前清除二级缓存 */
  private boolean flushCacheRequired;
  /** 是否启用二级缓存 */
  private boolean useCache;
  /** 结果是否有序（影响缓存key） */
  private boolean resultOrdered;
  /** SQL命令类型：INSERT、UPDATE、DELETE、SELECT */
  private SqlCommandType sqlCommandType;
  /** 主键生成器 */
  private KeyGenerator keyGenerator;
  /** 主键属性名数组（用于自动填充） */
  private String[] keyProperties;
  /** 主键列名数组（用于自动填充） */
  private String[] keyColumns;
  /** 是否包含嵌套结果映射 */
  private boolean hasNestedResultMaps;
  /** 数据库产品标识，用于多数据库支持 */
  private String databaseId;
  /** 日志记录器 */
  private Log statementLog;
  /** 脚本语言驱动 */
  private LanguageDriver lang;
  /** 结果集名称数组（用于多结果集映射） */
  private String[] resultSets;

  MappedStatement() {
    // constructor disabled
  }

  /** MappedStatement构建器，采用Builder模式创建语句对象 */
  public static class Builder {
    private MappedStatement mappedStatement = new MappedStatement();

    /**
     * 构造Builder实例。
     *
     * @param configuration 全局配置
     * @param id 语句标识
     * @param sqlSource SQL源码
     * @param sqlCommandType SQL命令类型
     */
    public Builder(Configuration configuration, String id, SqlSource sqlSource, SqlCommandType sqlCommandType) {
      mappedStatement.configuration = configuration;
      mappedStatement.id = id;
      mappedStatement.sqlSource = sqlSource;
      mappedStatement.statementType = StatementType.PREPARED;
      mappedStatement.resultSetType = ResultSetType.DEFAULT;
      mappedStatement.parameterMap = new ParameterMap.Builder(configuration, "defaultParameterMap", null, new ArrayList<>()).build();
      mappedStatement.resultMaps = new ArrayList<>();
      mappedStatement.sqlCommandType = sqlCommandType;
      mappedStatement.keyGenerator = configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType) ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
      String logId = id;
      if (configuration.getLogPrefix() != null) {
        logId = configuration.getLogPrefix() + id;
      }
      mappedStatement.statementLog = LogFactory.getLog(logId);
      mappedStatement.lang = configuration.getDefaultScriptingLanguageInstance();
    }

    /**
     * 设置资源路径。
     *
     * @param resource SQL映射文件或注解的资源路径
     * @return 当前Builder实例
     */
    public Builder resource(String resource) {
      mappedStatement.resource = resource;
      return this;
    }

    /**
     * 获取语句标识。
     *
     * @return 语句标识
     */
    public String id() {
      return mappedStatement.id;
    }

    /**
     * 设置参数映射。
     *
     * @param parameterMap 参数映射配置
     * @return 当前Builder实例
     */
    public Builder parameterMap(ParameterMap parameterMap) {
      mappedStatement.parameterMap = parameterMap;
      return this;
    }

    /**
     * 设置结果映射列表。
     *
     * @param resultMaps 结果映射列表
     * @return 当前Builder实例
     */
    public Builder resultMaps(List<ResultMap> resultMaps) {
      mappedStatement.resultMaps = resultMaps;
      // 检查是否包含嵌套结果映射
      for (ResultMap resultMap : resultMaps) {
        mappedStatement.hasNestedResultMaps = mappedStatement.hasNestedResultMaps || resultMap.hasNestedResultMaps();
      }
      return this;
    }

    /**
     * 设置fetch size。
     *
     * @param fetchSize 每次抓取的记录数
     * @return 当前Builder实例
     */
    public Builder fetchSize(Integer fetchSize) {
      mappedStatement.fetchSize = fetchSize;
      return this;
    }

    /**
     * 设置执行超时时间。
     *
     * @param timeout 超时时间（秒）
     * @return 当前Builder实例
     */
    public Builder timeout(Integer timeout) {
      mappedStatement.timeout = timeout;
      return this;
    }

    /**
     * 设置语句类型。
     *
     * @param statementType 语句类型
     * @return 当前Builder实例
     */
    public Builder statementType(StatementType statementType) {
      mappedStatement.statementType = statementType;
      return this;
    }

    /**
     * 设置结果集类型。
     *
     * @param resultSetType 结果集类型
     * @return 当前Builder实例
     */
    public Builder resultSetType(ResultSetType resultSetType) {
      mappedStatement.resultSetType = resultSetType == null ? ResultSetType.DEFAULT : resultSetType;
      return this;
    }

    /**
     * 设置二级缓存。
     *
     * @param cache 缓存对象
     * @return 当前Builder实例
     */
    public Builder cache(Cache cache) {
      mappedStatement.cache = cache;
      return this;
    }

    /**
     * 设置是否需要刷新缓存。
     *
     * @param flushCacheRequired 是否刷新
     * @return 当前Builder实例
     */
    public Builder flushCacheRequired(boolean flushCacheRequired) {
      mappedStatement.flushCacheRequired = flushCacheRequired;
      return this;
    }

    /**
     * 设置是否使用缓存。
     *
     * @param useCache 是否使用二级缓存
     * @return 当前Builder实例
     */
    public Builder useCache(boolean useCache) {
      mappedStatement.useCache = useCache;
      return this;
    }

    /**
     * 设置结果是否有序。
     *
     * @param resultOrdered 是否有序
     * @return 当前Builder实例
     */
    public Builder resultOrdered(boolean resultOrdered) {
      mappedStatement.resultOrdered = resultOrdered;
      return this;
    }

    /**
     * 设置主键生成器。
     *
     * @param keyGenerator 主键生成器
     * @return 当前Builder实例
     */
    public Builder keyGenerator(KeyGenerator keyGenerator) {
      mappedStatement.keyGenerator = keyGenerator;
      return this;
    }

    /**
     * 设置主键属性。
     *
     * @param keyProperty 主键属性名（逗号分隔）
     * @return 当前Builder实例
     */
    public Builder keyProperty(String keyProperty) {
      mappedStatement.keyProperties = delimitedStringToArray(keyProperty);
      return this;
    }

    /**
     * 设置主键列名。
     *
     * @param keyColumn 主键列名（逗号分隔）
     * @return 当前Builder实例
     */
    public Builder keyColumn(String keyColumn) {
      mappedStatement.keyColumns = delimitedStringToArray(keyColumn);
      return this;
    }

    /**
     * 设置数据库标识。
     *
     * @param databaseId 数据库产品标识
     * @return 当前Builder实例
     */
    public Builder databaseId(String databaseId) {
      mappedStatement.databaseId = databaseId;
      return this;
    }

    /**
     * 设置脚本语言驱动。
     *
     * @param driver 脚本语言驱动
     * @return 当前Builder实例
     */
    public Builder lang(LanguageDriver driver) {
      mappedStatement.lang = driver;
      return this;
    }

    /**
     * 设置结果集名称。
     *
     * @param resultSet 结果集名称（逗号分隔）
     * @return 当前Builder实例
     */
    public Builder resultSets(String resultSet) {
      mappedStatement.resultSets = delimitedStringToArray(resultSet);
      return this;
    }

    /**
     * Resul sets.
     *
     * @param resultSet
     *          the result set
     * @return the builder
     * @deprecated Use {@link #resultSets}
     */
    @Deprecated
    public Builder resulSets(String resultSet) {
      mappedStatement.resultSets = delimitedStringToArray(resultSet);
      return this;
    }

    /**
     * 构建MappedStatement对象。
     *
     * @return 构造完成的MappedStatement实例
     */
    public MappedStatement build() {
      assert mappedStatement.configuration != null;
      assert mappedStatement.id != null;
      assert mappedStatement.sqlSource != null;
      assert mappedStatement.lang != null;
      mappedStatement.resultMaps = Collections.unmodifiableList(mappedStatement.resultMaps);
      return mappedStatement;
    }
  }

  /**
   * 获取主键生成器。
   *
   * @return 主键生成器
   */
  public KeyGenerator getKeyGenerator() {
    return keyGenerator;
  }

  /**
   * 获取SQL命令类型。
   *
   * @return SQL命令类型
   */
  public SqlCommandType getSqlCommandType() {
    return sqlCommandType;
  }

  /**
   * 获取资源路径。
   *
   * @return 资源路径
   */
  public String getResource() {
    return resource;
  }

  /**
   * 获取全局配置对象。
   *
   * @return Configuration实例
   */
  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * 获取语句标识。
   *
   * @return 语句唯一标识
   */
  public String getId() {
    return id;
  }

  /**
   * 判断是否包含嵌套结果映射。
   *
   * @return 是否包含嵌套结果映射
   */
  public boolean hasNestedResultMaps() {
    return hasNestedResultMaps;
  }

  /**
   * 获取fetch size。
   *
   * @return fetch size
   */
  public Integer getFetchSize() {
    return fetchSize;
  }

  /**
   * 获取执行超时时间。
   *
   * @return 超时时间（秒）
   */
  public Integer getTimeout() {
    return timeout;
  }

  /**
   * 获取语句类型。
   *
   * @return 语句类型
   */
  public StatementType getStatementType() {
    return statementType;
  }

  /**
   * 获取结果集类型。
   *
   * @return 结果集类型
   */
  public ResultSetType getResultSetType() {
    return resultSetType;
  }

  /**
   * 获取SQL源码对象。
   *
   * @return SqlSource实例
   */
  public SqlSource getSqlSource() {
    return sqlSource;
  }

  /**
   * 获取参数映射配置。
   *
   * @return ParameterMap实例
   */
  public ParameterMap getParameterMap() {
    return parameterMap;
  }

  /**
   * 获取结果映射列表。
   *
   * @return ResultMap列表
   */
  public List<ResultMap> getResultMaps() {
    return resultMaps;
  }

  /**
   * 获取二级缓存对象。
   *
   * @return Cache实例
   */
  public Cache getCache() {
    return cache;
  }

  /**
   * 判断是否需要刷新缓存。
   *
   * @return 是否需要刷新
   */
  public boolean isFlushCacheRequired() {
    return flushCacheRequired;
  }

  /**
   * 判断是否使用二级缓存。
   *
   * @return 是否使用缓存
   */
  public boolean isUseCache() {
    return useCache;
  }

  /**
   * 判断结果是否有序。
   *
   * @return 是否有序
   */
  public boolean isResultOrdered() {
    return resultOrdered;
  }

  /**
   * 获取数据库标识。
   *
   * @return 数据库产品标识
   */
  public String getDatabaseId() {
    return databaseId;
  }

  /**
   * 获取主键属性数组。
   *
   * @return 主键属性名数组
   */
  public String[] getKeyProperties() {
    return keyProperties;
  }

  /**
   * 获取主键列名数组。
   *
   * @return 主键列名数组
   */
  public String[] getKeyColumns() {
    return keyColumns;
  }

  /**
   * 获取日志记录器。
   *
   * @return Log实例
   */
  public Log getStatementLog() {
    return statementLog;
  }

  /**
   * 获取脚本语言驱动。
   *
   * @return LanguageDriver实例
   */
  public LanguageDriver getLang() {
    return lang;
  }

  /**
   * 获取结果集名称数组。
   *
   * @return 结果集名称数组
   */
  public String[] getResultSets() {
    return resultSets;
  }

  /**
   * Gets the resul sets.
   *
   * @return the resul sets
   * @deprecated Use {@link #getResultSets()}
   */
  @Deprecated
  public String[] getResulSets() {
    return resultSets;
  }

  /**
   * 获取绑定的SQL对象。
   *
   * @param parameterObject 参数对象
   * @return BoundSql实例，包含完整SQL和参数映射
   */
  public BoundSql getBoundSql(Object parameterObject) {
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    // 若无参数映射，则使用默认参数映射
    if (parameterMappings == null || parameterMappings.isEmpty()) {
      boundSql = new BoundSql(configuration, boundSql.getSql(), parameterMap.getParameterMappings(), parameterObject);
    }

    // 检查参数映射中是否包含嵌套结果映射（issue #30）
    for (ParameterMapping pm : boundSql.getParameterMappings()) {
      String rmId = pm.getResultMapId();
      if (rmId != null) {
        ResultMap rm = configuration.getResultMap(rmId);
        if (rm != null) {
          hasNestedResultMaps |= rm.hasNestedResultMaps();
        }
      }
    }

    return boundSql;
  }

  /**
   * 将逗号分隔的字符串转换为字符串数组。
   *
   * @param in 逗号分隔的字符串
   * @return 字符串数组，若输入为空则返回null
   */
  private static String[] delimitedStringToArray(String in) {
    if (in == null || in.trim().length() == 0) {
      return null;
    } else {
      return in.split(",");
    }
  }

}
