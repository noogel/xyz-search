package noogel.xyz.search.infrastructure.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.model.sqlite.WorkQueueModel;
import noogel.xyz.search.service.IndexingService;
import org.springframework.stereotype.Component;

/**
 * 文件索引任务处理器
 */
@Component
@Slf4j
public class FileIndexingJobProcessor implements JobProcessor {

    @Resource
    private IndexingService indexingService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void process(WorkQueueModel job) throws Exception {
        log.info("处理文件索引任务: {}", job.getId());

        // 解析任务数据
        FileIndexingJobData jobData = objectMapper.readValue(job.getJobData(), FileIndexingJobData.class);
        // log
        System.out.println(jobData);
    }

    @Override
    public String getJobType() {
        return "FILE_INDEXING";
    }

    @Override
    public int getPriorities() {
        return 0;
    }

    /**
     * 文件索引任务数据
     */
    public static class FileIndexingJobData {
        private Long fileId;

        public Long getFileId() {
            return fileId;
        }

        public void setFileId(Long fileId) {
            this.fileId = fileId;
        }
    }
} 