package noogel.xyz.search.application.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.repo.FullTextSearchService;

@Service
@Slf4j
public class EsForceMergeScheduler {
    @Resource
    private FullTextSearchService fullTextSearchService;

    /**
     * 每天 5 点整理 es
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void init() {
        log.info("定时任务:自动合并索引版本");
        fullTextSearchService.getBean().forceMerge();
    }
}
