package com.collective.benchmark;

import java.util.TimerTask;

public class StatisticsConsoleUpdater extends TimerTask {

    private final RealTimeStats realTimeStats;
    private final ConsoleProvider consoleProvider;
    private final StringBuilder stringBuilder = new StringBuilder(200);

    private long count;

    public StatisticsConsoleUpdater(RealTimeStats realTimeStats, ConsoleProvider consoleProvider) {
        this.realTimeStats = realTimeStats;
        this.consoleProvider = consoleProvider;
        count = realTimeStats.requests.get();
    }

    public void run() {
        long currentCount = realTimeStats.requests.get();
        stringBuilder.append("\r");
        long lastSecondRequests = currentCount - count;
        stringBuilder.append(lastSecondRequests).append(" rps (");
        stringBuilder.append(currentCount).append(" requests ");
        stringBuilder.append(realTimeStats.failedRequests.get()).append(" failed)");

        if (lastSecondRequests != 0) {
            stringBuilder.append(" - ");
            stringBuilder.append(realTimeStats.timeSum.get() / lastSecondRequests).append(" ms ~ time (");
            stringBuilder.append(realTimeStats.minTime.get()).append(" ms min ");
            stringBuilder.append(realTimeStats.maxTime.get()).append(" ms max)");
        }

        stringBuilder.append(" - ").append(consoleProvider.realTime());

        System.out.print(stringBuilder.toString());
        stringBuilder.delete(0, stringBuilder.length());
        count = currentCount;

        // ugly
        realTimeStats.minTime.set(Long.MAX_VALUE);
        realTimeStats.maxTime.set(0);
        realTimeStats.timeSum.set(0);
    }
}
