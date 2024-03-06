package noogel.xyz.search.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dao.sqlite.FileResDao;
import noogel.xyz.search.infrastructure.dao.sqlite.NodeResDao;
import noogel.xyz.search.infrastructure.dto.dao.FileDbDto;
import noogel.xyz.search.infrastructure.dto.dao.FileFsDto;
import noogel.xyz.search.infrastructure.model.sqlite.FileRes;
import noogel.xyz.search.infrastructure.model.sqlite.NodeRes;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import noogel.xyz.search.infrastructure.utils.MD5Helper;
import noogel.xyz.search.infrastructure.utils.RankHelper;
import noogel.xyz.search.service.FileDbService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class FileDbServiceImpl implements FileDbService {

    @Resource
    private FileResDao fileResDao;
    @Resource
    private NodeResDao nodeResDao;

    @Override
    public List<FileDbDto> listFiles(File rootDir) {
        String dir = rootDir.getAbsolutePath();
        int dirDep = dir.split("/").length;

        List<FileDbDto> resp = new ArrayList<>();

        // 查询子目录
        List<String> subDirs = fileResDao.batchFindDistinctDirByDirStartsWithAndDirDep(dir, dirDep + 1);

        for (String subDir : subDirs) {
            FileDbDto dbDto = FileDbDto.of(subDir, true, null, null, null);
            resp.add(dbDto);
        }

        // 查询当前目录的文件
        List<FileRes> subFiles = fileResDao.batchFindByDir(dir);

        for (FileRes subFile : subFiles) {
            String path = subFile.getDir() + "/" + subFile.getName();
            FileDbDto dbDto = FileDbDto.of(path, false, subFile.getId(), subFile.getSize(), subFile.getModifiedAt());
            resp.add(dbDto);
        }

        return resp;
    }

    @Override
    public void appendFile(FileFsDto fsDto) {
        synchronized (this) {
            String title = Optional.ofNullable(fsDto.getOptions())
                    .map(FileFsDto.OptionsDto::getTitle)
                    .filter(StringUtils::isNotBlank)
                    .orElse(fsDto.getName());
            FileRes fileRes = new FileRes();
            fileRes.setDir(fsDto.getDir());
            fileRes.setDirDep(fsDto.calDirDep());
            fileRes.setName(fsDto.getName());
            fileRes.setSize(fsDto.getSize());
            fileRes.setModifiedAt(fsDto.getModifiedAt());
            fileRes.setExt("FILE:" + FileHelper.getFileExtension(fsDto.getName()).toUpperCase());
            fileRes.setHash(fsDto.getHash());
            fileRes.setRank(RankHelper.calcRank(title));
            fileRes.setMeta(JsonHelper.toJson(fsDto.getOptions()));
            fileRes.setState(FileStateEnum.VALID.getVal());
            fileResDao.save(fileRes);
            List<NodeRes> nodeResList = fsDto.getChapterList().stream().map(t -> {
                NodeRes nodeRes = new NodeRes();
                nodeRes.setFileId(fileRes.getId());
                nodeRes.setResId(UUID.randomUUID().toString());
                nodeRes.setChapter(t.getChapter());
                nodeRes.setText(t.getText());
                nodeRes.setHash(MD5Helper.getMD5(t.getText()));
                nodeRes.setSize(t.getText().length());
                return nodeRes;
            }).toList();
            nodeResDao.saveAll(nodeResList);
        }
    }

    @Override
    public void updateFileState(FileDbDto dbDto, FileStateEnum stateEnum) {
        synchronized (this) {
            fileResDao.updateStateById(stateEnum.getVal(), dbDto.getFileId());
        }
    }

    @Override
    public void updateDirectoryState(FileDbDto fsDto, FileStateEnum stateEnum) {
        synchronized (this) {
            fileResDao.updateStateByDirStartsWith(stateEnum.getVal(), fsDto.getPath());
        }
    }

    @Override
    public Optional<FileDbDto> findFirstByHash(String hash) {
        return fileResDao.findFirstByHash(hash).map(t-> {
            String path = t.getDir() + "/" + t.getName();
            return FileDbDto.of(path, false, t.getId(), t.getSize(), t.getModifiedAt());
        });
    }
}
