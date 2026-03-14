/**
 *    Copyright 2009-2026 the original author or authors.
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
package org.apache.ibatis.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;
import org.apache.ibatis.reflection.property.PropertyNamer;

/**
 * Reflector 会缓存一个类的反射元信息，
 * 以便在属性名与 getter/setter（或字段）之间快速映射。
 *
 * @author Clinton Begin
 */
public class Reflector {

  /** 当前被分析并缓存元信息的目标类型。 */
  private final Class<?> type;
  /** 可读属性名（存在 getter 或可直接读取字段）。 */
  private final String[] readablePropertyNames;
  /** 可写属性名（存在 setter 或可直接写入字段）。 */
  private final String[] writeablePropertyNames;
  /** 属性名 -> 写调用器（setter 方法或字段写入）。 */
  private final Map<String, Invoker> setMethods = new HashMap<String, Invoker>();
  /** 属性名 -> 读调用器（getter 方法或字段读取）。 */
  private final Map<String, Invoker> getMethods = new HashMap<String, Invoker>();
  /** 属性名 -> setter 参数类型。 */
  private final Map<String, Class<?>> setTypes = new HashMap<String, Class<?>>();
  /** 属性名 -> getter 返回类型。 */
  private final Map<String, Class<?>> getTypes = new HashMap<String, Class<?>>();
  /** 默认无参构造器。 */
  private Constructor<?> defaultConstructor;

  /** 大小写不敏感的属性名映射，key 统一为大写。 */
  private Map<String, String> caseInsensitivePropertyMap = new HashMap<String, String>();

  public Reflector(Class<?> clazz) {
    type = clazz;
    // 初始化构造器、方法、字段相关元信息缓存。
    addDefaultConstructor(clazz);
    addGetMethods(clazz);
    addSetMethods(clazz);
    addFields(clazz);
    readablePropertyNames = getMethods.keySet().toArray(new String[getMethods.keySet().size()]);
    writeablePropertyNames = setMethods.keySet().toArray(new String[setMethods.keySet().size()]);
    for (String propName : readablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
    for (String propName : writeablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
  }

  private void addDefaultConstructor(Class<?> clazz) {
    Constructor<?>[] consts = clazz.getDeclaredConstructors();
    for (Constructor<?> constructor : consts) {
      if (constructor.getParameterTypes().length == 0) {
        if (canAccessPrivateMethods()) {
          try {
            constructor.setAccessible(true);
          } catch (Exception e) {
            // 忽略：这里只是兜底尝试提升可访问性，失败时无可恢复操作。
          }
        }
        if (constructor.isAccessible()) {
          this.defaultConstructor = constructor;
        }
      }
    }
  }

  private void addGetMethods(Class<?> cls) {
    // 同名属性可能对应多个候选 getter，后续统一做冲突决议。
    Map<String, List<Method>> conflictingGetters = new HashMap<String, List<Method>>();
    Method[] methods = getClassMethods(cls);
    for (Method method : methods) {
      if (method.getParameterTypes().length > 0) {
        continue;
      }
      String name = method.getName();
      if ((name.startsWith("get") && name.length() > 3)
          || (name.startsWith("is") && name.length() > 2)) {
        name = PropertyNamer.methodToProperty(name);
        addMethodConflict(conflictingGetters, name, method);
      }
    }
    resolveGetterConflicts(conflictingGetters);
  }

  private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
    for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
      Method winner = null;
      String propName = entry.getKey();
      for (Method candidate : entry.getValue()) {
        if (winner == null) {
          winner = candidate;
          continue;
        }
        Class<?> winnerType = winner.getReturnType();
        Class<?> candidateType = candidate.getReturnType();
        if (candidateType.equals(winnerType)) {
          if (!boolean.class.equals(candidateType)) {
            throw new ReflectionException(
                "Illegal overloaded getter method with ambiguous type for property "
                    + propName + " in class " + winner.getDeclaringClass()
                    + ". This breaks the JavaBeans specification and can cause unpredictable results.");
          } else if (candidate.getName().startsWith("is")) {
            // boolean 类型在同等条件下优先 isXxx。
            winner = candidate;
          }
        } else if (candidateType.isAssignableFrom(winnerType)) {
          // 当前 winner 的返回类型是 candidate 的子类型，保留 winner。
        } else if (winnerType.isAssignableFrom(candidateType)) {
          // candidate 返回类型更具体，升级为 winner。
          winner = candidate;
        } else {
          throw new ReflectionException(
              "Illegal overloaded getter method with ambiguous type for property "
                  + propName + " in class " + winner.getDeclaringClass()
                  + ". This breaks the JavaBeans specification and can cause unpredictable results.");
        }
      }
      addGetMethod(propName, winner);
    }
  }

  /**
   * 添加 getter 方法到缓存。
   *
   * @param name 属性名
   * @param method getter 方法
   */
  private void addGetMethod(String name, Method method) {
    if (isValidPropertyName(name)) {
      getMethods.put(name, new MethodInvoker(method));
      Type returnType = TypeParameterResolver.resolveReturnType(method, type);
      getTypes.put(name, typeToClass(returnType));
    }
  }

  private void addSetMethods(Class<?> cls) {
    // 同名属性可能存在多个重载 setter，后续统一做冲突决议。
    Map<String, List<Method>> conflictingSetters = new HashMap<String, List<Method>>();
    Method[] methods = getClassMethods(cls);
    for (Method method : methods) {
      String name = method.getName();
      if (name.startsWith("set") && name.length() > 3) {
        if (method.getParameterTypes().length == 1) {
          name = PropertyNamer.methodToProperty(name);
          addMethodConflict(conflictingSetters, name, method);
        }
      }
    }
    resolveSetterConflicts(conflictingSetters);
  }

  /**
   * 将方法添加到冲突集合中，用于后续冲突决议。
   *
   * @param conflictingMethods 冲突方法集合
   * @param name 属性名
   * @param method 方法
   */
  private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
    List<Method> list = conflictingMethods.get(name);
    if (list == null) {
      list = new ArrayList<Method>();
      conflictingMethods.put(name, list);
    }
    list.add(method);
  }

  private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
    for (String propName : conflictingSetters.keySet()) {
      List<Method> setters = conflictingSetters.get(propName);
      Class<?> getterType = getTypes.get(propName);
      Method match = null;
      ReflectionException exception = null;
      for (Method setter : setters) {
        Class<?> paramType = setter.getParameterTypes()[0];
        if (paramType.equals(getterType)) {
          // 与 getter 类型完全一致时，通常就是最佳匹配。
          match = setter;
          break;
        }
        if (exception == null) {
          try {
            match = pickBetterSetter(match, setter, propName);
          } catch (ReflectionException e) {
            // 暂存异常，继续尝试后续候选，也许仍能找到最佳匹配。
            match = null;
            exception = e;
          }
        }
      }
      if (match == null) {
        throw exception;
      } else {
        addSetMethod(propName, match);
      }
    }
  }

  /**
   * 在两个 setter 方法中选择更合适的。
   *
   * @param setter1 候选 setter
   * @param setter2 候选 setter
   * @param property 属性名
   * @return 更好的 setter
   */
  private Method pickBetterSetter(Method setter1, Method setter2, String property) {
    if (setter1 == null) {
      return setter2;
    }
    Class<?> paramType1 = setter1.getParameterTypes()[0];
    Class<?> paramType2 = setter2.getParameterTypes()[0];
    if (paramType1.isAssignableFrom(paramType2)) {
      return setter2;
    } else if (paramType2.isAssignableFrom(paramType1)) {
      return setter1;
    }
    throw new ReflectionException("Ambiguous setters defined for property '" + property + "' in class '"
        + setter2.getDeclaringClass() + "' with types '" + paramType1.getName() + "' and '"
        + paramType2.getName() + "'.");
  }

  /**
   * 添加 setter 方法到缓存。
   *
   * @param name 属性名
   * @param method setter 方法
   */
  private void addSetMethod(String name, Method method) {
    if (isValidPropertyName(name)) {
      setMethods.put(name, new MethodInvoker(method));
      Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
      setTypes.put(name, typeToClass(paramTypes[0]));
    }
  }

  /**
   * 将泛型 Type 转换为具体 Class。
   *
   * @param src 泛型类型
   * @return 具体 Class
   */
  private Class<?> typeToClass(Type src) {
    // 将 Type 统一归一为 Class，便于后续缓存和快速查询。
    Class<?> result = null;
    if (src instanceof Class) {
      result = (Class<?>) src;
    } else if (src instanceof ParameterizedType) {
      result = (Class<?>) ((ParameterizedType) src).getRawType();
    } else if (src instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) src).getGenericComponentType();
      if (componentType instanceof Class) {
        result = Array.newInstance((Class<?>) componentType, 0).getClass();
      } else {
        Class<?> componentClass = typeToClass(componentType);
        result = Array.newInstance((Class<?>) componentClass, 0).getClass();
      }
    }
    if (result == null) {
      result = Object.class;
    }
    return result;
  }

  private void addFields(Class<?> clazz) {
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      if (canAccessPrivateMethods()) {
        try {
          field.setAccessible(true);
        } catch (Exception e) {
          // 忽略：这里只是兜底尝试提升可访问性，失败时无可恢复操作。
        }
      }
      if (field.isAccessible()) {
        if (!setMethods.containsKey(field.getName())) {
          // issue #379：移除了 final 检查，因为 JDK 1.5 允许通过反射修改 final 字段（JSR-133）。
          // pr #16：但 final static 只能由类加载器设置，这里仍需排除。
          int modifiers = field.getModifiers();
          if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
            addSetField(field);
          }
        }
        if (!getMethods.containsKey(field.getName())) {
          addGetField(field);
        }
      }
    }
    if (clazz.getSuperclass() != null) {
      addFields(clazz.getSuperclass());
    }
  }

  /**
   * 将字段添加为 setter。
   *
   * @param field 字段
   */
  private void addSetField(Field field) {
    if (isValidPropertyName(field.getName())) {
      setMethods.put(field.getName(), new SetFieldInvoker(field));
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      setTypes.put(field.getName(), typeToClass(fieldType));
    }
  }

  /**
   * 将字段添加为 getter。
   *
   * @param field 字段
   */
  private void addGetField(Field field) {
    if (isValidPropertyName(field.getName())) {
      getMethods.put(field.getName(), new GetFieldInvoker(field));
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      getTypes.put(field.getName(), typeToClass(fieldType));
    }
  }

  /**
   * 检查属性名是否合法。
   *
   * @param name 属性名
   * @return 合法返回 true
   */
  private boolean isValidPropertyName(String name) {
    return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
  }

  /**
   * 返回当前类及其父类链上的全部方法（包含私有方法）。
   * 不直接使用 {@link Class#getMethods()}，因为它无法覆盖全部私有声明方法。
   *
   * @param cls 目标类
   * @return 该类可见于继承层级中的方法集合
   */
  private Method[] getClassMethods(Class<?> cls) {
    Map<String, Method> uniqueMethods = new HashMap<String, Method>();
    Class<?> currentClass = cls;
    while (currentClass != null && currentClass != Object.class) {
      addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());

      // 还要补充接口方法：当前类可能是抽象类，接口方法未在声明方法中完全覆盖。
      Class<?>[] interfaces = currentClass.getInterfaces();
      for (Class<?> anInterface : interfaces) {
        addUniqueMethods(uniqueMethods, anInterface.getMethods());
      }

      currentClass = currentClass.getSuperclass();
    }

    Collection<Method> methods = uniqueMethods.values();

    return methods.toArray(new Method[methods.size()]);
  }

  /**
   * 将方法添加到去重集合中，子类方法会覆盖父类方法。
   *
   * @param uniqueMethods 去重方法集合
   * @param methods 待添加的方法数组
   */
  private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
    for (Method currentMethod : methods) {
      if (!currentMethod.isBridge()) {
        String signature = getSignature(currentMethod);
        // 若签名已存在，说明子类已覆盖父类方法；保留更靠近子类的版本。
        if (!uniqueMethods.containsKey(signature)) {
          if (canAccessPrivateMethods()) {
            try {
              currentMethod.setAccessible(true);
            } catch (Exception e) {
              // 忽略：这里只是兜底尝试提升可访问性，失败时无可恢复操作。
            }
          }

          uniqueMethods.put(signature, currentMethod);
        }
      }
    }
  }

  /**
   * 生成方法的唯一签名，用于去重和冲突检测。
   *
   * @param method 方法
   * @return 签名字符串（格式：返回类型#方法名:参数类型列表）
   */
  private String getSignature(Method method) {
    // 以“返回类型#方法名:参数类型列表”构建签名，区分重载并去重。
    StringBuilder sb = new StringBuilder();
    Class<?> returnType = method.getReturnType();
    if (returnType != null) {
      sb.append(returnType.getName()).append('#');
    }
    sb.append(method.getName());
    Class<?>[] parameters = method.getParameterTypes();
    for (int i = 0; i < parameters.length; i++) {
      if (i == 0) {
        sb.append(':');
      } else {
        sb.append(',');
      }
      sb.append(parameters[i].getName());
    }
    return sb.toString();
  }

  /**
   * 检测当前环境是否可以访问私有方法。
   *
   * @return 可以访问返回 true
   */
  private static boolean canAccessPrivateMethods() {
    // 在存在安全管理器时，先检测 suppressAccessChecks 权限。
    try {
      SecurityManager securityManager = System.getSecurityManager();
      if (null != securityManager) {
        securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
      }
    } catch (SecurityException e) {
      return false;
    }
    return true;
  }

  /**
   * 获取当前 Reflector 所服务的目标类型。
   *
   * @return 目标类型
   */
  public Class<?> getType() {
    return type;
  }

  /**
   * 获取默认无参构造器。
   *
   * @return 默认构造器
   */
  public Constructor<?> getDefaultConstructor() {
    if (defaultConstructor != null) {
      return defaultConstructor;
    } else {
      throw new ReflectionException("There is no default constructor for " + type);
    }
  }

  /**
   * 判断是否存在默认无参构造器。
   *
   * @return 存在返回 true
   */
  public boolean hasDefaultConstructor() {
    return defaultConstructor != null;
  }

  /**
   * 获取属性 setter 的调用器。
   *
   * @param propertyName 属性名
   * @return setter 调用器
   */
  public Invoker getSetInvoker(String propertyName) {
    Invoker method = setMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
    }
    return method;
  }

  /**
   * 获取属性 getter 的调用器。
   *
   * @param propertyName 属性名
   * @return getter 调用器
   */
  public Invoker getGetInvoker(String propertyName) {
    Invoker method = getMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
    }
    return method;
  }

  /**
   * 获取属性 setter 对应的参数类型。
   *
   * @param propertyName 属性名
   * @return setter 参数类型
   */
  public Class<?> getSetterType(String propertyName) {
    Class<?> clazz = setTypes.get(propertyName);
    if (clazz == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
    }
    return clazz;
  }

  /**
   * 获取属性 getter 对应的返回类型。
   *
   * @param propertyName 属性名
   * @return getter 返回类型
   */
  public Class<?> getGetterType(String propertyName) {
    Class<?> clazz = getTypes.get(propertyName);
    if (clazz == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
    }
    return clazz;
  }

  /**
   * 获取全部可读属性名。
   *
   * @return 可读属性名数组
   */
  public String[] getGetablePropertyNames() {
    return readablePropertyNames;
  }

  /**
   * 获取全部可写属性名。
   *
   * @return 可写属性名数组
   */
  public String[] getSetablePropertyNames() {
    return writeablePropertyNames;
  }

  /**
   * 判断是否存在指定名称的可写属性。
   *
   * @param propertyName 属性名
   * @return 存在则为 true
   */
  public boolean hasSetter(String propertyName) {
    return setMethods.keySet().contains(propertyName);
  }

  /**
   * 判断是否存在指定名称的可读属性。
   *
   * @param propertyName 属性名
   * @return 存在则为 true
   */
  public boolean hasGetter(String propertyName) {
    return getMethods.keySet().contains(propertyName);
  }

  /**
   * 根据名称查找属性名（不区分大小写）。
   *
   * @param name 属性名（任意大小写）
   * @return 实际属性名，未找到返回 null
   */
  public String findPropertyName(String name) {
    // 按不区分大小写规则查找原始属性名。
    return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
  }
}
