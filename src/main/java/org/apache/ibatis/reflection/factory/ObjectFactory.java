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
package org.apache.ibatis.reflection.factory;

import java.util.List;
import java.util.Properties;

/**
 * ObjectFactory 接口：负责创建 MyBatis 运行过程中需要的各种对象实例
 *
 * @author Clinton Begin
 * @see DefaultObjectFactory
 */
public interface ObjectFactory {

  /**
   * 设置配置属性，供 ObjectFactory 初始化时使用
   * @param properties 配置属性
   */
  default void setProperties(Properties properties) {
    // NOP - 默认实现为空，由具体实现类覆盖
  }

  /**
   * 使用无参构造函数创建对象实例
   *
   * @param <T> 对象类型
   * @param type 对象 Class
   * @return 创建的对象实例
   */
  <T> T create(Class<T> type);

  /**
   * 使用指定构造函数和参数创建对象实例
   *
   * @param <T> 对象类型
   * @param type 对象 Class
   * @param constructorArgTypes 构造函数参数类型列表
   * @param constructorArgs 构造函数参数值列表
   * @return 创建的对象实例
   */
  <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

  /**
   * 判断指定类型是否为集合类型
   * 主要用于支持 Scala 集合等非 java.util.Collection 的集合实现
   *
   * @param <T> 对象类型
   * @param type 对象 Class
   * @return 如果是集合类型返回 true
   * @since 3.1.0
   */
  <T> boolean isCollection(Class<T> type);

}
