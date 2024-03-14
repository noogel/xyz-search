package noogel.xyz.search.service.extension.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.ResRelationInfoDto;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.service.extension.ExtensionPointService;
import noogel.xyz.search.service.extension.ExtensionUtilsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class OTHERExtensionPointServiceImpl implements ExtensionPointService {

    private static final Set<String> SUPPORT = Set.of("mobi", "azw3", "azw", "mp4", "mkv", "avi");

    @Resource
    private ExtensionUtilsService extensionUtilsService;

    @Override
    public boolean supportFile(String filePath) {
        boolean supportFile = extensionUtilsService.supportFileExtension(SUPPORT, filePath);
        if (supportFile) {
            String fileExtension = FileHelper.getFileExtension(filePath);
            if ("mp4".equals(fileExtension)) {
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
        ResRelationInfoDto resRel = extensionUtilsService.autoFindRelationInfo(file);
        String title = Optional.ofNullable(resRel).map(ResRelationInfoDto::getTitle).orElse(null);
        String text = file.getName();
        if (StringUtils.isNotBlank(title)) {
            text = title;
        }
        return FileResContentDto.of(Collections.singletonList(ChapterDto.of("", text)), title);
    }
}
