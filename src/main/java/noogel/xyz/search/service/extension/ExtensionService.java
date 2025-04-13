package noogel.xyz.search.service.extension;

import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.consts.FileProcessClassEnum;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ExtensionService {

    Map<FileProcessClassEnum, Set<FileExtEnum>> fileExtensions();

    /**
     * 寻找处理类
     *
     * @param filePath
     * @return
     */
    Optional<ExtensionParserService> findParser(String filePath);
}
