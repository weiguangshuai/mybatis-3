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
package org.apache.ibatis.parsing;

import java.util.Properties;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
/**
 * 属性占位符解析器，负责替换字符串中的 ${key} 占位符为实际属性值。
 * 支持默认值语法 ${key:defaultValue}，可通过配置开启。
 */
public class PropertyParser {

  private static final String KEY_PREFIX = "org.apache.ibatis.parsing.PropertyParser.";
  /**
   * The special property key that indicate whether enable a default value on placeholder.
   * <p>
   *   The default value is {@code false} (indicate disable a default value on placeholder)
   *   If you specify the {@code true}, you can specify key and default value on placeholder (e.g. {@code ${db.username:postgres}}).
   * </p>
   * @since 3.4.2
   */
  public static final String KEY_ENABLE_DEFAULT_VALUE = KEY_PREFIX + "enable-default-value";

  /**
   * The special property key that specify a separator for key and default value on placeholder.
   * <p>
   *   The default separator is {@code ":"}.
   * </p>
   * @since 3.4.2
   */
  public static final String KEY_DEFAULT_VALUE_SEPARATOR = KEY_PREFIX + "default-value-separator";

  /** 默认情况下禁用占位符默认值功能 */
  private static final String ENABLE_DEFAULT_VALUE = "false";
  /** 默认的键与默认值的分隔符 */
  private static final String DEFAULT_VALUE_SEPARATOR = ":";

  private PropertyParser() {
    // Prevent Instantiation
  }

  /**
   * 解析字符串中的占位符，将其替换为属性值。
   * @param string 待解析的字符串，包含 ${key} 形式的占位符
   * @param variables 属性键值对，用于替换占位符
   * @return 替换后的字符串
   */
  public static String parse(String string, Properties variables) {
    VariableTokenHandler handler = new VariableTokenHandler(variables);
    GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
    return parser.parse(string);
  }

  /** 占位符变量处理器，负责解析 ${key} 或 ${key:defaultValue} 语法 */
  private static class VariableTokenHandler implements TokenHandler {
    /** 属性键值对 */
    private final Properties variables;
    /** 是否启用占位符默认值功能 */
    private final boolean enableDefaultValue;
    /** 键与默认值的分隔符，默认为 ":" */
    private final String defaultValueSeparator;

    private VariableTokenHandler(Properties variables) {
      this.variables = variables;
      // 根据配置确定是否启用默认值功能，并获取分隔符配置
      this.enableDefaultValue = Boolean.parseBoolean(getPropertyValue(KEY_ENABLE_DEFAULT_VALUE, ENABLE_DEFAULT_VALUE));
      this.defaultValueSeparator = getPropertyValue(KEY_DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR);
    }

    /** 获取属性值，若变量对象为空则返回默认值 */
    private String getPropertyValue(String key, String defaultValue) {
      return (variables == null) ? defaultValue : variables.getProperty(key, defaultValue);
    }

    /**
     * 处理占位符 token，返回替换后的值。
     * 支持两种语法：${key} 和 ${key:defaultValue}（需启用默认值功能）。
     * @param content 占位符中的内容部分
     * @return 替换后的值，若找不到对应属性则返回原占位符格式
     */
    @Override
    public String handleToken(String content) {
      if (variables != null) {
        String key = content;
        if (enableDefaultValue) {
          // 查找分隔符位置，解析默认值语法
          final int separatorIndex = content.indexOf(defaultValueSeparator);
          String defaultValue = null;
          if (separatorIndex >= 0) {
            key = content.substring(0, separatorIndex);
            defaultValue = content.substring(separatorIndex + defaultValueSeparator.length());
          }
          // 存在默认值时，优先使用属性值，否则使用默认值
          if (defaultValue != null) {
            return variables.getProperty(key, defaultValue);
          }
        }
        // 未启用默认值或无默认值时，直接查找属性
        if (variables.containsKey(key)) {
          return variables.getProperty(key);
        }
      }
      // 属性不存在时，返回原占位符格式
      return "${" + content + "}";
    }
  }

}
