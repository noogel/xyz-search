package noogel.xyz.search.application.scheduler;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dao.ElasticSearchFtsDao;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EsForceMergeScheduler {
    @Resource
    private ElasticSearchFtsDao ftsDao;

    @Scheduled(cron = "0 0 5 * * *")
    public void init() {
        log.info("auto forceMerge runDelay.");
        ftsDao.forceMerge();
    }

}
