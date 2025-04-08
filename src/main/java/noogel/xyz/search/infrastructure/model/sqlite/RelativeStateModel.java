package noogel.xyz.search.infrastructure.model.sqlite;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "RelativeState", indexes = {
        @Index(name = "idx_relativestate_resId_type", columnList = "resId, type"),
        @Index(name = "idx_relativestate_state_type", columnList = "state, type")
})

public class RelativeStateModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    /**
     * 资源ID
     */
    private String resId;
    /**
     * 类型
     */
    private String type;
    /**
     * 状态
     */
    private Integer state;
    /**
     * 选项
     */
    private String options;
    /**
     * 创建时间
     */
    private Long createTime;
    /**
     * 更新时间
     */
    private Long updateTime;
}
