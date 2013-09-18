package com.collective.benchmark;

import com.google.common.collect.Lists;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;

import java.util.Timer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Benchmark {

    private final RequestProvider requestProvider;
    private final ConsoleProvider consoleProvider;
    private final int concurrentConnections;
    private final Response.ContentListener contentListener;
    private final int maxConnections;

    public Benchmark(RequestProvider requestProvider, int concurrentConnections, int maxConnections, ConsoleProvider consoleProvider, Response.ContentListener contentListener) {
        this.requestProvider = requestProvider;
        this.consoleProvider = consoleProvider;
        this.concurrentConnections = concurrentConnections;
        this.contentListener = contentListener;
        this.maxConnections = maxConnections;
    }

    public Benchmark(RequestProvider requestProvider, int concurrentConnections, int maxConnections) {
        this(requestProvider, concurrentConnections, maxConnections, null, null);
    }

    public Benchmark(RequestProvider requestProvider, int concurrentConnections, int maxConnections, ConsoleProvider consoleProvider) {
        this(requestProvider, concurrentConnections, maxConnections, consoleProvider, null);
    }

    public Benchmark(RequestProvider requestProvider, int concurrentConnections, int maxConnections, Response.ContentListener contentListener) {
        this(requestProvider, concurrentConnections, maxConnections, null, contentListener);
    }

    public Benchmark(RequestProvider requestProvider, int concurrency) {
        this(requestProvider, concurrency, concurrency*2);
    }

    private void stop(HttpClient client, Timer timer) {
        timer.cancel();
        try {
            client.stop();
        } catch (Exception ignored) {
        }
    }

    public Future<BenchmarkStats> run() throws Exception {
        final HttpClient client = new HttpClient();
        client.setMaxConnectionsPerDestination(maxConnections);
        client.setIdleTimeout(0);
        client.start();

        final Timer timer = new Timer(true);
        final RealTimeStats currentStats = new RealTimeStats();

        final long start = System.currentTimeMillis();

        for (int i = 0; i < concurrentConnections; i++) {
            new PersistentConnection(client, requestProvider, currentStats, contentListener).start();
        }

        if (consoleProvider != null) {
            timer.scheduleAtFixedRate(new StatisticsConsoleUpdater(currentStats, consoleProvider), 0, 1000);
        }

        //noinspection NullableProblems
        return new Future<BenchmarkStats>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return !requestProvider.canProvide();
            }

            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            @Override
            public BenchmarkStats get() throws InterruptedException, ExecutionException {
                try {
                    while (requestProvider.canProvide()) {
                        Thread.sleep(10);
                    }
                    Thread.sleep(10); // this is a hotfix to make sure we do all requests
                    return new BenchmarkStats(currentStats.requests.get(), currentStats.failedRequests.get(), Lists.newArrayList(currentStats.allTimes), System.currentTimeMillis() - start);
                } finally {
                    stop(client, timer);
                }
            }

            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            @Override
            public BenchmarkStats get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                long start = System.currentTimeMillis();
                try {
                    while (requestProvider.canProvide()) {
                        Thread.sleep(10);
                        if (System.currentTimeMillis() - start >= unit.convert(timeout, TimeUnit.MILLISECONDS)) {
                            throw new TimeoutException("benchmark not finished");
                        }
                    }
                    Thread.sleep(10); // this is a hotfix to make sure we do all requests
                    return new BenchmarkStats(currentStats.requests.get(), currentStats.failedRequests.get(), Lists.newArrayList(currentStats.allTimes), System.currentTimeMillis() - start);
                } finally {
                    stop(client, timer);
                }
            }
        };
    }
}
