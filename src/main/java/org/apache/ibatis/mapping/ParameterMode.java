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
 * 参数模式枚举，定义存储过程调用中参数的方向类型。
 *
 * @author Clinton Begin
 */
public enum ParameterMode {
  /** 输入参数：仅作为调用入参使用 */
  IN,
  /** 输出参数：仅作为返回值使用 */
  OUT,
  /** 输入输出参数：既作为入参又作为返回值使用 */
  INOUT
}
