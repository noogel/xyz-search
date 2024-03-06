package noogel.xyz.search.infrastructure.model.sqlite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "FileRes", indexes = {
        @Index(name = "idx_fileres_state", columnList = "state"),
        @Index(name = "idx_fileres_dir_dir_dep", columnList = "dir, dir_dep"),
        @Index(name = "idx_fileres_hash", columnList = "hash")
})
public class FileRes {

    /**
     * ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    /**
     * 文件目录
     */
    private String dir;
    /**
     * 目录深度
     */
    private Integer dirDep;
    /**
     * 文件名.扩展名
     */
    private String name;
    /**
     * 文件大小
     */
    private Long size;
    /**
     * 资源最近更新时间 秒
     */
    private Long modifiedAt;

    /**
     * 扩展
     * FILE:EPUB
     * FILE:TXT
     */
    private String ext;
    /**
     * 文件 hash
     */
    private String hash;
    /**
     * 根据资源名称计算的分数
     */
    private Long rank;
    /**
     * 资源 meta 名称
     */
    private String meta;
    /**
     * 文件状态
     * 0 有效
     * 1 失效
     */
    private Integer state;
}
