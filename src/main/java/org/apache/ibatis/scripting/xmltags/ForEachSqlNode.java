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

import java.util.Map;
import java.util.Optional;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 */
/**
 * 处理 <foreach> 动态 SqlNode，负责遍历集合并生成对应的 SQL 片段
 */
public class ForEachSqlNode implements SqlNode {
  /** 自动生成的前缀，用于区分 foreach 迭代变量 */
  public static final String ITEM_PREFIX = "__frch_";

  /** ExpressionEvaluator，用于解析集合表达式 */
  private final ExpressionEvaluator evaluator;
  /** 集合表达式，如传入的参数名 */
  private final String collectionExpression;
  /** 是否允许集合为 null */
  private final Boolean nullable;
  /** 子 SqlNode，即 foreach 内部嵌套的 SqlNode */
  private final SqlNode contents;
  /** 整个 foreach 块的前缀字符串 */
  private final String open;
  /** 整个 foreach 块的后缀字符串 */
  private final String close;
  /** 各元素之间的分隔符 */
  private final String separator;
  /** 当前元素的变量名 */
  private final String item;
  /** 当前索引的变量名 */
  private final String index;
  /** MyBatis Configuration */
  private final Configuration configuration;

  /**
   * @deprecated Since 3.5.9, use the {@link #ForEachSqlNode(Configuration, SqlNode, String, Boolean, String, String, String, String, String)}.
   */
  @Deprecated
  /**
   * 构造方法（已废弃）
   *
   * @param configuration MyBatis Configuration
   * @param contents 子 SqlNode
   * @param collectionExpression 集合表达式
   * @param index 索引变量名
   * @param item 元素变量名
   * @param open 前缀字符串
   * @param close 后缀字符串
   * @param separator 分隔符
   */
  public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, String index, String item, String open, String close, String separator) {
    this(configuration, contents, collectionExpression, null, index, item, open, close, separator);
  }

  /**
   * @since 3.5.9
   */
  /**
   * 构造方法
   *
   * @param configuration MyBatis Configuration
   * @param contents 子 SqlNode
   * @param collectionExpression 集合表达式
   * @param nullable 是否允许集合为 null
   * @param index 索引变量名
   * @param item 元素变量名
   * @param open 前缀字符串
   * @param close 后缀字符串
   * @param separator 分隔符
   */
  public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, Boolean nullable, String index, String item, String open, String close, String separator) {
    this.evaluator = new ExpressionEvaluator();
    this.collectionExpression = collectionExpression;
    this.nullable = nullable;
    this.contents = contents;
    this.open = open;
    this.close = close;
    this.separator = separator;
    this.index = index;
    this.item = item;
    this.configuration = configuration;
  }

  /**
   * 应用当前 foreach SqlNode，遍历集合并拼接 SQL
   *
   * @param context DynamicContext，包含参数绑定和已生成的 SQL
   * @return 是否成功应用
   */
  @Override
  public boolean apply(DynamicContext context) {
    Map<String, Object> bindings = context.getBindings();
    final Iterable<?> iterable = evaluator.evaluateIterable(collectionExpression, bindings,
      Optional.ofNullable(nullable).orElseGet(configuration::isNullableOnForEach));
    if (iterable == null || !iterable.iterator().hasNext()) {
      return true;
    }
    boolean first = true;
    applyOpen(context);
    int i = 0;
    for (Object o : iterable) {
      DynamicContext oldContext = context;
      // 首个元素或没有分隔符时，前缀为空；否则使用 separator 作为前缀
      if (first || separator == null) {
        context = new PrefixedContext(context, "");
      } else {
        context = new PrefixedContext(context, separator);
      }
      int uniqueNumber = context.getUniqueNumber();
      // Issue #709：Map.Entry 类型时，key 作为索引，value 作为元素
      if (o instanceof Map.Entry) {
        @SuppressWarnings("unchecked")
        Map.Entry<Object, Object> mapEntry = (Map.Entry<Object, Object>) o;
        applyIndex(context, mapEntry.getKey(), uniqueNumber);
        applyItem(context, mapEntry.getValue(), uniqueNumber);
      } else {
        applyIndex(context, i, uniqueNumber);
        applyItem(context, o, uniqueNumber);
      }
      contents.apply(new FilteredDynamicContext(configuration, context, index, item, uniqueNumber));
      if (first) {
        first = !((PrefixedContext) context).isPrefixApplied();
      }
      context = oldContext;
      i++;
    }
    applyClose(context);
    context.getBindings().remove(item);
    context.getBindings().remove(index);
    return true;
  }

  /**
   * 将索引值绑定到 DynamicContext，同时绑定带唯一前缀的变量名
   */
  private void applyIndex(DynamicContext context, Object o, int i) {
    if (index != null) {
      context.bind(index, o);
      context.bind(itemizeItem(index, i), o);
    }
  }

  /**
   * 将元素值绑定到 DynamicContext，同时绑定带唯一前缀的变量名
   */
  private void applyItem(DynamicContext context, Object o, int i) {
    if (item != null) {
      context.bind(item, o);
      context.bind(itemizeItem(item, i), o);
    }
  }

  /**
   * 追加 open 前缀到 SQL
   */
  private void applyOpen(DynamicContext context) {
    if (open != null) {
      context.appendSql(open);
    }
  }

  /**
   * 追加 close 后缀到 SQL
   */
  private void applyClose(DynamicContext context) {
    if (close != null) {
      context.appendSql(close);
    }
  }

  /**
   * 生成带唯一前缀的变量名，用于区分不同迭代项
   */
  private static String itemizeItem(String item, int i) {
    return ITEM_PREFIX + item + "_" + i;
  }

  /**
   * 过滤 DynamicContext，将 #{item} 和 #{index} 替换为带唯一前缀的变量名，避免参数名冲突
   */
  private static class FilteredDynamicContext extends DynamicContext {
    /** 委托的原始 DynamicContext */
    private final DynamicContext delegate;
    /** 当前迭代项的唯一序号 */
    private final int index;
    /** 索引变量名 */
    private final String itemIndex;
    /** 元素变量名 */
    private final String item;

    public FilteredDynamicContext(Configuration configuration,DynamicContext delegate, String itemIndex, String item, int i) {
      super(configuration, null);
      this.delegate = delegate;
      this.index = i;
      this.itemIndex = itemIndex;
      this.item = item;
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
    public String getSql() {
      return delegate.getSql();
    }

    @Override
    public void appendSql(String sql) {
      GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> {
        // 优先替换 item 变量
        String newContent = content.replaceFirst("^\\s*" + item + "(?![^.,:\\s])", itemizeItem(item, index));
        // 若未替换且存在 index 变量，则尝试替换 index
        if (itemIndex != null && newContent.equals(content)) {
          newContent = content.replaceFirst("^\\s*" + itemIndex + "(?![^.,:\\s])", itemizeItem(itemIndex, index));
        }
        return "#{" + newContent + "}";
      });

      delegate.appendSql(parser.parse(sql));
    }

    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }

  }


  /**
   * 带前缀的 DynamicContext，仅在首次追加有效 SQL 时添加前缀（用于处理 separator）
   */
  private class PrefixedContext extends DynamicContext {
    /** 委托的原始 DynamicContext */
    private final DynamicContext delegate;
    /** 待添加的前缀 */
    private final String prefix;
    /** 前缀是否已应用 */
    private boolean prefixApplied;

    public PrefixedContext(DynamicContext delegate, String prefix) {
      super(configuration, null);
      this.delegate = delegate;
      this.prefix = prefix;
      this.prefixApplied = false;
    }

    public boolean isPrefixApplied() {
      return prefixApplied;
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
    public void appendSql(String sql) {
      // 首次追加非空 SQL 时，先添加前缀
      if (!prefixApplied && sql != null && sql.trim().length() > 0) {
        delegate.appendSql(prefix);
        prefixApplied = true;
      }
      delegate.appendSql(sql);
    }

    @Override
    public String getSql() {
      return delegate.getSql();
    }

    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }
  }

}
