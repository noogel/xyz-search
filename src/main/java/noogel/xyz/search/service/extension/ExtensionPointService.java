package noogel.xyz.search.service.extension;

import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;

import javax.annotation.Nullable;
import java.io.File;

public interface ExtensionPointService {

    /**
     * 是否支持文件处理
     *
     * @param file
     * @return
     */
    boolean supportFile(File file);

    /**
     * 解析文件
     *
     * @param file
     * @return
     */
    @Nullable
    FileResContentDto parseFile(FileResReadDto file);
}
