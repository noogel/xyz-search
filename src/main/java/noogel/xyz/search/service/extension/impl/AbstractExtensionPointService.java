package noogel.xyz.search.service.extension.impl;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileFsDto;
import noogel.xyz.search.infrastructure.utils.MD5Helper;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class AbstractExtensionPointService {

    protected FileFsDto genFileFsDto(@Nonnull File file, @Nonnull List<ChapterDto> chapters, @Nullable String title) {
        FileFsDto dto = new FileFsDto();
        dto.setOptions(new FileFsDto.OptionsDto());
        dto.setDir(file.getParent());
        dto.setName(file.getName());
        dto.setSize(file.length());
        try {
            dto.setHash(MD5Helper.getMD5(file));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        dto.setModifiedAt(file.lastModified());
        dto.setChapterList(chapters);
        Optional.ofNullable(title).filter(StringUtils::isNotBlank).ifPresent(dto.getOptions()::setTitle);
        return dto;
    }

}
