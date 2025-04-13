package noogel.xyz.search.infrastructure.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class AbstractJobProcessor implements JobProcessor {
    private static final int PROCESS = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    public static final ExecutorService EXECUTORS = new ThreadPoolExecutor(PROCESS, PROCESS, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(100), new ThreadPoolExecutor.CallerRunsPolicy());

    public ExecutorService executorService() {
        return EXECUTORS;
    }
}
