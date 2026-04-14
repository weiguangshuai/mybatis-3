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

import org.w3c.dom.*;
import org.w3c.dom.CharacterData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * DOM节点封装类，提供对XML节点属性的便捷访问和类型转换功能。
 *
 * @author Clinton Begin
 */
public class XNode {

  /**
   * 底层DOM节点对象
   */
  private final Node node;
  /**
   * 节点名称（标签名）
   */
  private final String name;
  /**
   * 节点文本内容
   */
  private final String body;
  /**
   * 节点属性集
   */
  private final Properties attributes;
  /**
   * 变量配置，用于属性值替换
   */
  private final Properties variables;
  /**
   * XPath解析器引用
   */
  private final XPathParser xpathParser;

  /**
   * 构造XNode对象，解析节点的属性和文本内容。
   *
   * @param xpathParser XPath解析器
   * @param node        DOM节点
   * @param variables   变量配置，用于属性值中的占位符替换
   */
  public XNode(XPathParser xpathParser, Node node, Properties variables) {
    this.xpathParser = xpathParser;
    this.node = node;
    this.name = node.getNodeName();
    this.variables = variables;
    this.attributes = parseAttributes(node);
    this.body = parseBody(node);
  }

  /**
   * 基于当前节点创建一个新的XNode实例。
   *
   * @param node 新的DOM节点
   * @return 新的XNode对象
   */
  public XNode newXNode(Node node) {
    return new XNode(xpathParser, node, variables);
  }

  /**
   * 获取父节点对应的XNode对象。
   *
   * @return 父节点XNode，若父节点不是Element则返回null
   */
  public XNode getParent() {
    Node parent = node.getParentNode();
    if (!(parent instanceof Element)) {
      return null;
    } else {
      return new XNode(xpathParser, parent, variables);
    }
  }

  /**
   * 获取从根节点到当前节点的路径。
   *
   * @return 节点路径，格式如 /根节点/子节点/当前节点
   */
  public String getPath() {
    StringBuilder builder = new StringBuilder();
    Node current = node;
    // 从当前节点向上遍历至根节点，构建路径
    while (current instanceof Element) {
      if (current != node) {
        builder.insert(0, "/");
      }
      builder.insert(0, current.getNodeName());
      current = current.getParentNode();
    }
    return builder.toString();
  }

  /**
   * 基于节点属性值生成唯一标识符。
   * 依次尝试获取id、value、property属性，拼接节点名和属性值生成标识。
   *
   * @return 基于属性值的唯一标识
   */
  public String getValueBasedIdentifier() {
    StringBuilder builder = new StringBuilder();
    XNode current = this;
    while (current != null) {
      if (current != this) {
        builder.insert(0, "_");
      }
      // 依次查找id、value、property属性
      String value = current.getStringAttribute("id",
        current.getStringAttribute("value",
          current.getStringAttribute("property", (String) null)));
      if (value != null) {
        value = value.replace('.', '_');
        builder.insert(0, "]");
        builder.insert(0, value);
        builder.insert(0, "[");
      }
      builder.insert(0, current.getName());
      current = current.getParent();
    }
    return builder.toString();
  }

  /**
   * 使用XPath表达式计算字符串值。
   *
   * @param expression XPath表达式
   * @return 字符串结果
   */
  public String evalString(String expression) {
    return xpathParser.evalString(node, expression);
  }

  /**
   * 使用XPath表达式计算布尔值。
   *
   * @param expression XPath表达式
   * @return 布尔结果
   */
  public Boolean evalBoolean(String expression) {
    return xpathParser.evalBoolean(node, expression);
  }

  /**
   * 使用XPath表达式计算数值。
   *
   * @param expression XPath表达式
   * @return 数值结果
   */
  public Double evalDouble(String expression) {
    return xpathParser.evalDouble(node, expression);
  }

  /**
   * 使用XPath表达式计算多个节点。
   *
   * @param expression XPath表达式
   * @return 匹配的XNode列表
   */
  public List<XNode> evalNodes(String expression) {
    return xpathParser.evalNodes(node, expression);
  }

  /**
   * 使用XPath表达式计算单个节点。
   *
   * @param expression XPath表达式
   * @return 匹配的XNode，若无匹配则返回null
   */
  public XNode evalNode(String expression) {
    return xpathParser.evalNode(node, expression);
  }

  /**
   * 获取底层DOM节点。
   *
   * @return DOM节点对象
   */
  public Node getNode() {
    return node;
  }

  /**
   * 获取节点名称。
   *
   * @return 节点名称（标签名）
   */
  public String getName() {
    return name;
  }

  /**
   * 获取节点文本内容。
   *
   * @return 文本内容，若无则返回null
   */
  public String getStringBody() {
    return getStringBody(null);
  }

  /**
   * 获取节点文本内容，默认值。
   *
   * @param def 默认值
   * @return 文本内容，若无则返回默认值
   */
  public String getStringBody(String def) {
    return body == null ? def : body;
  }

  /**
   * 获取布尔类型的节点内容。
   *
   * @return 布尔值，若无则返回null
   */
  public Boolean getBooleanBody() {
    return getBooleanBody(null);
  }

  /**
   * 获取布尔类型的节点内容，默认值。
   *
   * @param def 默认值
   * @return 布尔值，若无则返回默认值
   */
  public Boolean getBooleanBody(Boolean def) {
    return body == null ? def : Boolean.valueOf(body);
  }

  /**
   * 获取整数类型的节点内容。
   *
   * @return 整数值，若无则返回null
   */
  public Integer getIntBody() {
    return getIntBody(null);
  }

  /**
   * 获取整数类型的节点内容，默认值。
   *
   * @param def 默认值
   * @return 整数值，若无则返回默认值
   */
  public Integer getIntBody(Integer def) {
    return body == null ? def : Integer.valueOf(body);
  }

  /**
   * 获取长整数类型的节点内容。
   *
   * @return 长整数值，若无则返回null
   */
  public Long getLongBody() {
    return getLongBody(null);
  }

  /**
   * 获取长整数类型的节点内容，默认值。
   *
   * @param def 默认值
   * @return 长整数值，若无则返回默认值
   */
  public Long getLongBody(Long def) {
    return body == null ? def : Long.valueOf(body);
  }

  /**
   * 获取双精度类型的节点内容。
   *
   * @return 双精度数值，若无则返回null
   */
  public Double getDoubleBody() {
    return getDoubleBody(null);
  }

  /**
   * 获取双精度类型的节点内容，默认值。
   *
   * @param def 默认值
   * @return 双精度数值，若无则返回默认值
   */
  public Double getDoubleBody(Double def) {
    return body == null ? def : Double.valueOf(body);
  }

  /**
   * 获取浮点类型的节点内容。
   *
   * @return 浮点数值，若无则返回null
   */
  public Float getFloatBody() {
    return getFloatBody(null);
  }

  /**
   * 获取浮点类型的节点内容，默认值。
   *
   * @param def 默认值
   * @return 浮点数值，若无则返回默认值
   */
  public Float getFloatBody(Float def) {
    return body == null ? def : Float.valueOf(body);
  }

  /**
   * 获取枚举类型的属性值。
   *
   * @param enumType 枚举类型
   * @param name     属性名
   * @return 枚举值，若属性不存在则返回null
   */
  public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name) {
    return getEnumAttribute(enumType, name, null);
  }

  /**
   * 获取枚举类型的属性值，默认值。
   *
   * @param enumType 枚举类型
   * @param name     属性名
   * @param def      默认值
   * @return 枚举值，若属性不存在则返回默认值
   */
  public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name, T def) {
    String value = getStringAttribute(name);
    return value == null ? def : Enum.valueOf(enumType, value);
  }

  /**
   * Return a attribute value as String.
   *
   * <p>
   * If attribute value is absent, return value that provided from supplier of default value.
   *
   * @param name        attribute name
   * @param defSupplier a supplier of default value
   * @return the string attribute
   * @since 3.5.4
   */
  public String getStringAttribute(String name, Supplier<String> defSupplier) {
    String value = attributes.getProperty(name);
    return value == null ? defSupplier.get() : value;
  }

  /**
   * 获取字符串类型的属性值。
   *
   * @param name 属性名
   * @return 属性值，若不存在则返回null
   */
  public String getStringAttribute(String name) {
    return getStringAttribute(name, (String) null);
  }

  /**
   * 获取字符串类型的属性值，默认值。
   *
   * @param name 属性名
   * @param def  默认值
   * @return 属性值，若不存在则返回默认值
   */
  public String getStringAttribute(String name, String def) {
    String value = attributes.getProperty(name);
    return value == null ? def : value;
  }

  /**
   * 获取布尔类型的属性值。
   *
   * @param name 属性名
   * @return 布尔值，若不存在则返回null
   */
  public Boolean getBooleanAttribute(String name) {
    return getBooleanAttribute(name, null);
  }

  /**
   * 获取布尔类型的属性值，默认值。
   *
   * @param name 属性名
   * @param def  默认值
   * @return 布尔值，若不存在则返回默认值
   */
  public Boolean getBooleanAttribute(String name, Boolean def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Boolean.valueOf(value);
  }

  /**
   * 获取整数类型的属性值。
   *
   * @param name 属性名
   * @return 整数值，若不存在则返回null
   */
  public Integer getIntAttribute(String name) {
    return getIntAttribute(name, null);
  }

  /**
   * 获取整数类型的属性值，默认值。
   *
   * @param name 属性名
   * @param def  默认值
   * @return 整数值，若不存在则返回默认值
   */
  public Integer getIntAttribute(String name, Integer def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Integer.valueOf(value);
  }

  /**
   * 获取长整数类型的属性值。
   *
   * @param name 属性名
   * @return 长整数值，若不存在则返回null
   */
  public Long getLongAttribute(String name) {
    return getLongAttribute(name, null);
  }

  /**
   * 获取长整数类型的属性值，默认值。
   *
   * @param name 属性名
   * @param def  默认值
   * @return 长整数值，若不存在则返回默认值
   */
  public Long getLongAttribute(String name, Long def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Long.valueOf(value);
  }

  /**
   * 获取双精度类型的属性值。
   *
   * @param name 属性名
   * @return 双精度数值，若不存在则返回null
   */
  public Double getDoubleAttribute(String name) {
    return getDoubleAttribute(name, null);
  }

  /**
   * 获取双精度类型的属性值，默认值。
   *
   * @param name 属性名
   * @param def  默认值
   * @return 双精度数值，若不存在则返回默认值
   */
  public Double getDoubleAttribute(String name, Double def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Double.valueOf(value);
  }

  /**
   * 获取浮点类型的属性值。
   *
   * @param name 属性名
   * @return 浮点数值，若不存在则返回null
   */
  public Float getFloatAttribute(String name) {
    return getFloatAttribute(name, null);
  }

  /**
   * 获取浮点类型的属性值，默认值。
   *
   * @param name 属性名
   * @param def  默认值
   * @return 浮点数值，若不存在则返回默认值
   */
  public Float getFloatAttribute(String name, Float def) {
    String value = attributes.getProperty(name);
    return value == null ? def : Float.valueOf(value);
  }

  /**
   * 获取所有子节点。
   *
   * @return 子节点列表（仅包含Element类型的节点）
   */
  public List<XNode> getChildren() {
    List<XNode> children = new ArrayList<>();
    NodeList nodeList = node.getChildNodes();
    if (nodeList != null) {
      for (int i = 0, n = nodeList.getLength(); i < n; i++) {
        Node node = nodeList.item(i);
        // 只处理Element类型的子节点
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          children.add(new XNode(xpathParser, node, variables));
        }
      }
    }
    return children;
  }

  /**
   * 将子节点转换为Properties。
   * 每个子节点的name属性作为key，value属性作为value。
   *
   * @return 包含子节点name-value对的Properties对象
   */
  public Properties getChildrenAsProperties() {
    Properties properties = new Properties();
    for (XNode child : getChildren()) {
      String name = child.getStringAttribute("name");
      String value = child.getStringAttribute("value");
      if (name != null && value != null) {
        properties.setProperty(name, value);
      }
    }
    return properties;
  }

  /**
   * 返回节点的XML字符串表示。
   *
   * @return XML格式字符串
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    toString(builder, 0);
    return builder.toString();
  }

  /**
   * 递归构建XML字符串。
   *
   * @param builder 字符串构建器
   * @param level   缩进层级
   */
  private void toString(StringBuilder builder, int level) {
    builder.append("<");
    builder.append(name);
    // 添加属性
    for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
      builder.append(" ");
      builder.append(entry.getKey());
      builder.append("=\"");
      builder.append(entry.getValue());
      builder.append("\"");
    }
    List<XNode> children = getChildren();
    if (!children.isEmpty()) {
      // 有子节点，递归输出
      builder.append(">\n");
      for (XNode child : children) {
        indent(builder, level + 1);
        child.toString(builder, level + 1);
      }
      indent(builder, level);
      builder.append("</");
      builder.append(name);
      builder.append(">");
    } else if (body != null) {
      // 有文本内容
      builder.append(">");
      builder.append(body);
      builder.append("</");
      builder.append(name);
      builder.append(">");
    } else {
      // 自闭合标签
      builder.append("/>");
      indent(builder, level);
    }
    builder.append("\n");
  }

  /**
   * 添加缩进。
   *
   * @param builder 字符串构建器
   * @param level   缩进层级
   */
  private void indent(StringBuilder builder, int level) {
    for (int i = 0; i < level; i++) {
      builder.append("    ");
    }
  }

  /**
   * 解析节点的属性。
   *
   * @param n DOM节点
   * @return 解析后的属性Properties
   */
  private Properties parseAttributes(Node n) {
    Properties attributes = new Properties();
    NamedNodeMap attributeNodes = n.getAttributes();
    if (attributeNodes != null) {
      for (int i = 0; i < attributeNodes.getLength(); i++) {
        Node attribute = attributeNodes.item(i);
        // 解析属性值中的变量占位符
        String value = PropertyParser.parse(attribute.getNodeValue(), variables);
        attributes.put(attribute.getNodeName(), value);
      }
    }
    return attributes;
  }

  /**
   * 解析节点的文本内容。
   * 优先获取直接文本数据，若无则遍历子节点查找。
   *
   * @param node DOM节点
   * @return 解析后的文本内容
   */
  private String parseBody(Node node) {
    String data = getBodyData(node);
    if (data == null) {
      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        data = getBodyData(child);
        if (data != null) {
          break;
        }
      }
    }
    return data;
  }

  /**
   * 从子节点获取文本数据。
   *
   * @param child DOM子节点
   * @return 文本数据，若不是文本节点则返回null
   */
  private String getBodyData(Node child) {
    // 仅处理CDATA和文本节点
    if (child.getNodeType() == Node.CDATA_SECTION_NODE
      || child.getNodeType() == Node.TEXT_NODE) {
      String data = ((CharacterData) child).getData();
      // 解析文本中的变量占位符
      data = PropertyParser.parse(data, variables);
      return data;
    }
    return null;
  }

}
