/**
 *    Copyright 2009-2017 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.lang.UsesJava8;

/**
 * 参数名称工具类，用于获取方法或构造函数的参数名称。
 *
 * @author Clinton Begin
 */
@UsesJava8
public class ParamNameUtil {
  /**
   * 获取方法的参数名称列表。
   *
   * @param method 要获取参数名称的方法
   * @return 参数名称列表
   */
  public static List<String> getParamNames(Method method) {
    return getParameterNames(method);
  }

  /**
   * 获取构造函数的参数名称列表。
   *
   * @param constructor 要获取参数名称的构造函数
   * @return 参数名称列表
   */
  public static List<String> getParamNames(Constructor<?> constructor) {
    return getParameterNames(constructor);
  }

  /**
   * 获取可执行对象（方法或构造函数）的参数名称列表。
   *
   * @param executable 方法或构造函数对象
   * @return 参数名称列表
   */
  private static List<String> getParameterNames(Executable executable) {
    final List<String> names = new ArrayList<String>();
    final Parameter[] params = executable.getParameters();
    // 遍历所有参数，提取参数名称
    for (Parameter param : params) {
      names.add(param.getName());
    }
    return names;
  }

  /** 工具类禁止实例化 */
  private ParamNameUtil() {
    super();
  }
}
