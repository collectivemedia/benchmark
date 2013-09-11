package com.collective.benchmark;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

class RealTimeStats {
    final AtomicLong requests = new AtomicLong();
    final AtomicLong failedRequests = new AtomicLong();
    final AtomicLong maxTime = new AtomicLong();
    final AtomicLong timeSum = new AtomicLong();
    final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
    final List<Long> allTimes = new ArrayList<>();
}
