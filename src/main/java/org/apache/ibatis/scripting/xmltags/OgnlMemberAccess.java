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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.util.Map;

import ognl.MemberAccess;

import org.apache.ibatis.reflection.Reflector;

/**
 * OGNL 表达式访问成员时的权限控制类，用于在运行时设置成员的可访问性。
 *
 * <p>基于 <a href=
 * 'https://github.com/jkuhnert/ognl/blob/OGNL_3_2_1/src/java/ognl/DefaultMemberAccess.java'>DefaultMemberAccess</a> 实现。
 *
 * @author Kazuki Shimizu
 * @since 3.5.0
 *
 * @see <a href=
 *      'https://github.com/jkuhnert/ognl/blob/OGNL_3_2_1/src/java/ognl/DefaultMemberAccess.java'>DefaultMemberAccess</a>
 * @see <a href='https://github.com/jkuhnert/ognl/issues/47'>#47 of ognl</a>
 */
class OgnlMemberAccess implements MemberAccess {

  /**
   * 当前环境是否允许控制成员的可访问性
   */
  private final boolean canControlMemberAccessible;

  OgnlMemberAccess() {
    this.canControlMemberAccessible = Reflector.canControlMemberAccessible();
  }

  /**
   * 设置目标成员的可访问性，以便 OGNL 能够访问私有或受保护的成员。
   *
   * @param context OgnlContext
   * @param target 目标对象
   * @param member 要访问的成员
   * @param propertyName 属性名称
   * @return 若修改了可访问性则返回之前的可访问状态，否则返回 null
   */
  @Override
  public Object setup(Map context, Object target, Member member, String propertyName) {
    Object result = null;
    if (isAccessible(context, target, member, propertyName)) {
      AccessibleObject accessible = (AccessibleObject) member;
      if (!accessible.isAccessible()) {
        result = Boolean.FALSE;
        accessible.setAccessible(true);
      }
    }
    return result;
  }

  /**
   * 恢复成员的可访问性状态。由于翻转 accessible 标志不是线程安全的，因此不执行任何操作。
   *
   * @param context OgnlContext
   * @param target 目标对象
   * @param member 要访问的成员
   * @param propertyName 属性名称
   * @param state 之前保存的可访问状态
   */
  @Override
  public void restore(Map context, Object target, Member member, String propertyName,
      Object state) {
    // Flipping accessible flag is not thread safe. See #1648
  }

  /**
   * 判断当前环境是否允许访问指定成员。
   *
   * @param context OgnlContext
   * @param target 目标对象
   * @param member 要访问的成员
   * @param propertyName 属性名称
   * @return 若允许访问则返回 true，否则返回 false
   */
  @Override
  public boolean isAccessible(Map context, Object target, Member member, String propertyName) {
    return canControlMemberAccessible;
  }

}
