package noogel.xyz.search.infrastructure.queue.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.QueueStateEnum;
import noogel.xyz.search.infrastructure.dao.sqlite.WorkQueueDao;
import noogel.xyz.search.infrastructure.model.sqlite.WorkQueueModel;
import noogel.xyz.search.infrastructure.queue.*;
import noogel.xyz.search.infrastructure.utils.sqlite.SqliteLock;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 工作队列服务实现类
 */
@Service
@Slf4j
public class WorkQueueServiceImpl implements WorkQueueService {
    @Resource
    private WorkQueueDao workQueueDao;

    @Resource
    private JobProcessorFactory jobProcessorFactory;

    private final ExecutorService workExecutorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1), new ThreadPoolExecutor.CallerRunsPolicy());

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private final AtomicBoolean running = new AtomicBoolean(true);

    // 任务优先级配置，数字越小优先级越高
    private final Map<String, Integer> jobTypePriorities = new HashMap<>();

    // 默认优先级
    private static final int DEFAULT_PRIORITY = 100;

    @PostConstruct
    public void init() {
        // 初始化任务优先级
        initJobTypePriorities();
        // 启动任务处理线程
        startJobProcessor();
        // 启动超时任务检查线程
        startTimeoutChecker();
    }

    /**
     * 初始化任务优先级
     */
    private void initJobTypePriorities() {
        // 设置各队列的优先级，数字越小优先级越高
        jobTypePriorities.putAll(jobProcessorFactory.getAllProprity());
    }

    /**
     * 获取队列优先级
     */
    private int getJobTypePriority(String jobType) {
        return jobTypePriorities.getOrDefault(jobType, DEFAULT_PRIORITY);
    }

    @PreDestroy
    public void destroy() {
        running.set(false);
        scheduledExecutorService.shutdown();
        workExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
            if (!workExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                workExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutorService.shutdownNow();
            workExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 启动任务处理线程
     */
    private void startJobProcessor() {
        CompletableFuture.runAsync(() -> {
            while (running.get()) {
                try {
                    // 获取所有队列的待处理任务，限制为50条
                    List<WorkQueueModel> pendingJobs = getPendingJobs();
                    if (pendingJobs.isEmpty()) {
                        Thread.sleep(3000);
                        continue;
                    }
                    // 按优先级排序任务
                    List<WorkQueueModel> sortedJobs = sortJobsByPriority(pendingJobs);
                    // 任务列表
                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    // 处理每个任务
                    for (WorkQueueModel job : sortedJobs) {
                        ExecutorService runService = getJobExecutorService(job);
                        futures.add(CompletableFuture.runAsync(() -> processJob(job), runService));
                    }
                    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
                } catch (Exception e) {
                    log.error("处理任务时发生错误", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, workExecutorService);
    }

    /**
     * 获取任务的执行器服务
     */
    private ExecutorService getJobExecutorService(WorkQueueModel job) {
        JobProcessor processor = jobProcessorFactory.getProcessor(job.getJobType());
        return Optional.ofNullable(processor)
                .map(JobProcessor::executorService).orElse(AbstractJobProcessor.EXECUTORS);
    }

    @SqliteLock
    public List<WorkQueueModel> getPendingJobs() {
        return workQueueDao.listActiveJob(QueueStateEnum.INIT.getVal(),
                System.currentTimeMillis(), PageRequest.of(0, 50));
    }

    /**
     * 按优先级排序任务
     */
    private List<WorkQueueModel> sortJobsByPriority(List<WorkQueueModel> jobs) {
        return jobs.stream().sorted(Comparator.comparingInt(job -> getJobTypePriority(job.getJobType())))
                .collect(Collectors.toList());
    }

    /**
     * 启动超时任务检查线程
     */
    private void startTimeoutChecker() {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (!running.get()) {
                return;
            }
            // todo 定期清理完成的任务
            try {
                // 获取所有超时的任务，限制为10条
                List<WorkQueueModel> processingJobs = getProcessingJobs();

                if (processingJobs.isEmpty()) {
                    return;
                }

                long currentTime = System.currentTimeMillis();

                // 检查每个任务是否超时
                for (WorkQueueModel job : processingJobs) {
                    if (job.getReleaseTime() < currentTime) {
                        // 任务超时，重新加入队列
                        handleTimeoutJob(job);
                    }
                }
            } catch (Exception e) {
                log.error("检查超时任务时发生错误", e);
            }
        }, 10, 10, TimeUnit.MINUTES);
    }

    @SqliteLock
    public List<WorkQueueModel> getProcessingJobs() {
        return workQueueDao.listTimeoutJob(QueueStateEnum.PROCESSING.getVal(),
                System.currentTimeMillis(), PageRequest.of(0, 10));
    }

    /**
     * 处理任务
     */
    private void processJob(WorkQueueModel job) {
        try {
            // 更新任务状态为处理中
            long releaseTime = System.currentTimeMillis() + job.getTimeout() * 1000;
            boolean updated = updateJobState(job.getId(), QueueStateEnum.PROCESSING.getVal(), releaseTime,
                    job.getRunCount() + 1);
            if (!updated) {
                log.warn("更新任务状态失败，任务ID: {}", job.getId());
                return;
            }
            // 执行任务
            boolean result = executeJob(job);
            if (result) {
                // 更新任务状态为成功
                updateJobState(job.getId(), QueueStateEnum.SUCCESS.getVal(), 0L, job.getRunCount() + 1);
            } else {
                // 更新失败
                setJobFailed(job);
            }
        } catch (Exception e) {
            log.error("处理任务时发生错误，任务ID: {}", job.getId(), e);
            // 更新失败
            setJobFailed(job);
        }
    }

    private void setJobFailed(WorkQueueModel job) {
        // 检查是否超过最大重试次数
        if (job.getRunCount() >= job.getMaxRetry()) {
            // 超过最大重试次数，标记为失败
            updateJobState(job.getId(), QueueStateEnum.FAILED.getVal(), 0L, job.getRunCount());
        } else {
            // 未超过最大重试次数，重新加入队列
            updateJobState(job.getId(), QueueStateEnum.INIT.getVal(), 0L, job.getRunCount());
        }
    }

    /**
     * 处理超时任务
     */
    private void handleTimeoutJob(WorkQueueModel job) {
        log.warn("任务超时，任务ID: {}", job.getId());

        // 检查是否超过最大重试次数
        if (job.getRunCount() >= job.getMaxRetry()) {
            // 超过最大重试次数，标记为失败
            updateJobState(job.getId(), QueueStateEnum.FAILED.getVal(), 0L, job.getRunCount());
        } else {
            // 未超过最大重试次数，重新加入队列
            updateJobState(job.getId(), QueueStateEnum.INIT.getVal(), 0L, job.getRunCount());
        }
    }

    /**
     * 执行任务
     */
    private boolean executeJob(WorkQueueModel job) throws Exception {
        // 获取任务处理器
        JobProcessor processor = jobProcessorFactory.getProcessor(job.getJobType());

        if (processor == null) {
            log.error("未找到任务处理器，任务ID: {}, 队列: {}", job.getId(), job.getJobType());
            throw new RuntimeException("未找到任务处理器: " + job.getJobType());
        }

        // 执行任务
        return processor.process(job);
    }

    @SqliteLock
    public boolean updateJobState(Long id, Integer jobState, Long releaseTime, Integer runCount) {
        int updated = workQueueDao.updateJobStateAndReleaseTimeById(jobState, releaseTime, runCount, id);
        return updated > 0;
    }

    @SqliteLock
    @Override
    public void addDelayJob(String jobType, String uk, String jobData, JobMetaDto jobMetaDto) {
        WorkQueueModel job = new WorkQueueModel();
        job.setJobType(jobType);
        job.setUuid(StringUtils.isEmpty(uk) ? UUID.randomUUID().toString() : uk);
        job.setJobData(jobData);
        job.setMaxRetry(jobMetaDto.getMaxRetry() != null ? jobMetaDto.getMaxRetry() : 3);
        job.setRunCount(0);
        job.setJobState(QueueStateEnum.INIT.getVal());
        job.setActiveTime(System.currentTimeMillis() + jobMetaDto.getDelay().toMillis());
        job.setTimeout(jobMetaDto.getTimeout().toSeconds());
        job.setCreateTime(System.currentTimeMillis());
        job.setUpdateTime(System.currentTimeMillis());
        workQueueDao.save(job);
    }

    @SqliteLock
    @Override
    public void resetJobs(String jobType) {
        workQueueDao.deleteByJobType(jobType);
    }

}