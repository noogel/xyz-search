package noogel.xyz.search.infrastructure.dao.sqlite;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import noogel.xyz.search.infrastructure.model.sqlite.ConfigModel;

@Repository
public interface ConfigDao extends JpaRepository<ConfigModel, String> {
    
    Optional<ConfigModel> findByKey(String key);
    
    @Query("SELECT c FROM ConfigModel c WHERE c.key LIKE %:keyword% OR c.description LIKE %:keyword%")
    List<ConfigModel> searchByKeyword(String keyword);
    
    boolean existsByKey(String key);
} 