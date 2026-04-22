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

import java.util.regex.Pattern;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.scripting.ScriptingException;
import org.apache.ibatis.type.SimpleTypeRegistry;

/**
 * TextSqlNode - 封装静态或包含 ${} 占位符的动态 SQL 文本，并负责解析和参数绑定。
 *
 * @author Clinton Begin
 */
public class TextSqlNode implements SqlNode {
  /** SQL 文本内容，可能包含 ${} 占位符 */
  private final String text;
  /** 用于校验参数值防止 SQL 注入的正则过滤器 */
  private final Pattern injectionFilter;

  /**
   * 构造方法，不带注入过滤器。
   *
   * @param text SQL 文本
   */
  public TextSqlNode(String text) {
    this(text, null);
  }

  /**
   * 构造方法。
   *
   * @param text SQL 文本
   * @param injectionFilter 用于参数值安全检查的正则过滤器
   */
  public TextSqlNode(String text, Pattern injectionFilter) {
    this.text = text;
    this.injectionFilter = injectionFilter;
  }

  /**
   * 判断当前 SQL 文本是否包含动态 ${} 占位符。
   *
   * @return true 表示包含动态占位符
   */
  public boolean isDynamic() {
    DynamicCheckerTokenParser checker = new DynamicCheckerTokenParser();
    GenericTokenParser parser = createParser(checker);
    parser.parse(text);
    return checker.isDynamic();
  }

  /**
   * 解析 SQL 文本中的 ${} 占位符，绑定参数后追加到 DynamicContext 中。
   *
   * @param context DynamicContext
   * @return 始终返回 true
   */
  @Override
  public boolean apply(DynamicContext context) {
    GenericTokenParser parser = createParser(new BindingTokenParser(context, injectionFilter));
    context.appendSql(parser.parse(text));
    return true;
  }

  private GenericTokenParser createParser(TokenHandler handler) {
    return new GenericTokenParser("${", "}", handler);
  }

  /** 负责将 ${} 占位符替换为实际参数值的 TokenHandler */
  private static class BindingTokenParser implements TokenHandler {

    /** DynamicContext，用于获取参数绑定 */
    private DynamicContext context;
    /** 注入过滤器，用于校验替换后的值 */
    private Pattern injectionFilter;

    /**
     * 构造方法。
     *
     * @param context DynamicContext
     * @param injectionFilter 注入过滤器
     */
    public BindingTokenParser(DynamicContext context, Pattern injectionFilter) {
      this.context = context;
      this.injectionFilter = injectionFilter;
    }

    @Override
    public String handleToken(String content) {
      Object parameter = context.getBindings().get("_parameter");
      // 为简单类型参数或空参数设置 "value" 别名，方便 OGNL 表达式引用
      if (parameter == null) {
        context.getBindings().put("value", null);
      } else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
        context.getBindings().put("value", parameter);
      }
      Object value = OgnlCache.getValue(content, context.getBindings());
      // issue #274: 当值为 null 时返回空字符串，避免输出 "null" 字符串
      String srtValue = value == null ? "" : String.valueOf(value);
      checkInjection(srtValue);
      return srtValue;
    }

    /**
     * 检查参数值是否符合注入过滤规则。
     *
     * @param value 待校验的参数值
     */
    private void checkInjection(String value) {
      if (injectionFilter != null && !injectionFilter.matcher(value).matches()) {
        throw new ScriptingException("Invalid input. Please conform to regex" + injectionFilter.pattern());
      }
    }
  }

  /** 用于检测 SQL 文本中是否包含动态 ${} 占位符的 TokenHandler */
  private static class DynamicCheckerTokenParser implements TokenHandler {

    /** 标记是否检测到动态占位符 */
    private boolean isDynamic;

    public DynamicCheckerTokenParser() {
      // Prevent Synthetic Access
    }

    /**
     * 返回是否包含动态占位符。
     *
     * @return true 表示包含动态占位符
     */
    public boolean isDynamic() {
      return isDynamic;
    }

    @Override
    public String handleToken(String content) {
      // 一旦匹配到 ${} 占位符，即标记为动态 SQL
      this.isDynamic = true;
      return null;
    }
  }

}
