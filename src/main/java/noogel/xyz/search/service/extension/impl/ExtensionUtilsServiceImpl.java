package noogel.xyz.search.service.extension.impl;

import noogel.xyz.search.infrastructure.dao.ElasticSearchFtsDao;
import noogel.xyz.search.infrastructure.model.ResourceModel;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.service.extension.ExtensionUtilsService;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.io.File;
import java.util.Set;

@Service
public class ExtensionUtilsServiceImpl implements ExtensionUtilsService {

    @Resource
    private ElasticSearchFtsDao dao;

    @Nullable
    @Override
    public ResourceModel findExistResource(String resPath, Long resSize, Long modifiedAt) {
        return null;
    }

    @Override
    public boolean supportFileExtension(Set<String> supportExtension, File file) {
        if (file.isDirectory()) {
            return false;
        }
        String fileExtension = FileHelper.getFileExtension(file.getAbsolutePath());
        return supportExtension.contains(fileExtension);
    }
}
