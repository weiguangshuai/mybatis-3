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
package org.apache.ibatis.datasource.pooled;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Clinton Begin
 * @since 3.0
 *
 * 连接池状态管理类，统计并维护连接池的各项运行指标
 */
public class PoolState {

  /** 所属的数据源 */
  protected PooledDataSource dataSource;

  /** 空闲连接列表 */
  protected final List<PooledConnection> idleConnections = new ArrayList<PooledConnection>();
  /** 活跃连接列表 */
  protected final List<PooledConnection> activeConnections = new ArrayList<PooledConnection>();
  /** 获取连接的请求总数 */
  protected long requestCount = 0;
  /** 累计请求耗时（毫秒） */
  protected long accumulatedRequestTime = 0;
  /** 累计连接检出时间（毫秒） */
  protected long accumulatedCheckoutTime = 0;
  /** 逾期连接被强制关闭的次数 */
  protected long claimedOverdueConnectionCount = 0;
  /** 逾期连接的累计检出时间（毫秒） */
  protected long accumulatedCheckoutTimeOfOverdueConnections = 0;
  /** 累计等待时间（毫秒） */
  protected long accumulatedWaitTime = 0;
  /** 不得不等待的次数 */
  protected long hadToWaitCount = 0;
  /** 无效连接数量 */
  protected long badConnectionCount = 0;

  /**
   * 构造方法
   * @param dataSource 所属的数据源
   */
  public PoolState(PooledDataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * 获取连接请求总数
   * @return 请求总数
   */
  public synchronized long getRequestCount() {
    return requestCount;
  }

  /**
   * 获取平均请求耗时
   * @return 平均耗时（毫秒），无请求时返回0
   */
  public synchronized long getAverageRequestTime() {
    return requestCount == 0 ? 0 : accumulatedRequestTime / requestCount;
  }

  /**
   * 获取平均等待时间
   * @return 平均等待时间（毫秒），无等待时返回0
   */
  public synchronized long getAverageWaitTime() {
    return hadToWaitCount == 0 ? 0 : accumulatedWaitTime / hadToWaitCount;

  }

  /**
   * 获取不得不等待的次数
   * @return 等待次数
   */
  public synchronized long getHadToWaitCount() {
    return hadToWaitCount;
  }

  /**
   * 获取无效连接数量
   * @return 坏连接数
   */
  public synchronized long getBadConnectionCount() {
    return badConnectionCount;
  }

  /**
   * 获取逾期连接被关闭的次数
   * @return 逾期连接数
   */
  public synchronized long getClaimedOverdueConnectionCount() {
    return claimedOverdueConnectionCount;
  }

  /**
   * 获取平均逾期连接检出时间
   * @return 平均时间（毫秒），无逾期连接时返回0
   */
  public synchronized long getAverageOverdueCheckoutTime() {
    return claimedOverdueConnectionCount == 0 ? 0 : accumulatedCheckoutTimeOfOverdueConnections / claimedOverdueConnectionCount;
  }

  /**
   * 获取平均连接检出时间
   * @return 平均时间（毫秒），无请求时返回0
   */
  public synchronized long getAverageCheckoutTime() {
    return requestCount == 0 ? 0 : accumulatedCheckoutTime / requestCount;
  }


  /**
   * 获取当前空闲连接数
   * @return 空闲连接数
   */
  public synchronized int getIdleConnectionCount() {
    return idleConnections.size();
  }

  /**
   * 获取当前活跃连接数
   * @return 活跃连接数
   */
  public synchronized int getActiveConnectionCount() {
    return activeConnections.size();
  }

  /**
   * 生成连接池状态报告
   * @return 包含配置信息和运行统计的字符串
   */
  @Override
  public synchronized String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("\n===CONFINGURATION==============================================");
    builder.append("\n jdbcDriver                     ").append(dataSource.getDriver());
    builder.append("\n jdbcUrl                        ").append(dataSource.getUrl());
    builder.append("\n jdbcUsername                   ").append(dataSource.getUsername());
    builder.append("\n jdbcPassword                   ").append((dataSource.getPassword() == null ? "NULL" : "************")); // 隐藏密码以保证安全
    builder.append("\n poolMaxActiveConnections       ").append(dataSource.poolMaximumActiveConnections);
    builder.append("\n poolMaxIdleConnections         ").append(dataSource.poolMaximumIdleConnections);
    builder.append("\n poolMaxCheckoutTime            ").append(dataSource.poolMaximumCheckoutTime);
    builder.append("\n poolTimeToWait                 ").append(dataSource.poolTimeToWait);
    builder.append("\n poolPingEnabled                ").append(dataSource.poolPingEnabled);
    builder.append("\n poolPingQuery                  ").append(dataSource.poolPingQuery);
    builder.append("\n poolPingConnectionsNotUsedFor  ").append(dataSource.poolPingConnectionsNotUsedFor);
    builder.append("\n ---STATUS-----------------------------------------------------"); // 配置与状态分隔
    builder.append("\n activeConnections              ").append(getActiveConnectionCount());
    builder.append("\n idleConnections                ").append(getIdleConnectionCount());
    builder.append("\n requestCount                   ").append(getRequestCount());
    builder.append("\n averageRequestTime             ").append(getAverageRequestTime());
    builder.append("\n averageCheckoutTime            ").append(getAverageCheckoutTime());
    builder.append("\n claimedOverdue                 ").append(getClaimedOverdueConnectionCount());
    builder.append("\n averageOverdueCheckoutTime     ").append(getAverageOverdueCheckoutTime());
    builder.append("\n hadToWait                      ").append(getHadToWaitCount());
    builder.append("\n averageWaitTime                ").append(getAverageWaitTime());
    builder.append("\n badConnectionCount             ").append(getBadConnectionCount());
    builder.append("\n===============================================================");
    return builder.toString();
  }

}
