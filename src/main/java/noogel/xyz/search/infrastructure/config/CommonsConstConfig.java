package noogel.xyz.search.infrastructure.config;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class CommonsConstConfig {
    public static final Set<String> SUPPORT_VIEW_EXT = Set.of("xhtml", "html", "txt", "pdf");
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newWorkStealingPool();
    public static final ScheduledExecutorService DELAY_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors());

}
