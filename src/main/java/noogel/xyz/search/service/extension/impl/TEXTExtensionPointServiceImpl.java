package noogel.xyz.search.service.extension.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.service.extension.ExtensionPointService;
import noogel.xyz.search.service.extension.ExtensionUtilsService;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class TEXTExtensionPointServiceImpl implements ExtensionPointService {

    private static final Set<String> SUPPORT = Set.of("txt", "csv", "md");

    @Resource
    private ExtensionUtilsService extensionUtilsService;

    @Override
    public boolean supportFile(String filePath) {
        return extensionUtilsService.supportFileExtension(SUPPORT, filePath);
    }

    @Nullable
    @Override
    public FileResContentDto parseFile(FileResReadDto resReadDto) {
        File file = resReadDto.genFile();
        Path path = Paths.get(file.toURI());
        StringBuilder text = new StringBuilder();
        try {
            Charset charset = FileHelper.detectCharset(file);
            List<String> allLines = Files.readAllLines(path, charset);
            allLines.forEach(text::append);
        } catch (Exception ex) {
            log.error("TEXTExtensionPointServiceImpl error {}", path, ex);
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(ex);
        }
        return FileResContentDto.of(Collections.singletonList(ChapterDto.of("", text.toString())), null);
    }
}
