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
package org.apache.ibatis.scripting.xmltags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.ibatis.session.Configuration;

/**
 * 处理动态 SQL 中的 trim SqlNode，支持前缀/后缀的添加与覆盖。
 *
 * @author Clinton Begin
 */
public class TrimSqlNode implements SqlNode {

  /** 子 SqlNode */
  private final SqlNode contents;
  /** 需要添加的前缀 */
  private final String prefix;
  /** 需要添加的后缀 */
  private final String suffix;
  /** 需要移除的前缀 List */
  private final List<String> prefixesToOverride;
  /** 需要移除的后缀 List */
  private final List<String> suffixesToOverride;
  /** MyBatis Configuration */
  private final Configuration configuration;

  /**
   * 构造方法，支持以管道符分隔的覆盖字符串。
   *
   * @param configuration MyBatis Configuration
   * @param contents 子 SqlNode
   * @param prefix 需要添加的前缀
   * @param prefixesToOverride 以 "|" 分隔的需要移除的前缀
   * @param suffix 需要添加的后缀
   * @param suffixesToOverride 以 "|" 分隔的需要移除的后缀
   */
  public TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, String prefixesToOverride, String suffix, String suffixesToOverride) {
    this(configuration, contents, prefix, parseOverrides(prefixesToOverride), suffix, parseOverrides(suffixesToOverride));
  }

  /**
   * 受保护的构造方法，直接使用 List 形式的覆盖项。
   *
   * @param configuration MyBatis Configuration
   * @param contents 子 SqlNode
   * @param prefix 需要添加的前缀
   * @param prefixesToOverride 需要移除的前缀 List
   * @param suffix 需要添加的后缀
   * @param suffixesToOverride 需要移除的后缀 List
   */
  protected TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, List<String> prefixesToOverride, String suffix, List<String> suffixesToOverride) {
    this.contents = contents;
    this.prefix = prefix;
    this.prefixesToOverride = prefixesToOverride;
    this.suffix = suffix;
    this.suffixesToOverride = suffixesToOverride;
    this.configuration = configuration;
  }

  /**
   * 应用 trim 逻辑：先执行子 SqlNode，再对生成的 SQL 进行前缀/后缀处理。
   *
   * @param context DynamicContext
   * @return 子 SqlNode 是否成功应用
   */
  @Override
  public boolean apply(DynamicContext context) {
    FilteredDynamicContext filteredDynamicContext = new FilteredDynamicContext(context);
    boolean result = contents.apply(filteredDynamicContext);
    filteredDynamicContext.applyAll();
    return result;
  }

  /**
   * 将管道符分隔的字符串解析为大写的覆盖项 List。
   *
   * @param overrides 以 "|" 分隔的覆盖字符串
   * @return 解析后的覆盖项 List，若为空则返回空 List
   */
  private static List<String> parseOverrides(String overrides) {
    if (overrides != null) {
      final StringTokenizer parser = new StringTokenizer(overrides, "|", false);
      final List<String> list = new ArrayList<>(parser.countTokens());
      while (parser.hasMoreTokens()) {
        list.add(parser.nextToken().toUpperCase(Locale.ENGLISH));
      }
      return list;
    }
    return Collections.emptyList();
  }

  /**
   * 内部类，用于拦截子 SqlNode 生成的 SQL，并在最后统一应用 trim 逻辑。
   */
  private class FilteredDynamicContext extends DynamicContext {
    /** 被代理的 DynamicContext */
    private DynamicContext delegate;
    /** 前缀是否已应用 */
    private boolean prefixApplied;
    /** 后缀是否已应用 */
    private boolean suffixApplied;
    /** 缓存子 SqlNode 生成的 SQL */
    private StringBuilder sqlBuffer;

    /**
     * 构造方法。
     *
     * @param delegate 被代理的 DynamicContext
     */
    public FilteredDynamicContext(DynamicContext delegate) {
      super(configuration, null);
      this.delegate = delegate;
      this.prefixApplied = false;
      this.suffixApplied = false;
      this.sqlBuffer = new StringBuilder();
    }

    /**
     * 对缓存的 SQL 执行 trim 处理，并将结果追加到代理 DynamicContext 中。
     */
    public void applyAll() {
      sqlBuffer = new StringBuilder(sqlBuffer.toString().trim());
      String trimmedUppercaseSql = sqlBuffer.toString().toUpperCase(Locale.ENGLISH);
      if (trimmedUppercaseSql.length() > 0) {
        applyPrefix(sqlBuffer, trimmedUppercaseSql);
        applySuffix(sqlBuffer, trimmedUppercaseSql);
      }
      delegate.appendSql(sqlBuffer.toString());
    }

    @Override
    public Map<String, Object> getBindings() {
      return delegate.getBindings();
    }

    @Override
    public void bind(String name, Object value) {
      delegate.bind(name, value);
    }

    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }

    @Override
    public void appendSql(String sql) {
      sqlBuffer.append(sql);
    }

    @Override
    public String getSql() {
      return delegate.getSql();
    }

    /**
     * 应用前缀逻辑：先移除匹配的前缀，再插入指定前缀。
     *
     * @param sql 当前 SQL 缓冲区
     * @param trimmedUppercaseSql 转大写后的 SQL，用于匹配
     */
    private void applyPrefix(StringBuilder sql, String trimmedUppercaseSql) {
      if (!prefixApplied) {
        prefixApplied = true;
        if (prefixesToOverride != null) {
          for (String toRemove : prefixesToOverride) {
            if (trimmedUppercaseSql.startsWith(toRemove)) {
              sql.delete(0, toRemove.trim().length());
              break;
            }
          }
        }
        if (prefix != null) {
          sql.insert(0, " ");
          sql.insert(0, prefix);
        }
      }
    }

    /**
     * 应用后缀逻辑：先移除匹配的后缀，再追加指定后缀。
     *
     * @param sql 当前 SQL 缓冲区
     * @param trimmedUppercaseSql 转大写后的 SQL，用于匹配
     */
    private void applySuffix(StringBuilder sql, String trimmedUppercaseSql) {
      if (!suffixApplied) {
        suffixApplied = true;
        if (suffixesToOverride != null) {
          for (String toRemove : suffixesToOverride) {
            if (trimmedUppercaseSql.endsWith(toRemove) || trimmedUppercaseSql.endsWith(toRemove.trim())) {
              int start = sql.length() - toRemove.trim().length();
              int end = sql.length();
              sql.delete(start, end);
              break;
            }
          }
        }
        if (suffix != null) {
          sql.append(" ");
          sql.append(suffix);
        }
      }
    }

  }

}
