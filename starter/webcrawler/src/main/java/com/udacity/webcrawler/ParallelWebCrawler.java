package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;
import com.udacity.webcrawler.profiler.Profiled;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final PageParserFactory parserFactory;
  private final Duration timeout;
  private final int popularWordCount;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;
  private final ForkJoinPool pool;

  @Inject
  ParallelWebCrawler(
      Clock clock,
      PageParserFactory parserFactory,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @MaxDepth int maxDepth,
      @IgnoredUrls List<Pattern> ignoredUrls,
      @TargetParallelism int threadCount) {
    this.clock = clock;
    this.parserFactory = parserFactory;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
  }

  @Override
  @Profiled
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    Map<String, Integer> counts = new ConcurrentHashMap<>();
    Set<String> visitedUrls = new ConcurrentSkipListSet<>();

    // Invoke the initial set of tasks
    pool.invoke(new CrawlTask(startingUrls, deadline, maxDepth, counts, visitedUrls));

    if (counts.isEmpty()) {
      return new CrawlResult.Builder()
          .setWordCounts(counts)
          .setUrlsVisited(visitedUrls.size())
          .build();
    }

    return new CrawlResult.Builder()
        .setWordCounts(WordCounts.sort(counts, popularWordCount))
        .setUrlsVisited(visitedUrls.size())
        .build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }

  private class CrawlTask extends RecursiveAction {
    private final List<String> urls;
    private final Instant deadline;
    private final int depth;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;

    CrawlTask(List<String> urls, Instant deadline, int depth, Map<String, Integer> counts, Set<String> visitedUrls) {
      this.urls = urls;
      this.deadline = deadline;
      this.depth = depth;
      this.counts = counts;
      this.visitedUrls = visitedUrls;
    }

    @Override
    @Profiled
    protected void compute() {
        if (clock.instant().isAfter(deadline) || depth == 0) {
            return;
        }
    
        List<CrawlTask> subtasks = urls.stream()
            .filter(url -> ignoredUrls.stream().noneMatch(pattern -> pattern.matcher(url).matches()))
            .filter(url -> visitedUrls.add(url))
            .map(url -> {
                PageParser.Result result = parserFactory.get(url).parse();
                result.getWordCounts().forEach((word, count) -> counts.merge(word, count, Integer::sum));
                return new CrawlTask(result.getLinks(), deadline, depth - 1, counts, visitedUrls);
            })
            .collect(Collectors.toList());
    
        invokeAll(subtasks);
    }
    
  }
}
