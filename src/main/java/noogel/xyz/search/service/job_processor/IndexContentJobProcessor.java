package noogel.xyz.search.service.job_processor;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.client.VectorClient;
import noogel.xyz.search.infrastructure.consts.JobTypeEnum;
import noogel.xyz.search.infrastructure.dto.IndexedContentDto;
import noogel.xyz.search.infrastructure.model.sqlite.WorkQueueModel;
import noogel.xyz.search.infrastructure.queue.JobProcessor;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class IndexContentJobProcessor implements JobProcessor {

    @Resource
    private VectorClient vectorClient;

    @Override
    public boolean process(WorkQueueModel job) throws Exception {
        if (!vectorClient.isEnabled()) {
            return true;
        }
        // 解析任务数据
        IndexedContentDto indexedContentDto = JsonHelper.fromJson(job.getJobData(), IndexedContentDto.class);

        if (indexedContentDto.getContent() == null) {
            return deleteVectorStore(indexedContentDto);
        } else {
            return addToVectorStore(indexedContentDto);
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

    private boolean deleteVectorStore(IndexedContentDto dto) {
        try {
            log.info("删除向量数据: {}", dto.getResId());
            String filterExpression = String.format("resId == '%s'", dto.getResId());
            vectorClient.getVectorStore().delete(filterExpression);
            return true;
        } catch (Exception ex) {
            log.error("deleteVectorStore error {}", dto.getResId(), ex);
            return false;
        }
    }


    private boolean addToVectorStore(IndexedContentDto dto) {
        try {
            log.info("添加向量数据 prepare {}", dto.getResId());
            String content = dto.getContent().genContent();
            Map<String, String> contentMeta = dto.getContent().getMetadata();
            if (StringUtils.isEmpty(content)) {
                return true;
            }
            // 按照最长 1000 字符分割
            List<String> splitList = splitText(content, 1000);
            Map<String, Object> metadata = new HashMap<>();
            Optional.ofNullable(contentMeta).ifPresent(l -> l.forEach((k, v) -> metadata.put(k, Objects.isNull(v) ? "" : v)));
            metadata.put("resId", dto.getResId());

            List<Document> documents = new ArrayList<>();

            for (int i = 0; i < splitList.size(); i++) {
                documents.add(new Document(splitList.get(i), new HashMap<>(metadata)));
            }

            // Add the documents to Elasticsearch
            vectorClient.getVectorStore().add(documents);
            log.info("添加向量数据 success {}", dto.getResId());
            return true;
        } catch (Exception ex) {
            log.error("添加向量数据 error {}", dto.getResId(), ex);
            return false;
        }
    }

    public static List<String> splitText(String text, int maxLength) {
        List<String> paragraphs = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            int lastPunctuation = findLastPunctuation(text, start, end);
            end = (lastPunctuation != -1) ? lastPunctuation + 1 : end;
            paragraphs.add(text.substring(start, end).trim());
            start = end;
        }
        return paragraphs;
    }

    private static int findLastPunctuation(String text, int start, int end) {
        String[] punctuations = {".", ",", ";", "!", "?"};
        for (int i = end - 1; i >= start; i--) {
            for (String p : punctuations) {
                if (text.startsWith(p, i)) return i;
            }
        }
        return -1;
    }
}
