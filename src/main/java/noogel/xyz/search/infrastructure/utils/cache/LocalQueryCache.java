package noogel.xyz.search.infrastructure.utils.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

/**
 * 查询缓存管理器
 */
@Slf4j
public class LocalQueryCache {
    // 查询结果缓存，key 为查询字符串，value 为缓存条目
    private final ConcurrentHashMap<String, CacheEntry<Object>> cache = new ConcurrentHashMap<>();
    // 缓存过期时间（毫秒）
    private final long expireTime;
    // 最大缓存条目数
    private final int maxSize;

    /**
     * 创建查询缓存管理器
     *
     * @param expireTime 缓存过期时间（毫秒）
     * @param maxSize    最大缓存条目数
     */
    public LocalQueryCache(long expireTime, int maxSize) {
        this.expireTime = expireTime;
        this.maxSize = maxSize;
    }

    /**
     * 从缓存获取数据，如果不存在或已过期则执行查询并缓存结果
     *
     * @param key      缓存键
     * @param supplier 数据提供者
     * @param <T>      数据类型
     * @return 查询结果
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(String key, Supplier<T> supplier) {
        // 检查缓存
        CacheEntry<Object> entry = cache.get(key);
        if (entry != null && !entry.isExpired(expireTime)) {
            return (T) entry.getValue();
        }

        // 执行查询
        T result = supplier.get();

        // 缓存结果
        if (cache.size() < maxSize) {
            cache.put(key, new CacheEntry<>(result));
        } else {
            // 清理过期缓存
            cleanExpiredCache();
            if (cache.size() < maxSize) {
                cache.put(key, new CacheEntry<>(result));
            }
        }

        return result;
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 清理过期缓存
     */
    private void cleanExpiredCache() {
        if (cache.size() > maxSize / 2) {
            int beforeSize = cache.size();
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired(expireTime));
            int afterSize = cache.size();
            log.debug("清理过期缓存: {} -> {}", beforeSize, afterSize);
        }
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long timestamp;

        public CacheEntry(T value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired(long expireTime) {
            return System.currentTimeMillis() - timestamp > expireTime;
        }

        public T getValue() {
            return value;
        }
    }
} 