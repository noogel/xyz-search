package noogel.xyz.search.service.extension.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.TaskDto;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileFsDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.model.ResourceModel;
import noogel.xyz.search.service.extension.ExtensionPointService;
import noogel.xyz.search.service.extension.ExtensionUtilsService;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

@Service
@Slf4j
public class HTMLExtensionPointServiceImpl extends AbstractExtensionPointService implements ExtensionPointService {

    private static final Set<String> SUPPORT = Set.of("html", "xhtml", "htm");

    @Resource
    private ExtensionUtilsService extensionUtilsService;

    @Override
    public boolean supportFile(File file) {
        return extensionUtilsService.supportFileExtension(SUPPORT, file);
    }

    @Nullable
    @Override
    public ResourceModel parseFile(File file, TaskDto task) {
        String text = "";
        try {
            text = Jsoup.parse(file).text();
        } catch (IOException e) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(e);
        }
        return ResourceModel.buildBaseInfo(file, text, task);
    }

    @Nullable
    @Override
    public FileFsDto parseFile(File file) {
        String text = "";
        try {
            text = Jsoup.parse(file).text();
        } catch (IOException e) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(e);
        }
        return genFileFsDto(file, Collections.singletonList(ChapterDto.of("", text)), null);
    }
}
