package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

@Data
public class ModalInfoDto {
    private String header;
    private String body;
    private boolean show;

    public static ModalInfoDto ofOk(String op) {
        ModalInfoDto dto = new ModalInfoDto();
        dto.setHeader("操作成功");
        dto.setBody(op);
        dto.setShow(true);
        return dto;
    }

    public static ModalInfoDto ofErr(String op) {
        ModalInfoDto dto = new ModalInfoDto();
        dto.setHeader("操作失败");
        dto.setBody(op);
        dto.setShow(true);
        return dto;
    }
}
