package noogel.xyz.search.infrastructure.consts;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RelativeStateEnum {
    /**
     * 有效，未索引
     */
    VALID(0),
    /**
     * 有效，已索引
     */
    INDEXED(1),
    /**
     * 无效，待删除
     */
    INVALID(2),
    /**
     * 无效，标记删除
     */
    DELETED(3),
    /**
     * 处理状态异常
     */
    ERROR(4),
    ;

    private final int val;
}
