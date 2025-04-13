package noogel.xyz.search.infrastructure.queue;

import lombok.Data;

import java.time.Duration;

@Data
public class JobMetaDto {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    /**
     * 最大重试次数
     */
    private Integer maxRetry;
    /**
     * 延迟时间
     */
    private Duration delay;
    /**
     * 执行超时时间
     */
    private Duration timeout;

    public static JobMetaDto of(Integer maxRetry, Duration delay, Duration timeout) {
        JobMetaDto jobMetaDto = new JobMetaDto();
        jobMetaDto.setMaxRetry(maxRetry);
        jobMetaDto.setDelay(delay);
        jobMetaDto.setTimeout(timeout);
        return jobMetaDto;
    }

    public static JobMetaDto ofNow(Duration timeout) {
        JobMetaDto jobMetaDto = new JobMetaDto();
        jobMetaDto.setMaxRetry(3);
        jobMetaDto.setDelay(Duration.ZERO);
        jobMetaDto.setTimeout(timeout);
        return jobMetaDto;
    }

    public static JobMetaDto ofDelay(Duration delay) {
        JobMetaDto jobMetaDto = new JobMetaDto();
        jobMetaDto.setMaxRetry(3);
        jobMetaDto.setDelay(delay);
        jobMetaDto.setTimeout(DEFAULT_TIMEOUT);
        return jobMetaDto;
    }
}
