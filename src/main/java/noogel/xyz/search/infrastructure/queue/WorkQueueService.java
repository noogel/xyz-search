package noogel.xyz.search.infrastructure.queue;

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
     * @param jobMetaDto 任务元数据
     */
    void addDelayJob(String jobType, String uk, String jobData, JobMetaDto jobMetaDto);

    void resetJobs(String jobType);
} 