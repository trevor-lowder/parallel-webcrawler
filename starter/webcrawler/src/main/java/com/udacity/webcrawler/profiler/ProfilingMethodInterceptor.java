package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final ZonedDateTime zonedDateTime;
  private final Object delegate;
  private final ProfilingState profilingState;

  public ProfilingMethodInterceptor(Clock clock, ZonedDateTime zonedDateTime, Object delegate, ProfilingState profilingState) {
    this.clock = Objects.requireNonNull(clock);
    this.zonedDateTime = zonedDateTime;
    this.delegate = delegate;
    this.profilingState = profilingState;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    Instant currentInstant = null;
    boolean isProfiled = method.isAnnotationPresent(Profiled.class);
    if (isProfiled) {
      currentInstant = clock.instant();
    }

    try {
      return method.invoke(delegate, args);
      } catch (InvocationTargetException s) {
        throw s.getTargetException();
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } finally {
        if (isProfiled) {
          Duration howLong = Duration.between(currentInstant, clock.instant());
          profilingState.record(delegate.getClass(), method, howLong);
        }
    }
  }
}
