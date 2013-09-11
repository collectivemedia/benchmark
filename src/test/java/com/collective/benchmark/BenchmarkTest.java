package com.collective.benchmark;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BenchmarkTest {

    @Test
    public void canRunBenchmarkWithContentProviderOnly() throws Exception {
        RequestProvider requestProvider = mock(RequestProvider.class);
        Benchmark benchmark = new Benchmark(requestProvider, 1, 100);
        Future<BenchmarkStats> run = benchmark.run();
        BenchmarkStats stats = run.get();
        assertThat(stats.failedRequests).isEqualTo(0);
        assertThat(stats.requests).isEqualTo(0);
        assertThat(stats.allTimes).hasSize(0);
    }

    @Test
    public void futureWontReturnAsLongAsProviderCanProvide() throws Exception {
        RequestProvider requestProvider = mock(RequestProvider.class);
        when(requestProvider.canProvide()).thenReturn(true);

        // do some real requests
        when(requestProvider.getRequest(any(HttpClient.class))).thenAnswer(new Answer<Request>() {
            @Override
            public Request answer(InvocationOnMock invocation) throws Throwable {
                HttpClient client = (HttpClient) invocation.getArguments()[0];
                return client.POST("http://localhost:8080");
            }
        });

        Benchmark benchmark = new Benchmark(requestProvider, 1, 100);
        Future<BenchmarkStats> run = benchmark.run();
        try {
            run.get(100, TimeUnit.MILLISECONDS);
            fail("should not finish");
        } catch (TimeoutException expected) {
        }

        when(requestProvider.canProvide()).thenReturn(false);
        BenchmarkStats benchmarkStats = run.get();
        assertThat(benchmarkStats.requests).isGreaterThan(0);
    }

    @Test
    public void canRunBenchmarkWithConsoleProvider() throws Exception {
        ConsoleProvider consoleProvider = new ConsoleProvider() {
            @Override
            public String realTime() {
                return "i will be appended to the real time console";
            }
        };
        RequestProvider requestProvider = mock(RequestProvider.class);
        Benchmark benchmark = new Benchmark(requestProvider, 1, 100, consoleProvider);
        benchmark.run();
    }

    @Test
    public void canRunBenchmarkWithContentListener() throws Exception {
        Response.ContentListener contentListener = new Response.ContentListener() {
            @Override
            public void onContent(Response response, ByteBuffer content) {
                // do some stuff with the returned content
            }
        };
        RequestProvider requestProvider = mock(RequestProvider.class);
        Benchmark benchmark = new Benchmark(requestProvider, 1, 100, contentListener);
        benchmark.run();
    }
}
