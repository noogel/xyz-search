package noogel.xyz.search.service.extension.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.ResRelationInfoDto;
import noogel.xyz.search.infrastructure.dto.TaskDto;
import noogel.xyz.search.infrastructure.model.ResourceModel;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.service.extension.ExtensionPointService;
import noogel.xyz.search.service.extension.ExtensionUtilsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class OTHERExtensionPointServiceImpl implements ExtensionPointService {

    private static final Set<String> SUPPORT = Set.of("mobi", "azw3", "azw", "mp4");

    @Resource
    private ExtensionUtilsService extensionUtilsService;

    @Override
    public boolean supportFile(File file) {
        boolean supportFile = extensionUtilsService.supportFileExtension(SUPPORT, file);
        if (supportFile) {
            String fileExtension = FileHelper.getFileExtension(file.getAbsolutePath());
            if ("mp4".equals(fileExtension)) {
                // 小于 10M 的是视频不索引，扩展点
                if (file.length() < 1024 * 1024 * 10) {
                    supportFile = false;
                }
            }
        }
        return supportFile;
    }

    @Nullable
    @Override
    public ResourceModel parseFile(File file, TaskDto task) {
        ResRelationInfoDto resRel = extensionUtilsService.autoFindRelationInfo(file);
        String title = Optional.ofNullable(resRel).map(ResRelationInfoDto::getTitle).orElse(null);
        String text = file.getName();
        if (StringUtils.isNotBlank(title)) {
            text = title;
        }
        return ResourceModel.buildBaseInfo(file, text, title, task);
    }
}
