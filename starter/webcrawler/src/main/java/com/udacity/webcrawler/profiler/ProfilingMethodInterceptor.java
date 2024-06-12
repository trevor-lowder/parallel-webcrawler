package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class ProfilingMethodInterceptor implements InvocationHandler {
    private final Clock clock;
    private final Object target;
    private final ProfilingState state;

    ProfilingMethodInterceptor(Clock clock, Object target, ProfilingState state) {
        this.clock = Objects.requireNonNull(clock);
        this.target = Objects.requireNonNull(target);
        this.state = Objects.requireNonNull(state);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Instant start = null;
        boolean profiled = method.isAnnotationPresent(Profiled.class);
    
        if (profiled) {
            start = clock.instant();
            System.out.println("Profiling started for method: " + method.getName());
        }
    
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            if (profiled) {
                Instant end = clock.instant();
                state.record(target.getClass(), method, Duration.between(start, end));
                System.out.println("Profiling ended for method: " + method.getName());
            }
        }
    }
}
