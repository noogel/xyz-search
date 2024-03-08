package noogel.xyz.search.infrastructure.dao.sqlite;


import noogel.xyz.search.infrastructure.model.sqlite.FileResModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FileResDao extends JpaRepository<FileResModel, Long> {

    @Query("select f from FileResModel f where f.dir = :dir")
    @NonNull
    List<FileResModel> batchFindByDir(@Param("dir") @NonNull String dir);

    @NonNull
    Optional<FileResModel> findFirstByHash(@NonNull String hash);

    @Transactional
    @Modifying
    @Query("update FileResModel f set f.state = ?1 where f.id = ?2")
    int updateStateById(@NonNull Integer state, @NonNull Long id);

    @Transactional
    @Modifying
    @Query("update FileResModel f set f.state = ?1 where f.dir like concat(?2, '%')")
    int updateStateByDirStartsWith(@NonNull Integer state, @NonNull String dir);

    List<FileResModel> findTop12ByState(@NonNull Integer state);

    @Transactional
    @Modifying
    @Query("update FileResModel f set f.state = ?1, f.options = ?2 where f.id = ?3")
    int updateStateAndOptionsById(@NonNull Integer state, @NonNull String options, @NonNull Long id);
}
