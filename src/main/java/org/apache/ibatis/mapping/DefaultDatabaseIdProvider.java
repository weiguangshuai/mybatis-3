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
 * 已废弃的数据库标识提供器，现推荐使用 {@link VendorDatabaseIdProvider}。
 * 用于根据数据库产品名称返回对应的数据库 ID，以便在 SQL 映射中根据不同数据库执行不同的语句。
 *
 * @author Eduardo Macarron
 */
@Deprecated
public class DefaultDatabaseIdProvider extends VendorDatabaseIdProvider {
}
