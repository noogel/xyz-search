package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class TaskDto {
    /**
     * 任务 ID
     */
    private Long taskId;
    /**
     * 任务提交时间
     */
    private Long taskOpAt;

    public static TaskDto generateTask() {
        TaskDto dto = new TaskDto();
        dto.setTaskId(Instant.now().getEpochSecond());
        dto.setTaskOpAt(Instant.now().getEpochSecond());
        return dto;
    }
}
