package noogel.xyz.search.service.extension.impl;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.consts.FileProcessClassEnum;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

@Service
@Slf4j
public class HTMLExtensionPointServiceImpl extends AbstractExtensionPointService {

    @Getter
    private final FileProcessClassEnum fileClass = FileProcessClassEnum.HTML;
    @Getter
    private final Set<FileExtEnum> supportParseFileExtension = Set.of(
            FileExtEnum.HTML, FileExtEnum.XHTML, FileExtEnum.HTM
    );

    @Nullable
    @Override
    public FileResContentDto parseFile(FileResReadDto resReadDto) {
        File file = resReadDto.genFile();
        String text = "";
        try {
            text = Jsoup.parse(file).text();
        } catch (IOException e) {
            throw ExceptionCode.FILE_ACCESS_ERROR.throwExc(e);
        }
        return FileResContentDto.of(Collections.singletonList(ChapterDto.of("", text)));
    }
}
