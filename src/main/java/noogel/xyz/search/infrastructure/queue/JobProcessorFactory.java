package noogel.xyz.search.infrastructure.queue;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务处理器工厂
 * 用于管理和获取不同类型的任务处理器
 */
@Component
@Slf4j
public class JobProcessorFactory {

    @Resource
    private ApplicationContext applicationContext;

    private final Map<String, JobProcessor> processorMap = new HashMap<>();
    private final Map<String, Integer> proprityMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // 获取所有JobProcessor实现类
        Map<String, JobProcessor> processors = applicationContext.getBeansOfType(JobProcessor.class);

        // 注册处理器
        for (JobProcessor processor : processors.values()) {
            String jobType = processor.getJobType();
            processorMap.put(jobType, processor);
            proprityMap.put(jobType, processor.getPriorities());
            log.info("注册任务处理器: {}", jobType);
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
     * @return
     */
    public Map<String, Integer> getAllProprity() {
        return proprityMap;
    }
} 