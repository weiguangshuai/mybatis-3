/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.reflection.wrapper;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectionException;

/**
 * 默认的对象包装器工厂实现，不提供任何自定义包装器。
 *
 * @author Clinton Begin
 */
public class DefaultObjectWrapperFactory implements ObjectWrapperFactory {

  /**
   * 检查是否存在指定对象的包装器。
   * 默认实现不支持任何自定义包装器，始终返回 false。
   *
   * @param object 要检查的对象
   * @return 始终返回 false
   */
  @Override
  public boolean hasWrapperFor(Object object) {
    return false;
  }

  /**
   * 获取指定对象的包装器。
   * 默认实现不支持任何自定义包装器，调用此方法会抛出异常。
   *
   * @param metaObject 元对象，包含对象的元信息
   * @param object 要包装的对象
   * @return 永不返回，会抛出异常
   * @throws ReflectionException 总是抛出，表示不支持该操作
   */
  @Override
  public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
    // 默认工厂不支持自定义包装器，该方法不应被调用
    throw new ReflectionException("The DefaultObjectWrapperFactory should never be called to provide an ObjectWrapper.");
  }

}
