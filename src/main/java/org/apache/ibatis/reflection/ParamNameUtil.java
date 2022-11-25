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
package org.apache.ibatis.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用于获取方法或构造函数参数名称的工具类。
 */
public class ParamNameUtil {
  /**
   * 获取方法的参数名称列表。
   * @param method 要获取参数名称的方法
   * @return 参数名称列表
   */
  public static List<String> getParamNames(Method method) {
    return getParameterNames(method);
  }

  /**
   * 获取构造函数的参数名称列表。
   * @param constructor 要获取参数名称的构造函数
   * @return 参数名称列表
   */
  public static List<String> getParamNames(Constructor<?> constructor) {
    return getParameterNames(constructor);
  }

  /**
   * 获取可执行程序（方法或构造函数）的参数名称列表。
   * @param executable 方法或构造函数
   * @return 参数名称列表
   */
  private static List<String> getParameterNames(Executable executable) {
    // 将参数数组转为流，提取每个参数的名称，最后收集为列表
    return Arrays.stream(executable.getParameters()).map(Parameter::getName).collect(Collectors.toList());
  }

  /** 工具类不允许实例化 */
  private ParamNameUtil() {
    super();
  }
}
