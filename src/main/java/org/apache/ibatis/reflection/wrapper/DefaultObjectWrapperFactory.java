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
package org.apache.ibatis.reflection.wrapper;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectionException;

/**
 * 默认的 ObjectWrapper 工厂实现。
 * 该工厂不提供任何自定义的 ObjectWrapper，所有方法均返回默认值或抛出异常。
 *
 * @author Clinton Begin
 */
public class DefaultObjectWrapperFactory implements ObjectWrapperFactory {

  /**
   * 检查是否为指定对象提供了自定义的 ObjectWrapper。
   * 默认实现不支持任何对象，始终返回 false。
   *
   * @param object 要检查的对象
   * @return 始终返回 false，表示不支持自定义包装器
   */
  @Override
  public boolean hasWrapperFor(Object object) {
    return false;
  }

  /**
   * 获取指定对象的 ObjectWrapper。
   * 默认实现不应被调用，此处抛出异常以防止误用。
   *
   * @param metaObject 对象的元对象
   * @param object 要包装的对象
   * @return 永不返回，始终抛出异常
   * @throws ReflectionException 总是抛出，表示不支持获取包装器
   */
  @Override
  public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
    // 默认工厂不应被调用获取包装器，抛出异常提示配置错误
    throw new ReflectionException("The DefaultObjectWrapperFactory should never be called to provide an ObjectWrapper.");
  }

}
