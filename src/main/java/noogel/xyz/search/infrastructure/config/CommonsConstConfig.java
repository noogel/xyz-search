package noogel.xyz.search.infrastructure.config;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class CommonsConstConfig {
    public static final long SLEEP_SEC_MS = 60_000L;
    public static final Set<String> SUPPORT_VIEW_EXT = Set.of("xhtml", "html", "txt", "pdf", "epub", "mp4");
    public static final ExecutorService SHORT_EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    public static final ExecutorService MULTI_EXECUTOR_SERVICE = Executors.newWorkStealingPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() * 2)
    );
    public static final ExecutorService TICK_SCAN_EXECUTOR_SERVICE = Executors.newWorkStealingPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() * 2)
    );
    public static final ScheduledExecutorService DELAY_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);

}
