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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.apache.ibatis.reflection.invoker.Invoker;
import org.junit.Test;

/**
 * Micro benchmark style test for comparing reflective access cost.
 */
public class ReflectorVsJdkReflectionPerformanceTest {

  private static final int WARMUP_LOOPS = 10000;
  private static final int BENCHMARK_LOOPS = 100000;
  private static final Object[] NO_ARGS = new Object[0];

  @Test
  public void shouldCompareReflectorAndJdkReflectionCost() throws Exception {
    // Warm up JIT to make timing less noisy.
    runWithReflector(WARMUP_LOOPS);
    runWithJdkReflectionLookupEachTime(WARMUP_LOOPS);
    runWithJdkReflectionCachedMethods(WARMUP_LOOPS);

    long reflectorStart = System.nanoTime();
    long reflectorChecksum = runWithReflector(BENCHMARK_LOOPS);
    long reflectorNs = System.nanoTime() - reflectorStart;

    long jdkLookupStart = System.nanoTime();
    long jdkLookupChecksum = runWithJdkReflectionLookupEachTime(BENCHMARK_LOOPS);
    long jdkLookupNs = System.nanoTime() - jdkLookupStart;

    long jdkCachedStart = System.nanoTime();
    long jdkCachedChecksum = runWithJdkReflectionCachedMethods(BENCHMARK_LOOPS);
    long jdkCachedNs = System.nanoTime() - jdkCachedStart;

    // Make sure all paths execute equivalent logic.
    assertEquals(reflectorChecksum, jdkLookupChecksum);
    assertEquals(reflectorChecksum, jdkCachedChecksum);
    assertTrue(reflectorNs > 0L && jdkLookupNs > 0L && jdkCachedNs > 0L);

    System.out.println("[benchmark] Reflector cost (ms): " + toMillis(reflectorNs));
    System.out.println("[benchmark] JDK reflection with lookup each loop (ms): " + toMillis(jdkLookupNs));
    System.out.println("[benchmark] JDK reflection with cached methods (ms): " + toMillis(jdkCachedNs));
  }

  private long runWithReflector(int loops) throws Exception {
    Reflector reflector = new DefaultReflectorFactory().findForClass(DemoBean.class);
    Invoker setter = reflector.getSetInvoker("id");
    Invoker getter = reflector.getGetInvoker("id");
    DemoBean bean = new DemoBean();
    Object[] setArgs = new Object[1];
    long checksum = 0L;
    for (int i = 0; i < loops; i++) {
      setArgs[0] = Integer.valueOf(i);
      setter.invoke(bean, setArgs);
      checksum += ((Integer) getter.invoke(bean, NO_ARGS)).intValue();
    }
    return checksum;
  }

  private long runWithJdkReflectionLookupEachTime(int loops) throws Exception {
    DemoBean bean = new DemoBean();
    Object[] setArgs = new Object[1];
    long checksum = 0L;
    for (int i = 0; i < loops; i++) {
      Method setter = DemoBean.class.getMethod("setId", int.class);
      Method getter = DemoBean.class.getMethod("getId");
      setArgs[0] = Integer.valueOf(i);
      setter.invoke(bean, setArgs);
      checksum += ((Integer) getter.invoke(bean, NO_ARGS)).intValue();
    }
    return checksum;
  }

  private long runWithJdkReflectionCachedMethods(int loops) throws Exception {
    DemoBean bean = new DemoBean();
    Method setter = DemoBean.class.getMethod("setId", int.class);
    Method getter = DemoBean.class.getMethod("getId");
    setter.setAccessible(true);
    getter.setAccessible(true);
    Object[] setArgs = new Object[1];
    long checksum = 0L;
    for (int i = 0; i < loops; i++) {
      setArgs[0] = Integer.valueOf(i);
      setter.invoke(bean, setArgs);
      checksum += ((Integer) getter.invoke(bean, NO_ARGS)).intValue();
    }
    return checksum;
  }

  private long toMillis(long nanos) {
    return nanos / 1000000L;
  }

  static class DemoBean {
    private int id;

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }
  }
}
