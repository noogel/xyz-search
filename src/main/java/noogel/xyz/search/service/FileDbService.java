package noogel.xyz.search.service;

import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResWriteDto;
import noogel.xyz.search.infrastructure.dto.dao.FileViewDto;

import java.io.File;
import java.util.List;
import java.util.Optional;

public interface FileDbService {

    /**
     * 列出目录下文件或目录
     *
     * @param rootDir
     * @return
     */
    List<FileViewDto> listFiles(File rootDir);

    /**
     * 追加文件
     *
     * @param fsDto
     */
    Long appendFile(FileResWriteDto fsDto);

    /**
     * 更新文件状态
     *
     * @param fieldId
     * @param stateEnum
     */
    void updateFileState(Long fieldId, FileStateEnum stateEnum);

    /**
     * 移除文件
     * @param fieldId
     */
    void removeFile(Long fieldId);

    /**
     * 更新目录状态
     *
     * @param path
     * @param stateEnum
     */
    int updateDirectoryState(String path, FileStateEnum stateEnum);

    /**
     * 根据文件 hash 查询
     *
     * @param hash
     * @return
     */
    Optional<FileViewDto> findFirstByHash(String hash);

    /**
     * 根据ID查询完整信息
     *
     * @param id
     * @param state
     * @return
     */
    Optional<FileResReadDto> findByIdFilterState(Long id, FileStateEnum state);

    /**
     * 按状态搜索
     * @param state
     * @return
     */
    List<Long> scanFileResByState(FileStateEnum state);
}
