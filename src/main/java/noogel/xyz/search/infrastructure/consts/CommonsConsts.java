package noogel.xyz.search.infrastructure.consts;

import java.util.Set;
import java.util.concurrent.*;

public class CommonsConsts {
    public static final String FILE_SUFFIX = ".tmp.xyz.search";
    public static final long SLEEP_SEC_MS = 60_000L;
    public static final int DEFAULT_BATCH_COMMIT_LIMIT_MS = 3_000;
    public static final int DEFAULT_SCAN_FILE_LIMIT_MS = DEFAULT_BATCH_COMMIT_LIMIT_MS + 3_000;
    public static final int CORE_COUNT = Math.min(1, Runtime.getRuntime().availableProcessors());
    public static final Set<String> SUPPORT_VIEW_EXT = Set.of("xhtml", "html", "txt", "pdf", "epub",
            "mp4", "avi", "mkv", "jpeg", "jpg", "png", "webp", "bmp", "tiff");
    // todo "heif", "heic",
    public static final ExecutorService SHORT_EXECUTOR_SERVICE = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1000), new ThreadPoolExecutor.CallerRunsPolicy());
    public static final ExecutorService SYNC_EXECUTOR_SERVICE = new ThreadPoolExecutor(CORE_COUNT, CORE_COUNT,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1000), new ThreadPoolExecutor.CallerRunsPolicy());

    public static final ScheduledExecutorService DELAY_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);

}
