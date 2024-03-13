package noogel.xyz.search.infrastructure.config;

import java.util.Set;
import java.util.concurrent.*;

public class CommonsConsts {
    public static final String FILE_SUFFIX = ".tmp.xyz.search";
    public static final long SLEEP_SEC_MS = 60_000L;
    public static final int CORE_COUNT = Math.min(1, Runtime.getRuntime().availableProcessors());
    public static final Set<String> SUPPORT_VIEW_EXT = Set.of("xhtml", "html", "txt", "pdf", "epub", "mp4");
    public static final ExecutorService SHORT_EXECUTOR_SERVICE = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1000), new ThreadPoolExecutor.CallerRunsPolicy());
    public static final ExecutorService SYNC_EXECUTOR_SERVICE = new ThreadPoolExecutor(CORE_COUNT, CORE_COUNT,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1000), new ThreadPoolExecutor.CallerRunsPolicy());

    public static final ScheduledExecutorService DELAY_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);

}
