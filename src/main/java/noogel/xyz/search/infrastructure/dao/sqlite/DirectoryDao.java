package noogel.xyz.search.infrastructure.dao.sqlite;

import noogel.xyz.search.infrastructure.model.sqlite.DirectoryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DirectoryDao extends JpaRepository<DirectoryModel, Long> {

    @NonNull
    Optional<DirectoryModel> findByPath(@NonNull String path);

    @Query("select d from DirectoryModel d where d.path like concat(?1, '%') and d.dep = ?2")
    @NonNull
    List<DirectoryModel> findByPathStartsWithAndDep(@NonNull String path, @NonNull Integer dep);

    @Transactional
    @Modifying
    @Query("delete from DirectoryModel d where d.path like concat(:path, '%')")
    int deleteByPathStartsWith(@Param("path") @NonNull String path);

    @Transactional
    @Modifying
    @Query("delete from DirectoryModel d where d.path = :path")
    int deleteByPath(@Param("path") @NonNull String path);
}
