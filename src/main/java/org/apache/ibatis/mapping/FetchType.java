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
package org.apache.ibatis.mapping;

/**
 * 定义关联查询的加载策略。
 *
 * @author Eduardo Macarron
 */
public enum FetchType {
  /** 延迟加载：仅在访问关联属性时才加载 */
  LAZY,
  /** 立即加载：查询时立即加载所有关联数据 */
  EAGER,
  /** 默认：继承上层配置 */
  DEFAULT
}
