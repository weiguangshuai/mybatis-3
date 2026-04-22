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
package org.apache.ibatis.scripting.xmltags;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import ognl.OgnlContext;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

/**
 * DynamicContext - 负责在动态 SQL 解析过程中存储参数绑定、拼接 SQL 片段并生成最终 SQL。
 *
 * @author Clinton Begin
 */
public class DynamicContext {

  /** 参数对象在 DynamicContext 中的键名 */
  public static final String PARAMETER_OBJECT_KEY = "_parameter";
  /** 数据库标识在 DynamicContext 中的键名 */
  public static final String DATABASE_ID_KEY = "_databaseId";

  static {
    OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
  }

  /** 参数绑定 Map */
  private final ContextMap bindings;
  /** SQL 片段拼接器 */
  private final StringJoiner sqlBuilder = new StringJoiner(" ");
  /** 唯一序号生成器，用于生成不重复的占位符前缀 */
  private int uniqueNumber = 0;

  /**
   * 构造 DynamicContext，初始化参数绑定。
   *
   * @param configuration  MyBatis Configuration
   * @param parameterObject 参数对象
   */
  public DynamicContext(Configuration configuration, Object parameterObject) {
    if (parameterObject != null && !(parameterObject instanceof Map)) {
      MetaObject metaObject = configuration.newMetaObject(parameterObject);
      boolean existsTypeHandler = configuration.getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass());
      bindings = new ContextMap(metaObject, existsTypeHandler);
    } else {
      bindings = new ContextMap(null, false);
    }
    bindings.put(PARAMETER_OBJECT_KEY, parameterObject);
    bindings.put(DATABASE_ID_KEY, configuration.getDatabaseId());
  }

  /**
   * 获取参数绑定 Map。
   *
   * @return 参数绑定 Map
   */
  public Map<String, Object> getBindings() {
    return bindings;
  }

  /**
   * 向 Map 中绑定变量。
   *
   * @param name  变量名
   * @param value 变量值
   */
  public void bind(String name, Object value) {
    bindings.put(name, value);
  }

  /**
   * 追加 SQL 片段。
   *
   * @param sql SQL 片段
   */
  public void appendSql(String sql) {
    sqlBuilder.add(sql);
  }

  /**
   * 获取拼接后的完整 SQL，并去除首尾空白。
   *
   * @return 最终 SQL 字符串
   */
  public String getSql() {
    return sqlBuilder.toString().trim();
  }

  /**
   * 获取下一个唯一序号，用于生成不重复的占位符前缀。
   *
   * @return 当前序号，调用后自动递增
   */
  public int getUniqueNumber() {
    return uniqueNumber++;
  }

  /**
   * 参数绑定 Map - 支持从原始参数对象中读取属性作为回退。
   */
  static class ContextMap extends HashMap<String, Object> {
    private static final long serialVersionUID = 2977601501966151582L;
    /** 参数对象的元数据封装 */
    private final MetaObject parameterMetaObject;
    /** 当参数对象没有对应 getter 时，是否直接返回原始参数对象 */
    private final boolean fallbackParameterObject;

    public ContextMap(MetaObject parameterMetaObject, boolean fallbackParameterObject) {
      this.parameterMetaObject = parameterMetaObject;
      this.fallbackParameterObject = fallbackParameterObject;
    }

    @Override
    public Object get(Object key) {
      String strKey = (String) key;
      // 优先从当前绑定 Map 中查找
      if (super.containsKey(strKey)) {
        return super.get(strKey);
      }

      // 没有封装参数对象时直接返回 null
      if (parameterMetaObject == null) {
        return null;
      }

      // 若启用回退且参数对象无对应 getter，则返回原始参数对象本身（用于单值场景）
      if (fallbackParameterObject && !parameterMetaObject.hasGetter(strKey)) {
        return parameterMetaObject.getOriginalObject();
      } else {
        // issue #61 do not modify the context when reading
        return parameterMetaObject.getValue(strKey);
      }
    }
  }

  /**
   * OGNL PropertyAccessor - 使 OGNL 表达式能够从 {@link ContextMap} 中读取参数及绑定变量。
   */
  static class ContextAccessor implements PropertyAccessor {

    @Override
    public Object getProperty(Map context, Object target, Object name) {
      Map map = (Map) target;

      Object result = map.get(name);
      // 若 Map 中已包含该键，或结果非 null，直接返回
      if (map.containsKey(name) || result != null) {
        return result;
      }

      // 否则尝试从原始参数对象中读取（若参数本身是 Map）
      Object parameterObject = map.get(PARAMETER_OBJECT_KEY);
      if (parameterObject instanceof Map) {
        return ((Map)parameterObject).get(name);
      }

      return null;
    }

    @Override
    public void setProperty(Map context, Object target, Object name, Object value) {
      Map<Object, Object> map = (Map<Object, Object>) target;
      map.put(name, value);
    }

    @Override
    public String getSourceAccessor(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }

    @Override
    public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }
  }
}
