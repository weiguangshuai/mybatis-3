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
package org.apache.ibatis.datasource.pooled;

import java.util.ArrayList;
import java.util.List;

/**
 * 连接池状态管理类，统计和跟踪连接池的运行指标。
 *
 * @author Clinton Begin
 */
public class PoolState {

  /**
   * 关联的数据源
   */
  protected PooledDataSource dataSource;

  /**
   * 空闲连接列表
   */
  protected final List<PooledConnection> idleConnections = new ArrayList<>();
  /**
   * 活跃连接列表
   */
  protected final List<PooledConnection> activeConnections = new ArrayList<>();
  /**
   * 获取连接的请求总数
   */
  protected long requestCount = 0;
  /**
   * 累计请求处理时间（毫秒）
   */
  protected long accumulatedRequestTime = 0;
  /**
   * 累计连接检出时间（毫秒）
   */
  protected long accumulatedCheckoutTime = 0;
  /**
   * 被强制关闭的超时连接数量
   */
  protected long claimedOverdueConnectionCount = 0;
  /**
   * 超时连接的累计检出时间（毫秒）
   */
  protected long accumulatedCheckoutTimeOfOverdueConnections = 0;
  /**
   * 线程等待连接的总时间（毫秒）
   */
  protected long accumulatedWaitTime = 0;
  /**
   * 线程因无可用连接而等待的次数
   */
  protected long hadToWaitCount = 0;
  /**
   * 无效连接（已损坏）数量
   */
  protected long badConnectionCount = 0;

  /**
   * 构造方法，初始化连接池状态。
   *
   * @param dataSource 数据源
   */
  public PoolState(PooledDataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * 获取连接请求总数。
   *
   * @return 请求总数
   */
  public synchronized long getRequestCount() {
    return requestCount;
  }

  /**
   * 获取平均请求处理时间。
   *
   * @return 平均处理时间（毫秒），无请求时返回0
   */
  public synchronized long getAverageRequestTime() {
    return requestCount == 0 ? 0 : accumulatedRequestTime / requestCount;
  }

  /**
   * 获取平均等待时间。
   *
   * @return 平均等待时间（毫秒），无等待时返回0
   */
  public synchronized long getAverageWaitTime() {
    return hadToWaitCount == 0 ? 0 : accumulatedWaitTime / hadToWaitCount;

  }

  /**
   * 获取线程等待次数。
   *
   * @return 等待次数
   */
  public synchronized long getHadToWaitCount() {
    return hadToWaitCount;
  }

  /**
   * 获取无效连接数量。
   *
   * @return 坏连接数量
   */
  public synchronized long getBadConnectionCount() {
    return badConnectionCount;
  }

  /**
   * 获取被强制关闭的超时连接数量。
   *
   * @return 超时连接数量
   */
  public synchronized long getClaimedOverdueConnectionCount() {
    return claimedOverdueConnectionCount;
  }

  /**
   * 获取超时连接的平均检出时间。
   *
   * @return 平均检出时间（毫秒），无超时连接时返回0
   */
  public synchronized long getAverageOverdueCheckoutTime() {
    return claimedOverdueConnectionCount == 0 ? 0 : accumulatedCheckoutTimeOfOverdueConnections / claimedOverdueConnectionCount;
  }

  /**
   * 获取平均连接检出时间。
   *
   * @return 平均检出时间（毫秒），无请求时返回0
   */
  public synchronized long getAverageCheckoutTime() {
    return requestCount == 0 ? 0 : accumulatedCheckoutTime / requestCount;
  }

  /**
   * 获取空闲连接数量。
   *
   * @return 空闲连接数
   */
  public synchronized int getIdleConnectionCount() {
    return idleConnections.size();
  }

  /**
   * 获取活跃连接数量。
   *
   * @return 活跃连接数
   */
  public synchronized int getActiveConnectionCount() {
    return activeConnections.size();
  }

  /**
   * 生成连接池状态报告，包含配置信息和运行时统计。
   *
   * @return 格式化的状态报告字符串
   */
  @Override
  public synchronized String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("\n===CONFIGURATION==============================================");
    builder.append("\n jdbcDriver                     ").append(dataSource.getDriver());
    builder.append("\n jdbcUrl                        ").append(dataSource.getUrl());
    builder.append("\n jdbcUsername                   ").append(dataSource.getUsername());
    // 密码脱敏处理
    builder.append("\n jdbcPassword                   ").append(dataSource.getPassword() == null ? "NULL" : "************");
    builder.append("\n poolMaxActiveConnections       ").append(dataSource.poolMaximumActiveConnections);
    builder.append("\n poolMaxIdleConnections         ").append(dataSource.poolMaximumIdleConnections);
    builder.append("\n poolMaxCheckoutTime            ").append(dataSource.poolMaximumCheckoutTime);
    builder.append("\n poolTimeToWait                 ").append(dataSource.poolTimeToWait);
    builder.append("\n poolPingEnabled                ").append(dataSource.poolPingEnabled);
    builder.append("\n poolPingQuery                  ").append(dataSource.poolPingQuery);
    builder.append("\n poolPingConnectionsNotUsedFor  ").append(dataSource.poolPingConnectionsNotUsedFor);
    builder.append("\n ---STATUS-----------------------------------------------------");
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
