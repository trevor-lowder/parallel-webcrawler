package com.udacity.webcrawler.profiler;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import javax.inject.Inject;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    if (!isProfiled(klass)) {
      throw new IllegalArgumentException("Class does not contain any methods annotated with @Profiled");
    }

    return klass.cast(Proxy.newProxyInstance(
        klass.getClassLoader(),
        new Class<?>[]{klass},
        new ProfilingMethodInterceptor(clock, delegate, state)
    ));
  }

  private boolean isProfiled(Class<?> klass) {
    for (Method method : klass.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Profiled.class)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void writeData(Path path) {
    try (Writer writer = Files.newBufferedWriter(
            path,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND)){
      writeData(writer);
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}

