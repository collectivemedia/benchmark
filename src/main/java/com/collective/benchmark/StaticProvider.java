package com.collective.benchmark;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class StaticProvider implements RequestProvider {
    private String uri;
    private final AtomicInteger remainingRequests;

    public StaticProvider(String uri, int totalRequests) {
        this.uri = uri;
        this.remainingRequests = new AtomicInteger(totalRequests);
    }

    @Override
    public Request getRequest(HttpClient client) {
        remainingRequests.decrementAndGet();
        return client.newRequest(uri).method(HttpMethod.GET).timeout(100, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean canProvide() {
        return (remainingRequests.get() > 0);
    }
}
