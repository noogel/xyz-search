package noogel.xyz.search.service.job_processor;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.JobTypeEnum;
import noogel.xyz.search.infrastructure.dto.IndexedContentDto;
import noogel.xyz.search.infrastructure.model.sqlite.WorkQueueModel;
import noogel.xyz.search.infrastructure.queue.JobProcessor;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IndexContentJobProcessor implements JobProcessor {

    @Override
    public void process(WorkQueueModel job) throws Exception {
        // 解析任务数据
        IndexedContentDto indexedContentDto = JsonHelper.fromJson(job.getJobData(), IndexedContentDto.class);

        if (indexedContentDto.getContent() == null) {
            // todo 删除向量
        } else {
            // 添加到向量
            // vectorClient.getVectorStore().add();
        }
    }

    @Override
    public String getJobType() {
        return JobTypeEnum.INDEXED_CONTENT.name();
    }

    @Override
    public int getPriorities() {
        return 1;
    }

}
