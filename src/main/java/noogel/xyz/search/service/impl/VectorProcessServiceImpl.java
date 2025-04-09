package noogel.xyz.search.service.impl;

import java.time.Duration;

import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.client.VectorClient;
import noogel.xyz.search.infrastructure.consts.JobTypeEnum;
import noogel.xyz.search.infrastructure.dto.IndexedContentDto;
import noogel.xyz.search.infrastructure.model.sqlite.WorkQueueModel;
import noogel.xyz.search.infrastructure.queue.JobMetaDto;
import noogel.xyz.search.infrastructure.queue.JobProcessor;
import noogel.xyz.search.infrastructure.queue.WorkQueueService;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import noogel.xyz.search.service.VectorProcessService;

@Service
@Slf4j
public class VectorProcessServiceImpl implements VectorProcessService, JobProcessor {

    @Resource
    private VectorClient vectorClient;
    @Resource
    private WorkQueueService workQueueService;

    @Override
    public void asyncUpsert(IndexedContentDto indexedContentDto) {
        // 添加到队列
        workQueueService.addDelayJob(JobTypeEnum.INDEXED_CONTENT.name(), indexedContentDto.getResId() + "_ADD",
                JsonHelper.toJson(indexedContentDto), JobMetaDto.ofNow(Duration.ofMinutes(15)));
    }

    @Override
    public void asyncDelete(String resId) {
        // 添加到队列
        IndexedContentDto indexedContentDto = IndexedContentDto.of(resId, null);
        workQueueService.addDelayJob(JobTypeEnum.INDEXED_CONTENT.name(), resId + "_DELETE",
                JsonHelper.toJson(indexedContentDto), JobMetaDto.ofNow(Duration.ofMinutes(15)));
    }

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
