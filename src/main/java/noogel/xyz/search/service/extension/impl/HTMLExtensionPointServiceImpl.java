package noogel.xyz.search.service.extension.impl;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.TaskDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.model.ResourceModel;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.MD5Helper;
import noogel.xyz.search.service.extension.ExtensionPointService;
import noogel.xyz.search.service.extension.ExtensionUtilsService;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.io.*;
import java.util.Set;

@Service
@Slf4j
public class HTMLExtensionPointServiceImpl implements ExtensionPointService {

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
}
