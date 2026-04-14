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

import org.apache.ibatis.builder.BuilderException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 基于 XPath 的 XML 解析器，封装 DOM 解析和 XPath 查询能力。
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XPathParser {

  /**
   * 已解析的 DOM 文档对象
   */
  private final Document document;

  /**
   * 是否启用 XML -validation（验证）模式
   */
  private boolean validation;

  /**
   * XML 实体解析器，用于解析 DTD/Schema 等外部实体
   */
  private EntityResolver entityResolver;

  /**
   * 属性占位符变量集合，用于替换 XML 中的 ${variable} 占位符
   */
  private Properties variables;

  /**
   * XPath 引擎实例，用于执行 XPath 表达式查询
   */
  private XPath xpath;

  /**
   * 从字符串构造 XPathParser。
   *
   * @param xml XML 字符串内容
   */
  public XPathParser(String xml) {
    commonConstructor(false, null, null);
    this.document = createDocument(new InputSource(new StringReader(xml)));
  }

  /**
   * 从 Reader 构造 XPathParser。
   *
   * @param reader 用于读取 XML 内容的 Reader
   */
  public XPathParser(Reader reader) {
    commonConstructor(false, null, null);
    this.document = createDocument(new InputSource(reader));
  }

  /**
   * 从 InputStream 构造 XPathParser。
   *
   * @param inputStream 用于读取 XML 内容的输入流
   */
  public XPathParser(InputStream inputStream) {
    commonConstructor(false, null, null);
    this.document = createDocument(new InputSource(inputStream));
  }

  /**
   * 从已存在的 DOM Document 构造 XPathParser。
   *
   * @param document 已解析的 DOM 文档对象
   */
  public XPathParser(Document document) {
    commonConstructor(false, null, null);
    this.document = document;
  }

  /**
   * 从字符串构造 XPathParser，并指定是否验证。
   *
   * @param xml        XML 字符串内容
   * @param validation 是否启用 XML 验证
   */
  public XPathParser(String xml, boolean validation) {
    commonConstructor(validation, null, null);
    this.document = createDocument(new InputSource(new StringReader(xml)));
  }

  /**
   * 从 Reader 构造 XPathParser，并指定是否验证。
   *
   * @param reader     用于读取 XML 内容的 Reader
   * @param validation 是否启用 XML 验证
   */
  public XPathParser(Reader reader, boolean validation) {
    commonConstructor(validation, null, null);
    this.document = createDocument(new InputSource(reader));
  }

  /**
   * 从 InputStream 构造 XPathParser，并指定是否验证。
   *
   * @param inputStream 用于读取 XML 内容的输入流
   * @param validation  是否启用 XML 验证
   */
  public XPathParser(InputStream inputStream, boolean validation) {
    commonConstructor(validation, null, null);
    this.document = createDocument(new InputSource(inputStream));
  }

  /**
   * 从 DOM Document 构造 XPathParser，并指定是否验证。
   *
   * @param document   已解析的 DOM 文档对象
   * @param validation 是否启用 XML 验证
   */
  public XPathParser(Document document, boolean validation) {
    commonConstructor(validation, null, null);
    this.document = document;
  }

  /**
   * 从字符串构造 XPathParser，指定验证和变量属性。
   *
   * @param xml        XML 字符串内容
   * @param validation 是否启用 XML 验证
   * @param variables  属性变量集合，用于替换 ${variable} 占位符
   */
  public XPathParser(String xml, boolean validation, Properties variables) {
    commonConstructor(validation, variables, null);
    this.document = createDocument(new InputSource(new StringReader(xml)));
  }

  /**
   * 从 Reader 构造 XPathParser，指定验证和变量属性。
   *
   * @param reader     用于读取 XML 内容的 Reader
   * @param validation 是否启用 XML 验证
   * @param variables  属性变量集合，用于替换 ${variable} 占位符
   */
  public XPathParser(Reader reader, boolean validation, Properties variables) {
    commonConstructor(validation, variables, null);
    this.document = createDocument(new InputSource(reader));
  }

  /**
   * 从 InputStream 构造 XPathParser，指定验证和变量属性。
   *
   * @param inputStream 用于读取 XML 内容的输入流
   * @param validation  是否启用 XML 验证
   * @param variables   属性变量集合，用于替换 ${variable} 占位符
   */
  public XPathParser(InputStream inputStream, boolean validation, Properties variables) {
    commonConstructor(validation, variables, null);
    this.document = createDocument(new InputSource(inputStream));
  }

  /**
   * 从 DOM Document 构造 XPathParser，指定验证和变量属性。
   *
   * @param document   已解析的 DOM 文档对象
   * @param validation 是否启用 XML 验证
   * @param variables  属性变量集合，用于替换 ${variable} 占位符
   */
  public XPathParser(Document document, boolean validation, Properties variables) {
    commonConstructor(validation, variables, null);
    this.document = document;
  }

  /**
   * 从字符串构造 XPathParser完整配置。
   *
   * @param xml            XML 字符串内容
   * @param validation     是否启用 XML 验证
   * @param variables      属性变量集合
   * @param entityResolver XML 实体解析器
   */
  public XPathParser(String xml, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    this.document = createDocument(new InputSource(new StringReader(xml)));
  }

  /**
   * 从 Reader 构造 XPathParser完整配置。
   *
   * @param reader         用于读取 XML 内容的 Reader
   * @param validation     是否启用 XML 验证
   * @param variables      属性变量集合
   * @param entityResolver XML 实体解析器
   */
  public XPathParser(Reader reader, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    this.document = createDocument(new InputSource(reader));
  }

  /**
   * 从 InputStream 构造 XPathParser完整配置。
   *
   * @param inputStream    用于读取 XML 内容的输入流
   * @param validation     是否启用 XML 验证
   * @param variables      属性变量集合
   * @param entityResolver XML 实体解析器
   */
  public XPathParser(InputStream inputStream, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    this.document = createDocument(new InputSource(inputStream));
  }

  /**
   * 从 DOM Document 构造 XPathParser完整配置。
   *
   * @param document       已解析的 DOM 文档对象
   * @param validation     是否启用 XML 验证
   * @param variables      属性变量集合
   * @param entityResolver XML 实体解析器
   */
  public XPathParser(Document document, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    this.document = document;
  }

  /**
   * 设置属性变量集合，用于替换 XML 中的 ${variable} 占位符。
   *
   * @param variables 属性变量集合
   */
  public void setVariables(Properties variables) {
    this.variables = variables;
  }

  /**
   * 在根节点上执行 XPath 表达式，返回字符串结果。
   *
   * @param expression XPath 表达式
   * @return 字符串结果，会自动替换 ${variable} 占位符
   */
  public String evalString(String expression) {
    return evalString(document, expression);
  }

  /**
   * 在指定节点上执行 XPath 表达式，返回字符串结果。
   *
   * @param root       起始节点
   * @param expression XPath 表达式
   * @return 字符串结果，会自动替换 ${variable} 占位符
   */
  public String evalString(Object root, String expression) {
    String result = (String) evaluate(expression, root, XPathConstants.STRING);
    result = PropertyParser.parse(result, variables);
    return result;
  }

  /**
   * 在根节点上执行 XPath 表达式，返回布尔结果。
   *
   * @param expression XPath 表达式
   * @return 布尔结果
   */
  public Boolean evalBoolean(String expression) {
    return evalBoolean(document, expression);
  }

  /**
   * 在指定节点上执行 XPath 表达式，返回布尔结果。
   *
   * @param root       起始节点
   * @param expression XPath 表达式
   * @return 布尔结果
   */
  public Boolean evalBoolean(Object root, String expression) {
    return (Boolean) evaluate(expression, root, XPathConstants.BOOLEAN);
  }

  /**
   * 在根节点上执行 XPath 表达式，返回 Short 结果。
   *
   * @param expression XPath 表达式
   * @return Short 结果
   */
  public Short evalShort(String expression) {
    return evalShort(document, expression);
  }

  /**
   * 在指定节点上执行 XPath 表达式，返回 Short 结果。
   *
   * @param root       起始节点
   * @param expression XPath 表达式
   * @return Short 结果
   */
  public Short evalShort(Object root, String expression) {
    return Short.valueOf(evalString(root, expression));
  }

  /**
   * 在根节点上执行 XPath 表达式，返回 Integer 结果。
   *
   * @param expression XPath 表达式
   * @return Integer 结果
   */
  public Integer evalInteger(String expression) {
    return evalInteger(document, expression);
  }

  /**
   * 在指定节点上执行 XPath 表达式，返回 Integer 结果。
   *
   * @param root       起始节点
   * @param expression XPath 表达式
   * @return Integer 结果
   */
  public Integer evalInteger(Object root, String expression) {
    return Integer.valueOf(evalString(root, expression));
  }

  /**
   * 在根节点上执行 XPath 表达式，返回 Long 结果。
   *
   * @param expression XPath 表达式
   * @return Long 结果
   */
  public Long evalLong(String expression) {
    return evalLong(document, expression);
  }

  /**
   * 在指定节点上执行 XPath 表达式，返回 Long 结果。
   *
   * @param root       起始节点
   * @param expression XPath 表达式
   * @return Long 结果
   */
  public Long evalLong(Object root, String expression) {
    return Long.valueOf(evalString(root, expression));
  }

  /**
   * 在根节点上执行 XPath 表达式，返回 Float 结果。
   *
   * @param expression XPath 表达式
   * @return Float 结果
   */
  public Float evalFloat(String expression) {
    return evalFloat(document, expression);
  }

  /**
   * 在指定节点上执行 XPath 表达式，返回 Float 结果。
   *
   * @param root       起始节点
   * @param expression XPath 表达式
   * @return Float 结果
   */
  public Float evalFloat(Object root, String expression) {
    return Float.valueOf(evalString(root, expression));
  }

  /**
   * 在根节点上执行 XPath 表达式，返回 Double 结果。
   *
   * @param expression XPath 表达式
   * @return Double 结果
   */
  public Double evalDouble(String expression) {
    return evalDouble(document, expression);
  }

  /**
   * 在指定节点上执行 XPath 表达式，返回 Double 结果。
   *
   * @param root       起始节点
   * @param expression XPath 表达式
   * @return Double 结果
   */
  public Double evalDouble(Object root, String expression) {
    return (Double) evaluate(expression, root, XPathConstants.NUMBER);
  }

  /**
   * 在根节点上执行 XPath 表达式，返回节点列表。
   *
   * @param expression XPath 表达式
   * @return XNode 列表
   */
  public List<XNode> evalNodes(String expression) {
    return evalNodes(document, expression);
  }

  /**
   * 在指定节点上执行 XPath 表达式，返回节点列表。
   *
   * @param root       起始节点
   * @param expression XPath 表达式
   * @return XNode 列表
   */
  public List<XNode> evalNodes(Object root, String expression) {
    List<XNode> xnodes = new ArrayList<>();
    NodeList nodes = (NodeList) evaluate(expression, root, XPathConstants.NODESET);
    for (int i = 0; i < nodes.getLength(); i++) {
      xnodes.add(new XNode(this, nodes.item(i), variables));
    }
    return xnodes;
  }

  /**
   * 在根节点上执行 XPath 表达式，返回单个节点。
   *
   * @param expression XPath 表达式
   * @return XNode 节点，若无匹配返回 null
   */
  public XNode evalNode(String expression) {
    return evalNode(document, expression);
  }

  /**
   * 在指定节点上执行 XPath 表达式，返回单个节点。
   *
   * @param root       起始节点
   * @param expression XPath 表达式
   * @return XNode 节点，若无匹配返回 null
   */
  public XNode evalNode(Object root, String expression) {
    Node node = (Node) evaluate(expression, root, XPathConstants.NODE);
    if (node == null) {
      return null;
    }
    return new XNode(this, node, variables);
  }

  /**
   * 执行 XPath 表达式并返回指定类型的结果。
   *
   * @param expression XPath 表达式
   * @param root       起始节点（Document 或 Node）
   * @param returnType 返回类型（STRING、BOOLEAN、NUMBER、NODESET、NODE）
   * @return XPath 执行结果
   */
  private Object evaluate(String expression, Object root, QName returnType) {
    try {
      return xpath.evaluate(expression, root, returnType);
    } catch (Exception e) {
      throw new BuilderException("Error evaluating XPath.  Cause: " + e, e);
    }
  }

  /**
   * 创建 DOM 文档对象，解析 XML 输入源。
   *
   * @param inputSource XML 输入源
   * @return 解析后的 DOM 文档
   */
  private Document createDocument(InputSource inputSource) {
    // 重要：此方法必须在 commonConstructor 之后调用
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      // 启用安全处理模式，防止 XXE 攻击
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setValidating(validation);

      factory.setNamespaceAware(false);
      factory.setIgnoringComments(true);
      factory.setIgnoringElementContentWhitespace(false);
      factory.setCoalescing(false);
      factory.setExpandEntityReferences(true);

      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver(entityResolver);
      // 设置错误处理器，将 SAX 解析错误转为异常
      builder.setErrorHandler(new ErrorHandler() {
        @Override
        public void error(SAXParseException exception) throws SAXException {
          throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
          throw exception;
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
          // NOP
        }
      });
      return builder.parse(inputSource);
    } catch (Exception e) {
      throw new BuilderException("Error creating document instance.  Cause: " + e, e);
    }
  }

  /**
   * 通用构造函数逻辑，初始化验证、变量和实体解析器配置。
   *
   * @param validation     是否启用 XML 验证
   * @param variables      属性变量集合
   * @param entityResolver XML 实体解析器
   */
  private void commonConstructor(boolean validation, Properties variables, EntityResolver entityResolver) {
    this.validation = validation;
    this.entityResolver = entityResolver;
    this.variables = variables;
    XPathFactory factory = XPathFactory.newInstance();
    this.xpath = factory.newXPath();
  }

}
