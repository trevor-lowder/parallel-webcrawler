package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object delegate;
  private final ProfilingState state;

  ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.delegate = Objects.requireNonNull(delegate);
    this.state = Objects.requireNonNull(state);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Instant start = null;
    boolean isProfiled = method.isAnnotationPresent(Profiled.class);

    if (isProfiled) {
      start = clock.instant();
    }

    try {
      return method.invoke(delegate, args);
    } catch (IllegalAccessException | InvocationTargetException t) {
      if (t.getCause() != null) {
        throw t.getCause();
      }
      throw t;
    } finally {
      if (isProfiled) {
        Instant end = clock.instant();
        state.record(delegate.getClass(), method, Duration.between(start, end));
      }
    }
  }

  @Override 
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null || getClass() != obj.getClass()) return false;
    ProfilingMethodInterceptor that = (ProfilingMethodInterceptor) obj;
    return delegate.equals(that.delegate);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }
}
