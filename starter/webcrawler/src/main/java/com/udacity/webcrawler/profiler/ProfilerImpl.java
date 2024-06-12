package com.udacity.webcrawler.profiler;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import java.util.Arrays;
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

  @SuppressWarnings("unchecked")
  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);
    if (!isClassProfiled(klass)) {
      throw new IllegalArgumentException(klass.getName() + "does not consist of profiled methods.");
    }
    return (T) Proxy.newProxyInstance(
            ProfilerImpl.class.getClassLoader(),
            new Class<?>[]{klass},
            new ProfilingMethodInterceptor(clock, startTime, delegate, state)
    );
  }

  private Boolean isClassProfiled (Class<?> klass) {
    return Arrays.stream(klass.getDeclaredMethods())
            .anyMatch(method -> method.isAnnotationPresent(Profiled.class));
  }

  @Override
  public void writeData(Path path) throws IOException {
    try (Writer writer = Files.newBufferedWriter(
            path,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND)){
      writeData(writer);
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
