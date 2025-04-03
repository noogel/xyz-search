package noogel.xyz.search.infrastructure.queue;

import java.time.Duration;

/**
 * 工作队列服务接口
 * 用于管理异步任务的添加和执行
 */
public interface WorkQueueService {

    /**
     * 添加延迟任务到队列
     *
     * @param jobType 队列名称
     * @param jobData 任务数据
     * @param maxRetry 最大重试次数
     * @param timeout 延迟秒数
     * @return 任务ID
     */
    Long addDelayJob(String jobType, String jobData, Integer maxRetry, Duration timeout);
} 