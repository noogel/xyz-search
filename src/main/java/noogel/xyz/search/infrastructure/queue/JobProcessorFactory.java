package noogel.xyz.search.infrastructure.queue;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 任务处理器工厂
 * 用于管理和获取不同类型的任务处理器
 */
@Component
@Slf4j
public class JobProcessorFactory {

    @Resource
    private List<JobProcessor> jobProcessorList;

    private final Map<String, JobProcessor> processorMap = new HashMap<>();
    private final Map<String, Integer> proprityMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // 注册处理器
        for (JobProcessor processor : jobProcessorList) {
            String jobType = processor.getJobType();
            processorMap.put(jobType, processor);
            proprityMap.put(jobType, processor.getPriorities());
            log.info("注册任务处理器: {}", jobType);
        }
    }

    @PreDestroy
    public void destroy() {
        jobProcessorList.forEach(l -> l.executorService().shutdown());
        AbstractJobProcessor.EXECUTORS.shutdown();
        try {
            for (JobProcessor jobProcessor : jobProcessorList) {
                if (!jobProcessor.executorService().awaitTermination(10, TimeUnit.SECONDS)) {
                    jobProcessor.executorService().shutdownNow();
                }
            }
            if (!AbstractJobProcessor.EXECUTORS.awaitTermination(10, TimeUnit.SECONDS)) {
                AbstractJobProcessor.EXECUTORS.shutdownNow();
            }
        } catch (InterruptedException e) {
            jobProcessorList.forEach(l -> l.executorService().shutdownNow());
            AbstractJobProcessor.EXECUTORS.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取任务处理器
     *
     * @param jobType 任务类型
     * @return 任务处理器
     */
    public JobProcessor getProcessor(String jobType) {
        return processorMap.get(jobType);
    }


    /**
     * 获取所有任务处理器
     *
     * @return 任务处理器映射
     */
    public Map<String, JobProcessor> getAllProcessors() {
        return processorMap;
    }


    /**
     * 获取所有优先级
     *
     * @return
     */
    public Map<String, Integer> getAllProprity() {
        return proprityMap;
    }

} 