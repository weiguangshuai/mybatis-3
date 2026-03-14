/**
 *    Copyright 2009-2016 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection.factory;

import java.util.List;
import java.util.Properties;

/**
 * ObjectFactory 接口.
 * 定义对象创建工厂规范，MyBatis 使用它来创建所需的各种对象实例.
 *
 * @author Clinton Begin
 */
public interface ObjectFactory {

  /**
   * 设置配置属性.
   * @param properties 配置属性集合
   */
  void setProperties(Properties properties);

  /**
   * 使用无参构造函数创建对象实例.
   * @param type 要创建的对象类型
   * @return 创建的对象实例
   */
  <T> T create(Class<T> type);

  /**
   * 使用指定构造函数和参数创建对象实例.
   * @param type 对象类型
   * @param constructorArgTypes 构造函数参数类型列表
   * @param constructorArgs 构造函数参数值列表
   * @return 创建的对象实例
   */
  <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

  /**
   * 判断指定类型是否为集合类型.
   * 主要用于支持 Scala 集合等非 java.util.Collection 接口的集合类型.
   *
   * @param type 要检查的对象类型
   * @return 如果是集合类型返回 true，否则返回 false
   * @since 3.1.0
   */
  <T> boolean isCollection(Class<T> type);

}
