package com.collective.benchmark;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BenchmarkTest {


    private static final int PORT = 9999;
    private Server server;
    private AtomicInteger gotReqs;
    private int statusCode;

    @Before
    public void setUp() throws Exception {
        gotReqs = new AtomicInteger();
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(PORT);
        server.setConnectors(new ServerConnector[]{connector});

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                gotReqs.incrementAndGet();
                resp.getWriter().write("YO");
                resp.setStatus(statusCode);
            }
        }), "/valid/*");
        server.setHandler(context);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        server.destroy();
    }

    @Test
    public void canRunBenchmarkWithStaticProvider() throws Exception {
        statusCode = 200;
        RequestProvider requestProvider = new StaticProvider("http://127.0.0.1:"+PORT+"/valid/blah", 10);
        Benchmark benchmark = new Benchmark(requestProvider, 1, 100);
        Future<BenchmarkStats> run = benchmark.run();
        BenchmarkStats stats = run.get();
        assertThat(gotReqs.get()).isEqualTo(10);
        assertThat(stats.failedRequests).isEqualTo(0);
        assertThat(stats.requests).isEqualTo(10);
        assertThat(stats.allTimes).hasSize(10);
    }

    @Test
    public void canCountErrors() throws Exception {
        statusCode = 500;
        RequestProvider requestProvider = new StaticProvider("http://127.0.0.1:"+PORT+"/notFound", 10);
        Benchmark benchmark = new Benchmark(requestProvider, 1, 100);
        Future<BenchmarkStats> run = benchmark.run();
        BenchmarkStats stats = run.get();
        assertThat(stats.failedRequests).isEqualTo(10);
        assertThat(stats.requests).isEqualTo(10);
        assertThat(stats.allTimes).hasSize(10);
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
