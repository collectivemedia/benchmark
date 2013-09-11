package com.collective.benchmark;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;


public class PersistentConnection {

    private final HttpClient httpClient;
    private final RequestProvider requestProvider;
    private final RealTimeStats realTimeStats;
    private final Response.ContentListener contentListener;
    private volatile boolean run = false;

    public PersistentConnection(HttpClient httpClient, RequestProvider requestProvider, RealTimeStats realTimeStats, Response.ContentListener contentListener) {
        this.httpClient = httpClient;
        this.requestProvider = requestProvider;
        this.realTimeStats = realTimeStats;
        this.contentListener = contentListener;
    }

    public void start() {
        run = true;
        send();
    }

    public void stop() {
        run = false;
    }

    private void send() {
        if (!run || !requestProvider.canProvide())
            return;

        Request request = requestProvider.getRequest(httpClient);

        if (request != null) {
            final long time = System.currentTimeMillis();

            if (contentListener != null)
                request.onResponseContent(contentListener);

            request.send(new Response.CompleteListener() {
                @Override
                public void onComplete(Result result) {
                    long duration = System.currentTimeMillis() - time;
                    if (result.isFailed()) {
                        realTimeStats.failedRequests.incrementAndGet();
                    }
                    realTimeStats.allTimes.add(duration);
                    realTimeStats.requests.incrementAndGet();
                    realTimeStats.maxTime.set(Math.max(realTimeStats.maxTime.get(), duration));
                    realTimeStats.minTime.set(Math.min(realTimeStats.minTime.get(), duration));
                    realTimeStats.timeSum.addAndGet(duration);
                    send();
                }
            });
        } else {
            // if null just retry
            send();
        }
    }
}
