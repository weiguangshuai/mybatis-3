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

/**
 * 对象包装器工厂接口，负责为不同类型的对象创建对应的包装器。
 *
 * @author Clinton Begin
 */
public interface ObjectWrapperFactory {

  /**
   * 判断是否存在指定对象的包装器。
   *
   * @param object 待检查的对象
   * @return 存在对应包装器返回 true，否则返回 false
   */
  boolean hasWrapperFor(Object object);

  /**
   * 获取指定对象的包装器实例。
   *
   * @param metaObject 元对象，包含对象的元信息
   * @param object     待包装的目标对象
   * @return 对应的 ObjectWrapper 实例
   */
  ObjectWrapper getWrapperFor(MetaObject metaObject, Object object);

}
