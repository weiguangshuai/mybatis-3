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
package org.apache.ibatis.builder.xml;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * 处理 XML mapper 中的 include 节点，将引用替换为实际的 SQL 片段。
 *
 * @author Frank D. Martinez [mnesarco]
 */
public class XMLIncludeTransformer {

  /**
   * MyBatis 全局配置对象
   */
  private final Configuration configuration;
  /**
   * Mapper 构建助手，用于解析和构建映射器
   */
  private final MapperBuilderAssistant builderAssistant;

  /**
   * 构造 Include 转换器。
   *
   * @param configuration    MyBatis 配置对象
   * @param builderAssistant Mapper 构建助手
   */
  public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
    this.configuration = configuration;
    this.builderAssistant = builderAssistant;
  }

  /**
   * 对指定节点应用 include 处理，将所有 include 节点替换为对应的 SQL 片段。
   *
   * @param source 要处理的 DOM 节点
   */
  public void applyIncludes(Node source) {
    // 初始化变量上下文，先加载全局配置变量
    Properties variablesContext = new Properties();
    Properties configurationVariables = configuration.getVariables();
    Optional.ofNullable(configurationVariables).ifPresent(variablesContext::putAll);
    applyIncludes(source, variablesContext, false);
  }

  /**
   * Recursively apply includes through all SQL fragments.
   *
   * @param source           Include node in DOM tree
   * @param variablesContext Current context for static variables with values
   * @param included         标记当前节点是否在 include 链中
   */
  private void applyIncludes(Node source, final Properties variablesContext, boolean included) {
    // 处理 include 节点：替换为 SQL 片段
    if ("include".equals(source.getNodeName())) {
      // 查找引用的 SQL 片段
      Node toInclude = findSqlFragment(getStringAttribute(source, "refid"), variablesContext);
      // 获取 include 节点中定义的局部变量
      Properties toIncludeContext = getVariablesContext(source, variablesContext);
      // 递归处理被包含的 SQL 片段
      applyIncludes(toInclude, toIncludeContext, true);
      // 跨文档引用需要导入节点
      if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
        toInclude = source.getOwnerDocument().importNode(toInclude, true);
      }
      // 替换 include 节点为实际的 SQL 片段
      source.getParentNode().replaceChild(toInclude, source);
      // 将片段的子节点提升到父节点层级
      while (toInclude.hasChildNodes()) {
        toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
      }
      // 移除空的片段节点
      toInclude.getParentNode().removeChild(toInclude);
    } else if (source.getNodeType() == Node.ELEMENT_NODE) {
      // 处理元素节点：替换属性中的变量
      if (included && !variablesContext.isEmpty()) {
        // 替换属性值中的变量占位符
        NamedNodeMap attributes = source.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
          Node attr = attributes.item(i);
          attr.setNodeValue(PropertyParser.parse(attr.getNodeValue(), variablesContext));
        }
      }
      // 递归处理子节点
      NodeList children = source.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        applyIncludes(children.item(i), variablesContext, included);
      }
    } else if (included && (source.getNodeType() == Node.TEXT_NODE || source.getNodeType() == Node.CDATA_SECTION_NODE)
      && !variablesContext.isEmpty()) {
      // 替换文本节点中的变量占位符
      source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
    }
  }

  /**
   * 根据 refid 查找对应的 SQL 片段节点。
   *
   * @param refid     SQL 片段的引用 ID
   * @param variables 变量上下文
   * @return 找到的 SQL 片段节点副本
   */
  private Node findSqlFragment(String refid, Properties variables) {
    // 解析 refid 中的变量
    refid = PropertyParser.parse(refid, variables);
    // 应用当前命名空间
    refid = builderAssistant.applyCurrentNamespace(refid, true);
    try {
      // 从配置中获取 SQL 片段并返回克隆节点
      XNode nodeToInclude = configuration.getSqlFragments().get(refid);
      return nodeToInclude.getNode().cloneNode(true);
    } catch (IllegalArgumentException e) {
      throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
    }
  }

  /**
   * 获取节点指定的字符串属性值。
   *
   * @param node 目标节点
   * @param name 属性名称
   * @return 属性值
   */
  private String getStringAttribute(Node node, String name) {
    return node.getAttributes().getNamedItem(name).getNodeValue();
  }

  /**
   * Read placeholders and their values from include node definition.
   *
   * @param node                      Include node instance
   * @param inheritedVariablesContext Current context used for replace variables in new variables values
   * @return variables context from include instance (no inherited values)
   */
  private Properties getVariablesContext(Node node, Properties inheritedVariablesContext) {
    // 用于存储 include 节点中定义的局部变量
    Map<String, String> declaredProperties = null;
    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        String name = getStringAttribute(n, "name");
        // 解析属性值中的变量引用
        String value = PropertyParser.parse(getStringAttribute(n, "value"), inheritedVariablesContext);
        if (declaredProperties == null) {
          declaredProperties = new HashMap<>();
        }
        // 检测重复定义
        if (declaredProperties.put(name, value) != null) {
          throw new BuilderException("Variable " + name + " defined twice in the same include definition");
        }
      }
    }
    // 无局部变量定义时返回继承的上下文
    if (declaredProperties == null) {
      return inheritedVariablesContext;
    } else {
      // 合并继承上下文和局部变量
      Properties newProperties = new Properties();
      newProperties.putAll(inheritedVariablesContext);
      newProperties.putAll(declaredProperties);
      return newProperties;
    }
  }
}
