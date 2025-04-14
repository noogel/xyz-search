package noogel.xyz.search.infrastructure.dto.api;

import lombok.Data;

@Data
public class ChatRequestDto {
    private String message;
    private String resId;
    private String chatId;
}
