package noogel.xyz.search.application.scheduler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.CommonsConstConfig;
import noogel.xyz.search.infrastructure.dao.ElasticSearchFtsDao;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EsForceMergeScheduler {
    @Resource
    private ElasticSearchFtsDao ftsDao;

    @PostConstruct
    public void init() {
        // 执行时间点
        long dailyRunAt = 5 * 60 * 60;
        // 一天时间
        long oneDay = 24 * 60 * 60;
        // 今天已经过去多久
        long todayPast = LocalDateTime.now().getHour() * 3600 + LocalDateTime.now().getMinute() * 60 + LocalDateTime.now().getSecond();
        long initialDelay = todayPast > dailyRunAt ? (oneDay - todayPast + dailyRunAt) : (dailyRunAt - todayPast);
        log.info("auto forceMerge runDelay {}ms", initialDelay);
        // 定时执行
        CommonsConstConfig.COMMON_SCHEDULED_SERVICE.scheduleAtFixedRate(
                () -> ftsDao.forceMerge(),
                initialDelay,
                oneDay,
                TimeUnit.SECONDS
        );
    }

}
