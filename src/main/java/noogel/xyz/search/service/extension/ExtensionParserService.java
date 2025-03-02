package noogel.xyz.search.service.extension;

import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;

import jakarta.annotation.Nullable;

public interface ExtensionParserService {

    /**
     * 解析文件
     *
     * @param file
     * @return
     */
    @Nullable
    FileResContentDto parseFile(FileResReadDto file);
}
