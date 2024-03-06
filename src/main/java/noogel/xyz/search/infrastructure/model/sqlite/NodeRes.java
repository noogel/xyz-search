package noogel.xyz.search.infrastructure.model.sqlite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "NodeRes", indexes = {
        @Index(name = "idx_noderes_file_id", columnList = "file_id")
})
public class NodeRes {
    /**
     * ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    /**
     * 文件ID
     */
    private Long fileId;
    /**
     * 节点唯一 UUID
     */
    private String resId;
    /**
     * 章节名
     */
    private String chapter;

    /**
     * 搜索内容
     * searchableText
     */
    private String text;
    /**
     * 内容 HASH
     */
    private String hash;
    /**
     * 内容大小
     */
    private Integer size;

}
