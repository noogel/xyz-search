package noogel.xyz.search.infrastructure.dao.sqlite;


import noogel.xyz.search.infrastructure.model.sqlite.FileRes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FileResDao extends JpaRepository<FileRes, Long> {

    @Query("select distinct f.dir from FileRes f where f.dir like concat(:dir, '%') and f.dirDep = :dirDep")
    @NonNull
    List<String> batchFindDistinctDirByDirStartsWithAndDirDep(
            @Param("dir") @NonNull String dir, @Param("dirDep") @NonNull Integer dirDep);

    @Query("select f from FileRes f where f.dir = :dir")
    @NonNull
    List<FileRes> batchFindByDir(@Param("dir") @NonNull String dir);

    @NonNull
    Optional<FileRes> findFirstByHash(@NonNull String hash);

    @Transactional
    @Modifying
    @Query("update FileRes f set f.state = ?1 where f.id = ?2")
    int updateStateById(@NonNull Integer state, @NonNull Long id);

    @Transactional
    @Modifying
    @Query("update FileRes f set f.state = ?1 where f.dir like concat(?2, '%')")
    int updateStateByDirStartsWith(@NonNull Integer state, @NonNull String dir);
}
