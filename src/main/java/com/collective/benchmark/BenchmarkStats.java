package com.collective.benchmark;


import java.util.List;

public class BenchmarkStats {

    public final long requests;
    public final long failedRequests;
    public final List<Long> allTimes;
    public final long duration;

    public BenchmarkStats(long requests, long failedRequests, List<Long> allTimes, long duration) {
        this.requests = requests;
        this.failedRequests = failedRequests;
        this.allTimes = allTimes;
        this.duration = duration;
    }
}
