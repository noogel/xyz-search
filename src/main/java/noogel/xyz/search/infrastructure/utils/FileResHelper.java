package noogel.xyz.search.infrastructure.utils;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.dao.FileResWriteDto;

@Slf4j
public abstract class FileResHelper {

    public static FileResWriteDto genFileFsDto(@Nonnull File file) {
        FileResWriteDto dto = new FileResWriteDto();
        dto.setResId(UUID.randomUUID().toString().replace("-", ""));
        dto.setOptions(new HashMap<>());
        dto.setDir(file.getParent());
        dto.setName(file.getName());
        dto.setSize(file.length());
        dto.setRank(RankHelper.calcRank(file.getName()));
        dto.setType("FILE:" + FileHelper.getFileExtension(file.getName()).name());
        try {
            dto.setHash(MD5Helper.calculateMD5Buffered(file.getAbsolutePath()));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        dto.setModifiedAt(file.lastModified());
        return dto;
    }

}
