package noogel.xyz.search.infrastructure.utils.pool;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 批处理器工厂
 * 用于创建和管理批处理器实例
 */
@Component
public class BatchProcessorFactory {

    private final Map<String, BatchProcessor<?>> processors = new ConcurrentHashMap<>();

    /**
     * 创建或获取批处理器
     *
     * @param name           处理器名称
     * @param batchSize      批处理大小
     * @param intervalMillis 处理间隔(毫秒)
     * @param batchProcessor 批处理函数
     * @param <T>            数据类型
     * @return 批处理器实例
     */
    @SuppressWarnings("unchecked")
    public <T> BatchProcessor<T> getOrCreate(String name, int batchSize, long intervalMillis, Consumer<List<T>> batchProcessor) {
        return (BatchProcessor<T>) processors.computeIfAbsent(name, 
            k -> new BatchProcessor<>(batchSize, intervalMillis, batchProcessor));
    }

    /**
     * 获取已存在的批处理器
     *
     * @param name 处理器名称
     * @param <T>  数据类型
     * @return 批处理器实例，不存在则返回null
     */
    @SuppressWarnings("unchecked")
    public <T> BatchProcessor<T> get(String name) {
        return (BatchProcessor<T>) processors.get(name);
    }

    /**
     * 关闭所有批处理器
     */
    @PreDestroy
    public void shutdownAll() {
        processors.values().forEach(BatchProcessor::shutdown);
        processors.clear();
    }
} 