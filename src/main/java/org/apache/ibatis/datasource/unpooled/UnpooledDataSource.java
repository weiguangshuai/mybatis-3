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
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.ibatis.io.Resources;

/**
 * 非池化数据源实现，每次请求创建新的数据库连接。
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class UnpooledDataSource implements DataSource {

  /** 用于加载 JDBC 驱动的自定义类加载器 */
  private ClassLoader driverClassLoader;
  /** JDBC 驱动的额外配置属性 */
  private Properties driverProperties;
  /** 已注册的 JDBC 驱动缓存，避免重复加载 */
  private static Map<String, Driver> registeredDrivers = new ConcurrentHashMap<>();

  /** JDBC 驱动类名 */
  private String driver;
  /** 数据库连接 URL */
  private String url;
  /** 数据库用户名 */
  private String username;
  /** 数据库密码 */
  private String password;

  /** 连接是否自动提交 */
  private Boolean autoCommit;
  /** 默认事务隔离级别 */
  private Integer defaultTransactionIsolationLevel;
  /** 默认网络超时时间（毫秒）*/
  private Integer defaultNetworkTimeout;

  static {
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      registeredDrivers.put(driver.getClass().getName(), driver);
    }
  }

  /** 无参构造器 */
  public UnpooledDataSource() {
  }

  /**
   * 使用用户名密码构造非池化数据源。
   *
   * @param driver JDBC 驱动类名
   * @param url 数据库连接 URL
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
   * 使用驱动属性构造非池化数据源。
   *
   * @param driver JDBC 驱动类名
   * @param url 数据库连接 URL
   * @param driverProperties 驱动属性
   */
  public UnpooledDataSource(String driver, String url, Properties driverProperties) {
    this.driver = driver;
    this.url = url;
    this.driverProperties = driverProperties;
  }

  /**
   * 使用自定义类加载器构造非池化数据源。
   *
   * @param driverClassLoader 自定义类加载器
   * @param driver JDBC 驱动类名
   * @param url 数据库连接 URL
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
   * 使用自定义类加载器和驱动属性构造非池化数据源。
   *
   * @param driverClassLoader 自定义类加载器
   * @param driver JDBC 驱动类名
   * @param url 数据库连接 URL
   * @param driverProperties 驱动属性
   */
  public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
    this.driverClassLoader = driverClassLoader;
    this.driver = driver;
    this.url = url;
    this.driverProperties = driverProperties;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return doGetConnection(username, password);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return doGetConnection(username, password);
  }

  @Override
  public void setLoginTimeout(int loginTimeout) {
    DriverManager.setLoginTimeout(loginTimeout);
  }

  @Override
  public int getLoginTimeout() {
    return DriverManager.getLoginTimeout();
  }

  @Override
  public void setLogWriter(PrintWriter logWriter) {
    DriverManager.setLogWriter(logWriter);
  }

  @Override
  public PrintWriter getLogWriter() {
    return DriverManager.getLogWriter();
  }

  /** 获取自定义类加载器 */
  public ClassLoader getDriverClassLoader() {
    return driverClassLoader;
  }

  /** 设置自定义类加载器 */
  public void setDriverClassLoader(ClassLoader driverClassLoader) {
    this.driverClassLoader = driverClassLoader;
  }

  /** 获取驱动属性 */
  public Properties getDriverProperties() {
    return driverProperties;
  }

  /** 设置驱动属性 */
  public void setDriverProperties(Properties driverProperties) {
    this.driverProperties = driverProperties;
  }

  /** 获取 JDBC 驱动类名 */
  public synchronized String getDriver() {
    return driver;
  }

  /** 设置 JDBC 驱动类名 */
  public synchronized void setDriver(String driver) {
    this.driver = driver;
  }

  /** 获取数据库连接 URL */
  public String getUrl() {
    return url;
  }

  /** 设置数据库连接 URL */
  public void setUrl(String url) {
    this.url = url;
  }

  /** 获取用户名 */
  public String getUsername() {
    return username;
  }

  /** 设置用户名 */
  public void setUsername(String username) {
    this.username = username;
  }

  /** 获取密码 */
  public String getPassword() {
    return password;
  }

  /** 设置密码 */
  public void setPassword(String password) {
    this.password = password;
  }

  /** 获取自动提交状态 */
  public Boolean isAutoCommit() {
    return autoCommit;
  }

  /** 设置自动提交状态 */
  public void setAutoCommit(Boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  /** 获取默认事务隔离级别 */
  public Integer getDefaultTransactionIsolationLevel() {
    return defaultTransactionIsolationLevel;
  }

  /** 设置默认事务隔离级别 */
  public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
    this.defaultTransactionIsolationLevel = defaultTransactionIsolationLevel;
  }

  /**
   * Gets the default network timeout.
   *
   * @return the default network timeout
   * @since 3.5.2
   */
  public Integer getDefaultNetworkTimeout() {
    return defaultNetworkTimeout;
  }

  /**
   * Sets the default network timeout value to wait for the database operation to complete. See {@link Connection#setNetworkTimeout(java.util.concurrent.Executor, int)}
   *
   * @param defaultNetworkTimeout
   *          The time in milliseconds to wait for the database operation to complete.
   * @since 3.5.2
   */
  public void setDefaultNetworkTimeout(Integer defaultNetworkTimeout) {
    this.defaultNetworkTimeout = defaultNetworkTimeout;
  }

  /** 使用用户名密码获取数据库连接 */
  private Connection doGetConnection(String username, String password) throws SQLException {
    Properties props = new Properties();
    if (driverProperties != null) {
      // 合并驱动属性
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

  /** 使用属性对象获取数据库连接 */
  private Connection doGetConnection(Properties properties) throws SQLException {
    initializeDriver(); // 确保驱动已注册
    Connection connection = DriverManager.getConnection(url, properties);
    configureConnection(connection); // 配置连接参数
    return connection;
  }

  /** 初始化并注册 JDBC 驱动 */
  private synchronized void initializeDriver() throws SQLException {
    if (!registeredDrivers.containsKey(driver)) {
      Class<?> driverType;
      try {
        // 使用自定义类加载器或系统类加载器加载驱动
        if (driverClassLoader != null) {
          driverType = Class.forName(driver, true, driverClassLoader);
        } else {
          driverType = Resources.classForName(driver);
        }
        // DriverManager requires the driver to be loaded via the system ClassLoader.
        // https://www.kfu.com/~nsayer/Java/dyn-jdbc.html
        Driver driverInstance = (Driver) driverType.getDeclaredConstructor().newInstance();
        DriverManager.registerDriver(new DriverProxy(driverInstance));
        registeredDrivers.put(driver, driverInstance);
      } catch (Exception e) {
        throw new SQLException("Error setting driver on UnpooledDataSource. Cause: " + e);
      }
    }
  }

  /** 配置数据库连接参数 */
  private void configureConnection(Connection conn) throws SQLException {
    if (defaultNetworkTimeout != null) {
      conn.setNetworkTimeout(Executors.newSingleThreadExecutor(), defaultNetworkTimeout);
    }
    // 仅当与当前值不同时才设置，避免不必要的数据库交互
    if (autoCommit != null && autoCommit != conn.getAutoCommit()) {
      conn.setAutoCommit(autoCommit);
    }
    if (defaultTransactionIsolationLevel != null) {
      conn.setTransactionIsolation(defaultTransactionIsolationLevel);
    }
  }

  /** JDBC 驱动代理类，用于包装驱动实例 */
  private static class DriverProxy implements Driver {
    private Driver driver;

    DriverProxy(Driver d) {
      this.driver = d;
    }

    @Override
    public boolean acceptsURL(String u) throws SQLException {
      return this.driver.acceptsURL(u);
    }

    @Override
    public Connection connect(String u, Properties p) throws SQLException {
      return this.driver.connect(u, p);
    }

    @Override
    public int getMajorVersion() {
      return this.driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
      return this.driver.getMinorVersion();
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
      return this.driver.getPropertyInfo(u, p);
    }

    @Override
    public boolean jdbcCompliant() {
      return this.driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() {
      return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException(getClass().getName() + " is not a wrapper.");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  @Override
  public Logger getParentLogger() {
    // requires JDK version 1.6
    return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  }

}
