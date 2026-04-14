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

import org.apache.ibatis.io.Resources;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * MyBatis DTD 的离线实体解析器。
 * 将 XML 中的公共/系统标识符解析为本地 DTD 文件，实现脱机验证。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class XMLMapperEntityResolver implements EntityResolver {

  /**
   * iBatis 配置文件 DTD 的系统标识符
   */
  private static final String IBATIS_CONFIG_SYSTEM = "ibatis-3-config.dtd";
  /**
   * iBatis 映射文件 DTD 的系统标识符
   */
  private static final String IBATIS_MAPPER_SYSTEM = "ibatis-3-mapper.dtd";
  /**
   * MyBatis 配置文件 DTD 的系统标识符
   */
  private static final String MYBATIS_CONFIG_SYSTEM = "mybatis-3-config.dtd";
  /**
   * MyBatis 映射文件 DTD 的系统标识符
   */
  private static final String MYBATIS_MAPPER_SYSTEM = "mybatis-3-mapper.dtd";

  /**
   * MyBatis 配置文件 DTD 的类路径
   */
  private static final String MYBATIS_CONFIG_DTD = "org/apache/ibatis/builder/xml/mybatis-3-config.dtd";
  /**
   * MyBatis 映射文件 DTD 的类路径
   */
  private static final String MYBATIS_MAPPER_DTD = "org/apache/ibatis/builder/xml/mybatis-3-mapper.dtd";

  /**
   * 将公共/系统 DTD 标识符解析为本地 DTD 文件。
   *
   * @param publicId 公共标识符（PUBLIC 关键字后的内容）
   * @param systemId 系统标识符（公共标识符后的内容）
   * @return DTD 的 InputSource，若无法识别则返回 null
   * @throws SAXException 解析失败时抛出
   */
  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
    try {
      // 解析配置文件或映射文件的 DTD
      if (systemId != null) {
        String lowerCaseSystemId = systemId.toLowerCase(Locale.ENGLISH);
        // 匹配 MyBatis/iBatis 配置文件 DTD
        if (lowerCaseSystemId.contains(MYBATIS_CONFIG_SYSTEM) || lowerCaseSystemId.contains(IBATIS_CONFIG_SYSTEM)) {
          return getInputSource(MYBATIS_CONFIG_DTD, publicId, systemId);
          // 匹配 MyBatis/iBatis 映射文件 DTD
        } else if (lowerCaseSystemId.contains(MYBATIS_MAPPER_SYSTEM) || lowerCaseSystemId.contains(IBATIS_MAPPER_SYSTEM)) {
          return getInputSource(MYBATIS_MAPPER_DTD, publicId, systemId);
        }
      }
      // 无法识别的 DTD，返回 null 使用默认解析方式
      return null;
    } catch (Exception e) {
      throw new SAXException(e.toString());
    }
  }

  /**
   * 根据 DTD 路径创建 InputSource。
   *
   * @param path     DTD 文件的类路径
   * @param publicId 公共标识符
   * @param systemId 系统标识符
   * @return InputSource 对象，若加载失败则返回 null
   */
  private InputSource getInputSource(String path, String publicId, String systemId) {
    InputSource source = null;
    if (path != null) {
      try {
        // 从类路径加载 DTD 文件
        InputStream in = Resources.getResourceAsStream(path);
        source = new InputSource(in);
        source.setPublicId(publicId);
        source.setSystemId(systemId);
      } catch (IOException e) {
        // DTD 加载失败时返回 null，后续会使用默认解析方式
      }
    }
    return source;
  }

}
