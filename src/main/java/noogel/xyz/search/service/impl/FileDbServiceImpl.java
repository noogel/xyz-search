package noogel.xyz.search.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dao.sqlite.DirectoryDao;
import noogel.xyz.search.infrastructure.dao.sqlite.FileResDao;
import noogel.xyz.search.infrastructure.dto.dao.DirectoryDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResWriteDto;
import noogel.xyz.search.infrastructure.dto.dao.FileViewDto;
import noogel.xyz.search.infrastructure.model.sqlite.DirectoryModel;
import noogel.xyz.search.infrastructure.model.sqlite.FileResModel;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import noogel.xyz.search.service.FileDbService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class FileDbServiceImpl implements FileDbService {

    @Resource
    private FileResDao fileResDao;
    @Resource
    private DirectoryDao directoryDao;

    @Override
    public List<FileViewDto> listFiles(File rootDir) {
        DirectoryDto directoryDto = DirectoryDto.of(rootDir.getAbsolutePath());

        List<FileViewDto> resp = new ArrayList<>();

        // 查询子目录

        List<String> subDirs = directoryDao
                .findByPathStartsWithAndDep(directoryDto.getPath() + "/", directoryDto.calDirDep() + 1)
                .stream().map(DirectoryModel::getPath).toList();

        for (String subDir : subDirs) {
            FileViewDto dbDto = FileViewDto.of(subDir, true, null, null, null);
            resp.add(dbDto);
        }

        // 查询当前目录的文件
        List<FileResModel> subFiles = fileResDao.batchFindByDir(directoryDto.getPath());

        for (FileResModel subFile : subFiles) {
            String path = subFile.getDir() + "/" + subFile.getName();
            FileViewDto dbDto = FileViewDto.of(path, false, subFile.getId(), subFile.getSize(), subFile.getModifiedAt());
            resp.add(dbDto);
        }

        return resp;
    }

    @Override
    public Long appendFile(FileResWriteDto writeDto) {
        synchronized (this) {
            FileResModel fileResModel = new FileResModel();
            fileResModel.setResId(writeDto.getResId());
            fileResModel.setDir(writeDto.getDir());
            fileResModel.setDirDep(writeDto.calDirDep());
            fileResModel.setName(writeDto.getName());
            fileResModel.setSize(writeDto.getSize());
            fileResModel.setModifiedAt(writeDto.getModifiedAt());
            fileResModel.setType(writeDto.getType());
            fileResModel.setHash(writeDto.getHash());
            fileResModel.setRank(writeDto.getRank());
            fileResModel.setOptions(JsonHelper.toJson(writeDto.getOptions()));
            fileResModel.setState(FileStateEnum.VALID.getVal());
            fileResDao.save(fileResModel);
            return fileResModel.getId();
        }
    }

    @Override
    public void updateFileState(Long fieldId, FileStateEnum stateEnum) {
        synchronized (this) {
            fileResDao.updateStateById(stateEnum.getVal(), fieldId);
        }
    }

    @Override
    public void updateFileState(Long fieldId, FileStateEnum stateEnum, Map<String, String> appendOptions) {
        synchronized (this) {
            String options = JsonHelper.toJson(appendOptions);
            if (StringUtils.isBlank(options)) {
                fileResDao.updateStateById(stateEnum.getVal(), fieldId);
            } else {
                fileResDao.updateStateAndOptionsById(stateEnum.getVal(), options, fieldId);
            }
        }
    }

    @Override
    public void deleteFile(Long fieldId) {
        synchronized (this) {
            fileResDao.deleteById(fieldId);
        }
    }

    @Override
    public int updateDirectoryState(String path, FileStateEnum stateEnum) {
        synchronized (this) {
            // 删除目录
            directoryDao.deleteByPath(path);
            directoryDao.deleteByPathStartsWith(path + "/");
            // 标记清理文件
            return fileResDao.updateStateByDirStartsWith(stateEnum.getVal(), path);
        }
    }

    @Override
    public Optional<FileViewDto> findFirstByHash(String hash) {
        return fileResDao.findFirstByHash(hash).map(t -> {
            String path = t.getDir() + "/" + t.getName();
            return FileViewDto.of(path, false, t.getId(), t.getSize(), t.getModifiedAt());
        });
    }

    @Override
    public Optional<FileResReadDto> findByIdFilterState(Long id, FileStateEnum state) {
        Optional<FileResModel> fileRes = fileResDao.findById(id);
        if (fileRes.isEmpty()) {
            return Optional.empty();
        }
        if (state.getVal() != fileRes.get().getState()) {
            return Optional.empty();
        }
        FileResReadDto dto = new FileResReadDto();
        fileRes.ifPresent(t -> {
            dto.setFieldId(t.getId());
            dto.setState(t.getState());
            dto.setResId(t.getResId());
            dto.setDir(t.getDir());
            dto.setName(t.getName());
            dto.setSize(t.getSize());
            dto.setModifiedAt(t.getModifiedAt());
            dto.setType(t.getType());
            dto.setHash(t.getHash());
            dto.setRank(t.getRank());
            dto.setOptions(JsonHelper.fromJson(t.getOptions()));
        });
        return Optional.of(dto);
    }

    @Override
    public List<Long> scanFileResByState(FileStateEnum state) {
        List<Long> list = fileResDao.findTop12ByState(state.getVal()).stream().map(FileResModel::getId).toList();
        return list;
    }

    @Override
    public void upsertPath(String path) {
        synchronized (this) {
            if (directoryDao.findByPath(path).isEmpty()) {
                DirectoryDto dto = DirectoryDto.of(path);
                DirectoryModel model = new DirectoryModel();
                model.setPath(dto.getPath());
                model.setDep(dto.calDirDep());
                directoryDao.save(model);
            }
        }
    }

}
