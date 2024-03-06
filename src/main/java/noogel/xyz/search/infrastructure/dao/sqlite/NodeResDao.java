package noogel.xyz.search.infrastructure.dao.sqlite;


import noogel.xyz.search.infrastructure.model.sqlite.NodeRes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeResDao extends JpaRepository<NodeRes, Long> {
}
