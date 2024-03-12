package noogel.xyz.search.service.extension;

import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;

import javax.annotation.Nullable;

public interface ExtensionPointService {

    /**
     * 是否支持文件处理
     *
     * @param filePath
     * @return
     */
    boolean supportFile(String filePath);

    /**
     * 解析文件
     *
     * @param file
     * @return
     */
    @Nullable
    FileResContentDto parseFile(FileResReadDto file);
}
