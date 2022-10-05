package noogel.xyz.search.infrastructure.config;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommonsConstConfig {
    public static final Set<String> SUPPORT_VIEW_EXT = Set.of("xhtml", "html", "txt");
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newWorkStealingPool();

}
