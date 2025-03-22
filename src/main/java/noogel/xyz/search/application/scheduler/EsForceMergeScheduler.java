package noogel.xyz.search.application.scheduler;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.repo.FullTextSearchRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EsForceMergeScheduler {
    @Resource
    private FullTextSearchRepo fullTextSearchRepo;

    /**
     * 每天 5 点整理 es
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void init() {
        log.info("定时任务:自动合并索引版本");
        fullTextSearchRepo.forceMerge();
    }
}
