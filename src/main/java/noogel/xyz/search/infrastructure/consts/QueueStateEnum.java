package noogel.xyz.search.infrastructure.consts;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QueueStateEnum {
    /**
     * 初始任务
     */
    INIT(0),
    /**
     * 处理中
     */
    PROCESSING(1),
    /**
     * 成功
     */
    SUCCESS(2),
    /**
     * 失败
     */
    FAILED(3),
    ;

    private final int val;
}
