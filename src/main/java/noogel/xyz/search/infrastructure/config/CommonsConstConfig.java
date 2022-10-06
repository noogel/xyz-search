package noogel.xyz.search.infrastructure.config;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class CommonsConstConfig {
    public static final Set<String> SUPPORT_VIEW_EXT = Set.of("xhtml", "html", "txt");
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newWorkStealingPool();
    public static final ScheduledExecutorService DELAY_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors());

}
