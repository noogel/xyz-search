package noogel.xyz.search.service.extension.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.consts.FileProcessClassEnum;
import noogel.xyz.search.infrastructure.dto.ResRelationInfoDto;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class OTHERExtensionPointServiceImpl extends AbstractExtensionPointService {
    @Getter
    private final FileProcessClassEnum fileClass = FileProcessClassEnum.OTHER;

    @Getter
    private final Set<FileExtEnum> supportParseFileExtension = Set.of(
            FileExtEnum.MOBI, FileExtEnum.AZW3, FileExtEnum.AZW, FileExtEnum.MP4, FileExtEnum.MKV, FileExtEnum.AVI
    );

    @Override
    public boolean supportFile(String filePath) {
        boolean supportFile = super.supportFile(filePath);
        if (supportFile) {
            FileExtEnum fileExtension = FileHelper.getFileExtension(filePath);
            if (FileExtEnum.MP4.equals(fileExtension)) {
                // 小于 10M 的是视频不索引，扩展点
                if (new File(filePath).length() < 1024 * 1024 * 10) {
                    supportFile = false;
                }
            }
        }
        return supportFile;
    }

    @Nullable
    @Override
    public FileResContentDto parseFile(FileResReadDto resReadDto) {
        File file = resReadDto.genFile();
        ResRelationInfoDto resRel = autoFindRelationInfo(file);
        String title = Optional.ofNullable(resRel).map(ResRelationInfoDto::getTitle).orElse(null);
        String text = file.getName();
        if (StringUtils.isNotBlank(title)) {
            text = title;
        }
        return FileResContentDto.of(Collections.singletonList(ChapterDto.of("", text))).metaData("metaTitle", title);
    }
}
