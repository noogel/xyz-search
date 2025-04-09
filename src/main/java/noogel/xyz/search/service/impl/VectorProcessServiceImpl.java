package noogel.xyz.search.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.client.VectorClient;
import noogel.xyz.search.infrastructure.consts.JobTypeEnum;
import noogel.xyz.search.infrastructure.dto.IndexedContentDto;
import noogel.xyz.search.infrastructure.queue.JobMetaDto;
import noogel.xyz.search.infrastructure.queue.WorkQueueService;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import noogel.xyz.search.service.VectorProcessService;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class VectorProcessServiceImpl implements VectorProcessService {

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

}
