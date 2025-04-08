package noogel.xyz.search.infrastructure.dao.sqlite;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import noogel.xyz.search.infrastructure.model.sqlite.WorkQueueModel;

public interface WorkQueueDao extends JpaRepository<WorkQueueModel, Long> {

    /**
     * 获取超时任务
     *
     * @param jobState
     * @param activeTime
     * @return
     */
    @Query("select f from WorkQueueModel f where f.jobState = :jobState and f.activeTime < :activeTime")
    @NonNull
    List<WorkQueueModel> listActiveJob(@Param("jobState") @NonNull Integer jobState,
            @Param("activeTime") @NonNull Long activeTime, Pageable pageable);

    /**
     * 更新任务状态和超时时间
     *
     * @param jobState
     * @param releaseTime
     * @param id
     * @return
     */
    @Transactional
    @Modifying
    @Query("update WorkQueueModel f set f.jobState = :jobState, f.releaseTime = :releaseTime, f.runCount = :runCount where f.id = :id")
    int updateJobStateAndReleaseTimeById(@Param("jobState") @NonNull Integer jobState,
            @Param("releaseTime") @NonNull Long releaseTime, @Param("runCount") @NonNull Integer runCount,
            @Param("id") @NonNull Long id);

    /**
     * 获取超时任务
     *
     * @param jobState
     * @param releaseTime
     * @return
     */
    @Query("select f from WorkQueueModel f where f.jobState = :jobState and f.releaseTime < :releaseTime")
    @NonNull
    List<WorkQueueModel> listTimeoutJob(@Param("jobState") @NonNull Integer jobState,
            @Param("releaseTime") @NonNull Long releaseTime, Pageable pageable);
}
