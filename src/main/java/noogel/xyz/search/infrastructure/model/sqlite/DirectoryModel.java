package noogel.xyz.search.infrastructure.model.sqlite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Directory", indexes = {
        @Index(name = "idx_directorymodel_path", columnList = "path")
})
public class DirectoryModel {
    /**
     * ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 目录
     */
    private String path;
    /**
     * 深度
     */
    private Integer dep;
}
