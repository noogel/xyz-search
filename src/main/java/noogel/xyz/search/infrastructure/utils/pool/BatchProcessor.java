package noogel.xyz.search.infrastructure.utils.pool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 批量处理工具
 * 用于收集数据并周期性批量处理
 */
@Slf4j
public class BatchProcessor<T> {

    private final BlockingQueue<DataItem<T>> queue;
    private final ScheduledExecutorService scheduler;
    private final Consumer<List<T>> batchProcessor;
    private final int batchSize;
    private final long intervalMillis;
    private volatile boolean running = true;

    /**
     * 创建批量处理器
     *
     * @param batchSize      批处理大小
     * @param intervalMillis 处理间隔(毫秒)
     * @param batchProcessor 批处理函数
     */
    public BatchProcessor(int batchSize, long intervalMillis, Consumer<List<T>> batchProcessor) {
        this.queue = new LinkedBlockingQueue<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "batch-processor");
            thread.setDaemon(true);
            return thread;
        });
        this.batchProcessor = batchProcessor;
        this.batchSize = batchSize;
        this.intervalMillis = intervalMillis;

        // 启动定时处理任务
        this.scheduler.scheduleAtFixedRate(this::processBatch, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 添加数据项到处理队列
     *
     * @param data     数据对象
     * @param callback 处理完成后的回调
     * @return 是否添加成功
     */
    public boolean add(T data, Runnable callback) {
        if (!running) {
            return false;
        }
        return queue.offer(new DataItem<>(data, callback));
    }

    /**
     * 处理批次数据
     */
    private void processBatch() {
        if (queue.isEmpty()) {
            return;
        }

        List<DataItem<T>> batch = new ArrayList<>(batchSize);
        queue.drainTo(batch, batchSize);

        if (batch.isEmpty()) {
            return;
        }

        try {
            // 提取数据对象列表
            List<T> dataList = batch.stream().map(DataItem::getData).toList();

            // 执行批处理
            batchProcessor.accept(dataList);

            // 执行回调
            batch.forEach(item -> {
                try {
                    if (item.getCallback() != null) {
                        item.getCallback().run();
                    }
                } catch (Exception e) {
                    log.error("Error executing callback", e);
                }
            });
        } catch (Exception e) {
            log.error("Error processing batch", e);
        }
    }

    /**
     * 关闭处理器
     */
    public void shutdown() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(intervalMillis * 2, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 数据项，包含数据对象和回调
     */
    @Data
    @AllArgsConstructor
    private static class DataItem<T> {
        private T data;
        private Runnable callback;
    }
}