package noogel.xyz.search.service;

import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dto.dao.FileDbDto;
import noogel.xyz.search.infrastructure.dto.dao.FileFsDto;

import java.io.File;
import java.util.List;
import java.util.Optional;

public interface FileDbService {

    /**
     * 列出目录下文件或目录
     * @param rootDir
     * @return
     */
    List<FileDbDto> listFiles(File rootDir);

    /**
     * 追加文件
     * @param fsDto
     */
    void appendFile(FileFsDto fsDto);

    /**
     * 更新文件状态
     * @param fsDto
     * @param stateEnum
     */
    void updateFileState(FileDbDto fsDto, FileStateEnum stateEnum);

    /**
     * 更新目录状态
     * @param fsDto
     * @param stateEnum
     */
    void updateDirectoryState(FileDbDto fsDto, FileStateEnum stateEnum);

    /**
     * 根据文件 hash 查询
     * @param hash
     * @return
     */
    Optional<FileDbDto> findFirstByHash(String hash);
}
