package noogel.xyz.search.infrastructure.dao.sqlite;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import noogel.xyz.search.infrastructure.model.sqlite.RelativeStateModel;

public interface RelativeStateDao extends JpaRepository<RelativeStateModel, Long> {

    /**
     * 根据资源ID和类型查找相对状态
     *
     * @param resId 资源ID
     * @param type  类型
     * @return 相对状态
     */
    Optional<RelativeStateModel> findByResIdAndType(@NonNull String resId, @NonNull String type);

    /**
     * 根据状态和类型查找相对状态列表
     *
     * @param state 状态
     * @param type  类型
     * @return 相对状态列表
     */
    List<RelativeStateModel> findByStateAndType(@NonNull Integer state, @NonNull String type);

    /**
     * 根据类型查找相对状态列表
     *
     * @param type 类型
     * @return 相对状态列表
     */
    List<RelativeStateModel> findByType(@NonNull String type);

    /**
     * 更新相对状态
     *
     * @param state     状态
     * @param options   选项
     * @param updateTime 更新时间
     * @param id        记录ID
     * @return 更新记录数
     */
    @Transactional
    @Modifying
    @Query("update RelativeStateModel r set r.state = :state, r.options = :options, r.updateTime = :updateTime where r.id = :id")
    int updateStateAndOptionsById(@Param("state") @NonNull Integer state,
                                 @Param("options") String options,
                                 @Param("updateTime") @NonNull Long updateTime,
                                 @Param("id") @NonNull Long id);

    /**
     * 根据资源ID和类型删除相对状态
     *
     * @param resId 资源ID
     * @param type  类型
     * @return 删除记录数
     */
    @Transactional
    @Modifying
    @Query("delete from RelativeStateModel r where r.resId = :resId and r.type = :type")
    int deleteByResIdAndType(@Param("resId") @NonNull String resId, @Param("type") @NonNull String type);
} 