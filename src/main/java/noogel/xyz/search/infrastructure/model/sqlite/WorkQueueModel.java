package noogel.xyz.search.infrastructure.model.sqlite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "WorkQueue", indexes = {
        @Index(name = "idx_workqueue_mix", columnList = "jobState, tube, releaseTime"),
        @Index(name = "idx_workqueue_updatetime", columnList = "updateTime")
})
public class WorkQueueModel {
    /**
     * ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    /**
     * 任务状态
     */
    private Integer jobState;
    /**
     * 超时时间
     */
    private Long releaseTime;
    /**
     * 队列名称
     */
    private String jobType;
    /**
     * 执行次数
     */
    private Integer runCount;
    /**
     * 超时（秒）
     */
    private Long timeout;
    /**
     * 最大重试次数
     */
    private Integer maxRetry;
    /**
     * 任务内容
     */
    private String jobData;
    /**
     * 创建时间
     */
    private Long createTime;
    /**
     * 更新时间
     */
    private Long updateTime;
}
