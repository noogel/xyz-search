package noogel.xyz.search.service.extension;

import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.consts.FileProcessClassEnum;

import java.util.Set;

public interface ExtensionPointService extends ExtensionParserService {

    FileProcessClassEnum getFileClass();

    /**
     * 获取支持的文件扩展
     *
     * @return
     */
    Set<FileExtEnum> getSupportParseFileExtension();

    /**
     * 是否支持文件处理
     *
     * @param filePath
     * @return
     */
    boolean supportFile(String filePath);

}
