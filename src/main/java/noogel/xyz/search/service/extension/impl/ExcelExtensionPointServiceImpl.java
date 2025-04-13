package noogel.xyz.search.service.extension.impl;

import java.util.Collections;
import java.util.Set;

import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.consts.FileProcessClassEnum;
import noogel.xyz.search.infrastructure.dto.dao.ChapterDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.utils.ExcelParser;
import noogel.xyz.search.infrastructure.utils.JsonHelper;

@Service
@Slf4j
public class ExcelExtensionPointServiceImpl extends AbstractExtensionPointService {

    @Getter
    private final FileProcessClassEnum fileClass = FileProcessClassEnum.OFFICE;

    @Getter
    private final Set<FileExtEnum> supportParseFileExtension = Set.of(
            FileExtEnum.XLS, FileExtEnum.XLSX);

    @Nullable
    @Override
    public FileResContentDto parseFile(FileResReadDto resReadDto) {
        var result = ExcelParser.parseExcel(
                resReadDto.genFile().getAbsolutePath(), true);
        return FileResContentDto.of(Collections.singletonList(ChapterDto.of("", JsonHelper.toJson(result))));
    }
}
