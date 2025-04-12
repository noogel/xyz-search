package noogel.xyz.search.infrastructure.model.sqlite;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "WorkQueue", indexes = {
        @Index(name = "idx_workqueue_release", columnList = "jobState, jobType, releaseTime"),
        @Index(name = "idx_workqueue_active", columnList = "jobState, jobType, activeTime"),
        @Index(name = "idx_workqueue_uuid", columnList = "uuid"),
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
     * 唯一标识
     */
    private String uuid;
    /**
     * 任务状态
     */
    private Integer jobState;
    /**
     * 激活时间
     */
    private Long activeTime;
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
