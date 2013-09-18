package com.collective.benchmark;


import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

class RealTimeStats {
    final AtomicLong requests = new AtomicLong();
    final AtomicLong failedRequests = new AtomicLong();
    final AtomicLong maxTime = new AtomicLong();
    final AtomicLong timeSum = new AtomicLong();
    final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
    final Queue<Long> allTimes = new ConcurrentLinkedQueue<>();
}
