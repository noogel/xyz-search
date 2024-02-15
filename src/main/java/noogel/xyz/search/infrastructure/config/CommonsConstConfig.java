package noogel.xyz.search.infrastructure.config;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class CommonsConstConfig {
    public static final Set<String> SUPPORT_VIEW_EXT = Set.of("xhtml", "html", "txt", "pdf", "epub", "mp4");
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    public static final ExecutorService SHORT_EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    public static final ExecutorService MULTI_EXECUTOR_SERVICE = Executors.newWorkStealingPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 4)
    );
    public static final ScheduledExecutorService COMMON_SCHEDULED_SERVICE = Executors.newScheduledThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 4)
    );
    public static final ScheduledExecutorService DELAY_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);

}
