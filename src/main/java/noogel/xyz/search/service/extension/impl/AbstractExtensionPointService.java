package noogel.xyz.search.service.extension.impl;


import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.dto.OPDSResMetaDataDto;
import noogel.xyz.search.infrastructure.dto.ResRelationInfoDto;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.OPDSHelper;
import noogel.xyz.search.service.extension.ExtensionPointService;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public abstract class AbstractExtensionPointService implements ExtensionPointService {

    @Nullable
    protected static ResRelationInfoDto autoFindRelationInfo(File file) {
        ResRelationInfoDto dto = new ResRelationInfoDto();
        String parent = file.getParent();
        OPDSResMetaDataDto metaData = OPDSHelper.readMetaData(parent);
        if (Objects.nonNull(metaData)) {
            StringBuilder resTitle = new StringBuilder();
            Optional.ofNullable(metaData.getTitle()).ifPresent(resTitle::append);
            Optional.ofNullable(metaData.getCreator()).ifPresent(t -> {
                if (CollectionUtils.isEmpty(t)) {
                    return;
                }
                if (resTitle.length() > 0) {
                    resTitle.append(" - ");
                }
                resTitle.append(String.join(",", t));
            });
            if (resTitle.length() > 0) {
                resTitle.append(".").append(FileHelper.getFileExtension(file.getName()));
                dto.setTitle(resTitle.toString());
            }
            Optional.of(metaData.getSource()).map(Object::toString).ifPresent(l -> {
                dto.setMetaContent(l);
                log.debug("autoFindRelationInfo parse metadata.opf {} {}", file.getAbsoluteFile(), l);
            });
            return dto;
        }
        return null;
    }

    public boolean supportFile(String filePath) {
        FileExtEnum fileExtension = FileHelper.getFileExtension(filePath);
        return this.getSupportParseFileExtension().contains(fileExtension);
    }

}
