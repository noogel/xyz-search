package noogel.xyz.search.infrastructure.queue;

import noogel.xyz.search.infrastructure.model.sqlite.WorkQueueModel;

import java.util.concurrent.ExecutorService;

/**
 * 任务处理器接口
 * 用于处理不同类型的任务
 */
public interface JobProcessor {

    ExecutorService executorService();

    /**
     * 处理任务
     *
     * @param job 任务
     * @throws Exception 处理异常
     */
    boolean process(WorkQueueModel job) throws Exception;

    /**
     * 获取处理器支持的任务类型
     *
     * @return 任务类型
     */
    String getJobType();

    /**
     * 优先级，数字越小优先级越高
     * @return
     */
    int getPriorities();
} 