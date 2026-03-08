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
package org.apache.ibatis.datasource.unpooled;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.ibatis.io.Resources;

/**
 * 非池化数据源实现。
 * 每次获取连接时都会直接向 JDBC 驱动申请新连接，不维护连接池状态，
 * 适用于简单场景、测试环境或由外部容器负责连接池管理的场景。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class UnpooledDataSource implements DataSource {
  
  /**
   * JDBC 驱动类加载器。
   * 为空时使用默认类加载逻辑，非空时优先通过该加载器加载驱动类。
   */
  private ClassLoader driverClassLoader;
  /**
   * 传递给 JDBC 驱动的连接属性（如连接参数、认证扩展参数）。
   */
  private Properties driverProperties;
  /**
   * 当前 JVM 已注册驱动缓存，key 为驱动类全名。
   * 使用并发容器保证并发读写安全，避免重复注册同一驱动。
   */
  private static Map<String, Driver> registeredDrivers = new ConcurrentHashMap<String, Driver>();

  /**
   * JDBC 驱动类全限定名。
   */
  private String driver;
  /**
   * JDBC 连接 URL。
   */
  private String url;
  /**
   * 默认连接用户名。
   */
  private String username;
  /**
   * 默认连接密码。
   */
  private String password;

  /**
   * 连接创建后是否强制设置自动提交；为 null 表示保持驱动默认值。
   */
  private Boolean autoCommit;
  /**
   * 连接创建后是否强制设置事务隔离级别；为 null 表示保持驱动默认值。
   */
  private Integer defaultTransactionIsolationLevel;

  static {
    // 启动时读取 DriverManager 已持有的驱动，避免重复注册。
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      registeredDrivers.put(driver.getClass().getName(), driver);
    }
  }

  /**
   * 创建空配置的数据源实例，参数可通过 setter 逐步注入。
   */
  public UnpooledDataSource() {
  }

  /**
   * 使用基础参数创建数据源。
   *
   * @param driver JDBC 驱动类名
   * @param url JDBC 连接 URL
   * @param username 用户名
   * @param password 密码
   */
  public UnpooledDataSource(String driver, String url, String username, String password) {
    this.driver = driver;
    this.url = url;
    this.username = username;
    this.password = password;
  }

  /**
   * 使用驱动属性创建数据源。
   *
   * @param driver JDBC 驱动类名
   * @param url JDBC 连接 URL
   * @param driverProperties 驱动属性
   */
  public UnpooledDataSource(String driver, String url, Properties driverProperties) {
    this.driver = driver;
    this.url = url;
    this.driverProperties = driverProperties;
  }

  /**
   * 使用指定类加载器与基础认证参数创建数据源。
   *
   * @param driverClassLoader 驱动类加载器
   * @param driver JDBC 驱动类名
   * @param url JDBC 连接 URL
   * @param username 用户名
   * @param password 密码
   */
  public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
    this.driverClassLoader = driverClassLoader;
    this.driver = driver;
    this.url = url;
    this.username = username;
    this.password = password;
  }

  /**
   * 使用指定类加载器与驱动属性创建数据源。
   *
   * @param driverClassLoader 驱动类加载器
   * @param driver JDBC 驱动类名
   * @param url JDBC 连接 URL
   * @param driverProperties 驱动属性
   */
  public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
    this.driverClassLoader = driverClassLoader;
    this.driver = driver;
    this.url = url;
    this.driverProperties = driverProperties;
  }

  /**
   * 使用当前配置的用户名/密码创建新连接。
   *
   * @return 新创建的 JDBC 连接
   * @throws SQLException 获取连接失败时抛出
   */
  @Override
  public Connection getConnection() throws SQLException {
    return doGetConnection(username, password);
  }

  /**
   * 使用调用方传入的用户名/密码创建新连接。
   *
   * @param username 用户名
   * @param password 密码
   * @return 新创建的 JDBC 连接
   * @throws SQLException 获取连接失败时抛出
   */
  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return doGetConnection(username, password);
  }

  /**
   * 设置 JDBC 登录超时时间（秒）。
   *
   * @param loginTimeout 登录超时时间（秒）
   * @throws SQLException 设置失败时抛出
   */
  @Override
  public void setLoginTimeout(int loginTimeout) throws SQLException {
    DriverManager.setLoginTimeout(loginTimeout);
  }

  /**
   * 获取 JDBC 登录超时时间（秒）。
   *
   * @return 登录超时时间（秒）
   * @throws SQLException 读取失败时抛出
   */
  @Override
  public int getLoginTimeout() throws SQLException {
    return DriverManager.getLoginTimeout();
  }

  /**
   * 设置 DriverManager 级别日志输出器。
   *
   * @param logWriter 日志输出器
   * @throws SQLException 设置失败时抛出
   */
  @Override
  public void setLogWriter(PrintWriter logWriter) throws SQLException {
    DriverManager.setLogWriter(logWriter);
  }

  /**
   * 获取 DriverManager 级别日志输出器。
   *
   * @return 当前日志输出器
   * @throws SQLException 读取失败时抛出
   */
  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return DriverManager.getLogWriter();
  }

  /**
   * 获取驱动类加载器。
   *
   * @return 驱动类加载器
   */
  public ClassLoader getDriverClassLoader() {
    return driverClassLoader;
  }

  /**
   * 设置驱动类加载器。
   *
   * @param driverClassLoader 驱动类加载器
   */
  public void setDriverClassLoader(ClassLoader driverClassLoader) {
    this.driverClassLoader = driverClassLoader;
  }

  /**
   * 获取驱动属性。
   *
   * @return 驱动属性
   */
  public Properties getDriverProperties() {
    return driverProperties;
  }

  /**
   * 设置驱动属性。
   *
   * @param driverProperties 驱动属性
   */
  public void setDriverProperties(Properties driverProperties) {
    this.driverProperties = driverProperties;
  }

  /**
   * 获取 JDBC 驱动类名。
   *
   * @return 驱动类名
   */
  public String getDriver() {
    return driver;
  }

  /**
   * 设置 JDBC 驱动类名。
   * 使用 synchronized 以保证配置更新与驱动初始化流程的可见性。
   *
   * @param driver 驱动类名
   */
  public synchronized void setDriver(String driver) {
    this.driver = driver;
  }

  /**
   * 获取 JDBC URL。
   *
   * @return JDBC URL
   */
  public String getUrl() {
    return url;
  }

  /**
   * 设置 JDBC URL。
   *
   * @param url JDBC URL
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * 获取默认用户名。
   *
   * @return 默认用户名
   */
  public String getUsername() {
    return username;
  }

  /**
   * 设置默认用户名。
   *
   * @param username 默认用户名
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * 获取默认密码。
   *
   * @return 默认密码
   */
  public String getPassword() {
    return password;
  }

  /**
   * 设置默认密码。
   *
   * @param password 默认密码
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * 获取自动提交配置。
   *
   * @return 自动提交配置；null 表示不干预驱动默认值
   */
  public Boolean isAutoCommit() {
    return autoCommit;
  }

  /**
   * 设置自动提交配置。
   *
   * @param autoCommit 自动提交配置；null 表示不干预驱动默认值
   */
  public void setAutoCommit(Boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  /**
   * 获取默认事务隔离级别配置。
   *
   * @return 事务隔离级别；null 表示不干预驱动默认值
   */
  public Integer getDefaultTransactionIsolationLevel() {
    return defaultTransactionIsolationLevel;
  }

  /**
   * 设置默认事务隔离级别配置。
   *
   * @param defaultTransactionIsolationLevel 事务隔离级别；null 表示不干预驱动默认值
   */
  public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
    this.defaultTransactionIsolationLevel = defaultTransactionIsolationLevel;
  }

  /**
   * 基于用户名/密码组装属性后创建连接。
   *
   * @param username 用户名
   * @param password 密码
   * @return 新创建的 JDBC 连接
   * @throws SQLException 获取连接失败时抛出
   */
  private Connection doGetConnection(String username, String password) throws SQLException {
    Properties props = new Properties();
    if (driverProperties != null) {
      // 先合并用户预设驱动属性，再叠加显式传入账号密码。
      props.putAll(driverProperties);
    }
    if (username != null) {
      props.setProperty("user", username);
    }
    if (password != null) {
      props.setProperty("password", password);
    }
    return doGetConnection(props);
  }

  /**
   * 基于完整属性创建连接并应用连接级配置。
   *
   * @param properties 驱动连接属性
   * @return 新创建的 JDBC 连接
   * @throws SQLException 获取或配置连接失败时抛出
   */
  private Connection doGetConnection(Properties properties) throws SQLException {
    // 确保目标驱动已注册到 DriverManager。
    initializeDriver();
    Connection connection = DriverManager.getConnection(url, properties);
    // 连接建立后再应用自动提交与隔离级别配置。
    configureConnection(connection);
    return connection;
  }

  /**
   * 按需加载并注册 JDBC 驱动。
   * 已注册驱动会直接复用，避免重复注册。
   *
   * @throws SQLException 驱动加载或注册失败时抛出
   */
  private synchronized void initializeDriver() throws SQLException {
    if (!registeredDrivers.containsKey(driver)) {
      Class<?> driverType;
      try {
        if (driverClassLoader != null) {
          // 优先使用外部指定的类加载器，支持容器隔离场景。
          driverType = Class.forName(driver, true, driverClassLoader);
        } else {
          driverType = Resources.classForName(driver);
        }
        // DriverManager requires the driver to be loaded via the system ClassLoader.
        // http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
        Driver driverInstance = (Driver)driverType.newInstance();
        // 通过代理包装驱动，规避部分类加载器场景下 DriverManager 可见性问题。
        DriverManager.registerDriver(new DriverProxy(driverInstance));
        registeredDrivers.put(driver, driverInstance);
      } catch (Exception e) {
        throw new SQLException("Error setting driver on UnpooledDataSource. Cause: " + e);
      }
    }
  }

  /**
   * 对新建连接应用可选配置。
   *
   * @param conn 目标连接
   * @throws SQLException 配置连接失败时抛出
   */
  private void configureConnection(Connection conn) throws SQLException {
    // 仅在目标值与当前值不一致时更新，减少不必要的驱动调用。
    if (autoCommit != null && autoCommit != conn.getAutoCommit()) {
      conn.setAutoCommit(autoCommit);
    }
    if (defaultTransactionIsolationLevel != null) {
      conn.setTransactionIsolation(defaultTransactionIsolationLevel);
    }
  }

  /**
   * JDBC 驱动代理，透传标准 Driver 行为。
   * 该代理用于被 DriverManager 注册，以适配不同类加载器环境。
   */
  private static class DriverProxy implements Driver {
    /**
     * 被代理的真实 JDBC 驱动实例。
     */
    private Driver driver;

    /**
     * 创建驱动代理。
     *
     * @param d 真实驱动实例
     */
    DriverProxy(Driver d) {
      this.driver = d;
    }

    /**
     * 判断当前驱动是否接受给定 URL。
     *
     * @param u JDBC URL
     * @return true 表示可处理
     * @throws SQLException 驱动判断失败时抛出
     */
    @Override
    public boolean acceptsURL(String u) throws SQLException {
      return this.driver.acceptsURL(u);
    }

    /**
     * 通过真实驱动建立连接。
     *
     * @param u JDBC URL
     * @param p 驱动属性
     * @return 新建连接
     * @throws SQLException 连接失败时抛出
     */
    @Override
    public Connection connect(String u, Properties p) throws SQLException {
      return this.driver.connect(u, p);
    }

    /**
     * 获取驱动主版本号。
     *
     * @return 主版本号
     */
    @Override
    public int getMajorVersion() {
      return this.driver.getMajorVersion();
    }

    /**
     * 获取驱动次版本号。
     *
     * @return 次版本号
     */
    @Override
    public int getMinorVersion() {
      return this.driver.getMinorVersion();
    }

    /**
     * 获取驱动属性元信息。
     *
     * @param u JDBC URL
     * @param p 驱动属性
     * @return 驱动属性元信息数组
     * @throws SQLException 获取失败时抛出
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
      return this.driver.getPropertyInfo(u, p);
    }

    /**
     * 判断驱动是否符合 JDBC 规范。
     *
     * @return true 表示符合
     */
    @Override
    public boolean jdbcCompliant() {
      return this.driver.jdbcCompliant();
    }

    /**
     * 返回父级日志记录器（JDK 7+ Driver 接口方法）。
     *
     * @return 全局日志记录器
     */
    // @Override only valid jdk7+
    public Logger getParentLogger() {
      return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }
  }

  /**
   * 当前数据源不支持 unwrap 操作。
   *
   * @param iface 目标接口
   * @param <T> 接口类型
   * @return 无返回，始终抛异常
   * @throws SQLException 始终抛出
   */
  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException(getClass().getName() + " is not a wrapper.");
  }

  /**
   * 当前数据源不包装其他对象。
   *
   * @param iface 目标接口
   * @return 始终为 false
   * @throws SQLException 按接口声明保留
   */
  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  /**
   * 返回父级日志记录器（JDK 7+ DataSource 接口方法）。
   *
   * @return 全局日志记录器
   */
  // @Override only valid jdk7+
  public Logger getParentLogger() {
    // requires JDK version 1.6
    return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  }

}
