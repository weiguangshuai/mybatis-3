/**
 *    Copyright 2009-2016 the original author or authors.
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
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * 类型参数解析器，用于将泛型类型变量解析为具体的运行时类型。
 *
 * @author Iwao AVE!
 */
public class TypeParameterResolver {

  /**
   * 解析字段的泛型类型，将类型变量替换为实际类型。
   *
   * @param field   目标字段
   * @param srcType 源码类型，用于查找泛型参数映射
   * @return 解析后的类型，若有泛型参数则返回实际运行时类型
   */
  public static Type resolveFieldType(Field field, Type srcType) {
    Type fieldType = field.getGenericType();
    Class<?> declaringClass = field.getDeclaringClass();
    return resolveType(fieldType, srcType, declaringClass);
  }

  /**
   * 解析方法的泛型返回类型，将类型变量替换为实际类型。
   *
   * @param method  目标方法
   * @param srcType 源码类型，用于查找泛型参数映射
   * @return 解析后的返回类型
   */
  public static Type resolveReturnType(Method method, Type srcType) {
    Type returnType = method.getGenericReturnType();
    Class<?> declaringClass = method.getDeclaringClass();
    return resolveType(returnType, srcType, declaringClass);
  }

  /**
   * 解析方法的泛型参数类型数组，将类型变量替换为实际类型。
   *
   * @param method  目标方法
   * @param srcType 源码类型，用于查找泛型参数映射
   * @return 解析后的参数类型数组
   */
  public static Type[] resolveParamTypes(Method method, Type srcType) {
    Type[] paramTypes = method.getGenericParameterTypes();
    Class<?> declaringClass = method.getDeclaringClass();
    Type[] result = new Type[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      result[i] = resolveType(paramTypes[i], srcType, declaringClass);
    }
    return result;
  }

  /**
   * 根据类型种类选择对应的解析策略。
   *
   * @param type           待解析的类型
   * @param srcType        源码类型，用于查找泛型参数映射
   * @param declaringClass 声明该类型的类
   * @return 解析后的类型
   */
  private static Type resolveType(Type type, Type srcType, Class<?> declaringClass) {
    if (type instanceof TypeVariable) {
      // 类型变量（如 T）需要查找具体的类型绑定
      return resolveTypeVar((TypeVariable<?>) type, srcType, declaringClass);
    } else if (type instanceof ParameterizedType) {
      // 参数化类型（如 List<String>）需要递归解析其类型参数
      return resolveParameterizedType((ParameterizedType) type, srcType, declaringClass);
    } else if (type instanceof GenericArrayType) {
      // 泛型数组（如 T[]）需要解析其组件类型
      return resolveGenericArrayType((GenericArrayType) type, srcType, declaringClass);
    } else {
      // 普通类型直接返回
      return type;
    }
  }

  /**
   * 解析泛型数组类型（如 T[]），将其组件类型解析为具体类型。
   *
   * @param genericArrayType 泛型数组类型
   * @param srcType          源码类型
   * @param declaringClass   声明该类型的类
   * @return 解析后的数组类型
   */
  private static Type resolveGenericArrayType(GenericArrayType genericArrayType, Type srcType, Class<?> declaringClass) {
    // 获取数组的组件类型（如 T[] 中的 T）
    Type componentType = genericArrayType.getGenericComponentType();
    Type resolvedComponentType = null;
    if (componentType instanceof TypeVariable) {
      resolvedComponentType = resolveTypeVar((TypeVariable<?>) componentType, srcType, declaringClass);
    } else if (componentType instanceof GenericArrayType) {
      // 多维数组递归解析
      resolvedComponentType = resolveGenericArrayType((GenericArrayType) componentType, srcType, declaringClass);
    } else if (componentType instanceof ParameterizedType) {
      resolvedComponentType = resolveParameterizedType((ParameterizedType) componentType, srcType, declaringClass);
    }
    // 如果解析后是普通类，返回其数组类型；否则返回泛型数组类型实现
    if (resolvedComponentType instanceof Class) {
      return Array.newInstance((Class<?>) resolvedComponentType, 0).getClass();
    } else {
      return new GenericArrayTypeImpl(resolvedComponentType);
    }
  }

  /**
   * 解析参数化类型（如 List&lt;T&gt;、Map&lt;K, V&gt;），递归解析其类型参数。
   *
   * @param parameterizedType 参数化类型
   * @param srcType           源码类型
   * @param declaringClass    声明该类型的类
   * @return 解析后的参数化类型
   */
  private static ParameterizedType resolveParameterizedType(ParameterizedType parameterizedType, Type srcType, Class<?> declaringClass) {
    // 获取原始类型（如 List）
    Class<?> rawType = (Class<?>) parameterizedType.getRawType();
    // 获取类型参数（如 List<T> 中的 T）
    Type[] typeArgs = parameterizedType.getActualTypeArguments();
    Type[] args = new Type[typeArgs.length];
    for (int i = 0; i < typeArgs.length; i++) {
      if (typeArgs[i] instanceof TypeVariable) {
        // 类型变量需要解析为具体类型
        args[i] = resolveTypeVar((TypeVariable<?>) typeArgs[i], srcType, declaringClass);
      } else if (typeArgs[i] instanceof ParameterizedType) {
        // 嵌套的参数化类型递归解析
        args[i] = resolveParameterizedType((ParameterizedType) typeArgs[i], srcType, declaringClass);
      } else if (typeArgs[i] instanceof WildcardType) {
        // 通配符类型
        args[i] = resolveWildcardType((WildcardType) typeArgs[i], srcType, declaringClass);
      } else {
        // 普通类型直接使用
        args[i] = typeArgs[i];
      }
    }
    return new ParameterizedTypeImpl(rawType, null, args);
  }

  /**
   * 解析通配符类型（如 ? extends T、? super E）。
   *
   * @param wildcardType  通配符类型
   * @param srcType       源码类型
   * @param declaringClass 声明该类型的类
   * @return 解析后的通配符类型
   */
  private static Type resolveWildcardType(WildcardType wildcardType, Type srcType, Class<?> declaringClass) {
    // 解析上界（如 ? extends Number 中的 Number）
    Type[] lowerBounds = resolveWildcardTypeBounds(wildcardType.getLowerBounds(), srcType, declaringClass);
    // 解析下界（如 ? super Integer 中的 Integer）
    Type[] upperBounds = resolveWildcardTypeBounds(wildcardType.getUpperBounds(), srcType, declaringClass);
    return new WildcardTypeImpl(lowerBounds, upperBounds);
  }

  /**
   * 解析通配符类型的边界数组（上下界）。
   *
   * @param bounds         边界类型数组
   * @param srcType        源码类型
   * @param declaringClass 声明该类型的类
   * @return 解析后的边界数组
   */
  private static Type[] resolveWildcardTypeBounds(Type[] bounds, Type srcType, Class<?> declaringClass) {
    Type[] result = new Type[bounds.length];
    for (int i = 0; i < bounds.length; i++) {
      if (bounds[i] instanceof TypeVariable) {
        result[i] = resolveTypeVar((TypeVariable<?>) bounds[i], srcType, declaringClass);
      } else if (bounds[i] instanceof ParameterizedType) {
        result[i] = resolveParameterizedType((ParameterizedType) bounds[i], srcType, declaringClass);
      } else if (bounds[i] instanceof WildcardType) {
        // 嵌套通配符递归解析
        result[i] = resolveWildcardType((WildcardType) bounds[i], srcType, declaringClass);
      } else {
        result[i] = bounds[i];
      }
    }
    return result;
  }

  /**
   * 解析类型变量（如 T、E、K 等），在类的继承层次中查找其具体类型绑定。
   *
   * @param typeVar       类型变量
   * @param srcType       源码类型，包含泛型参数信息
   * @param declaringClass 声明该类型变量的类
   * @return 解析后的具体类型，若未找到则返回 Object
   */
  private static Type resolveTypeVar(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass) {
    Type result = null;
    Class<?> clazz = null;
    // 从源码类型提取出原始类
    if (srcType instanceof Class) {
      clazz = (Class<?>) srcType;
    } else if (srcType instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) srcType;
      clazz = (Class<?>) parameterizedType.getRawType();
    } else {
      throw new IllegalArgumentException("The 2nd arg must be Class or ParameterizedType, but was: " + srcType.getClass());
    }

    // 若是直接在同一类中解析，使用类型变量的边界作为默认类型
    if (clazz == declaringClass) {
      Type[] bounds = typeVar.getBounds();
      if(bounds.length > 0) {
        return bounds[0];
      }
      return Object.class;
    }

    // 从父类继承层次中查找类型变量的具体绑定
    Type superclass = clazz.getGenericSuperclass();
    result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superclass);
    if (result != null) {
      return result;
    }

    // 从实现的接口中查找类型变量的具体绑定
    Type[] superInterfaces = clazz.getGenericInterfaces();
    for (Type superInterface : superInterfaces) {
      result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superInterface);
      if (result != null) {
        return result;
      }
    }
    // 未找到绑定时返回 Object
    return Object.class;
  }

  /**
   * 在父类型层次中扫描，查找类型变量的具体绑定。
   *
   * @param typeVar        类型变量
   * @param srcType        源码类型
   * @param declaringClass 声明该类型变量的类
   * @param clazz          当前类
   * @param superclass     父类型
   * @return 找到的类型绑定，或 null
   */
  private static Type scanSuperTypes(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass, Class<?> clazz, Type superclass) {
    Type result = null;
    if (superclass instanceof ParameterizedType) {
      // 父类型是参数化类型（如 Base&lt;String&gt;）
      ParameterizedType parentAsType = (ParameterizedType) superclass;
      Class<?> parentAsClass = (Class<?>) parentAsType.getRawType();
      if (declaringClass == parentAsClass) {
        // 找到声明该类型变量的类，获取其类型参数映射
        Type[] typeArgs = parentAsType.getActualTypeArguments();
        TypeVariable<?>[] declaredTypeVars = declaringClass.getTypeParameters();
        for (int i = 0; i < declaredTypeVars.length; i++) {
          if (declaredTypeVars[i] == typeVar) {
            // 找到对应的类型参数
            if (typeArgs[i] instanceof TypeVariable) {
              // 类型参数仍是变量，需要继续向上查找
              TypeVariable<?>[] typeParams = clazz.getTypeParameters();
              for (int j = 0; j < typeParams.length; j++) {
                if (typeParams[j] == typeArgs[i]) {
                  // 从当前类的类型参数获取具体类型
                  if (srcType instanceof ParameterizedType) {
                    result = ((ParameterizedType) srcType).getActualTypeArguments()[j];
                  }
                  break;
                }
              }
            } else {
              // 找到具体类型绑定
              result = typeArgs[i];
            }
          }
        }
      } else if (declaringClass.isAssignableFrom(parentAsClass)) {
        // 继续向上递归查找
        result = resolveTypeVar(typeVar, parentAsType, declaringClass);
      }
    } else if (superclass instanceof Class) {
      // 父类型是普通类（非泛型），继续递归
      if (declaringClass.isAssignableFrom((Class<?>) superclass)) {
        result = resolveTypeVar(typeVar, superclass, declaringClass);
      }
    }
    return result;
  }

  /**
   * 工具类禁止实例化。
   */
  private TypeParameterResolver() {
    super();
  }

  /**
   * 参数化类型的实现类，用于在解析泛型后重新构建参数化类型。
   */
  static class ParameterizedTypeImpl implements ParameterizedType {
    /** 原始类型，如 List */
    private Class<?> rawType;

    /** 所有者类型 */
    private Type ownerType;

    /** 实际类型参数，如 List&lt;String&gt; 中的 String */
    private Type[] actualTypeArguments;

    /**
     * 构造参数化类型实例。
     *
     * @param rawType            原始类型
     * @param ownerType          所有者类型
     * @param actualTypeArguments 实际类型参数数组
     */
    public ParameterizedTypeImpl(Class<?> rawType, Type ownerType, Type[] actualTypeArguments) {
      super();
      this.rawType = rawType;
      this.ownerType = ownerType;
      this.actualTypeArguments = actualTypeArguments;
    }

    @Override
    public Type[] getActualTypeArguments() {
      return actualTypeArguments;
    }

    @Override
    public Type getOwnerType() {
      return ownerType;
    }

    @Override
    public Type getRawType() {
      return rawType;
    }

    @Override
    public String toString() {
      return "ParameterizedTypeImpl [rawType=" + rawType + ", ownerType=" + ownerType + ", actualTypeArguments=" + Arrays.toString(actualTypeArguments) + "]";
    }
  }

  /**
   * 通配符类型的实现类，用于表示 ? extends T 或 ? super E 这样的类型。
   */
  static class WildcardTypeImpl implements WildcardType {
    /** 下界，如 ? super E 中的 E */
    private Type[] lowerBounds;

    /** 上界，如 ? extends T 中的 T */
    private Type[] upperBounds;

    /**
     * 构造通配符类型实例。
     *
     * @param lowerBounds 下界数组
     * @param upperBounds 上界数组
     */
    private WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
      super();
      this.lowerBounds = lowerBounds;
      this.upperBounds = upperBounds;
    }

    @Override
    public Type[] getLowerBounds() {
      return lowerBounds;
    }

    @Override
    public Type[] getUpperBounds() {
      return upperBounds;
    }
  }

  /**
   * 泛型数组类型的实现类，用于表示 T[] 这样的泛型数组类型。
   */
  static class GenericArrayTypeImpl implements GenericArrayType {
    /** 泛型组件类型，如 T[] 中的 T */
    private Type genericComponentType;

    /**
     * 构造泛型数组类型实例。
     *
     * @param genericComponentType 泛型组件类型
     */
    private GenericArrayTypeImpl(Type genericComponentType) {
      super();
      this.genericComponentType = genericComponentType;
    }

    @Override
    public Type getGenericComponentType() {
      return genericComponentType;
    }
  }
}
