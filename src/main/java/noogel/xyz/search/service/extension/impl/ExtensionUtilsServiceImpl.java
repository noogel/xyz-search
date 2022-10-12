package noogel.xyz.search.service.extension.impl;

import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.service.extension.ExtensionUtilsService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Set;

@Service
public class ExtensionUtilsServiceImpl implements ExtensionUtilsService {

    @Override
    public boolean supportFileExtension(Set<String> supportExtension, File file) {
        if (file.isDirectory()) {
            return false;
        }
        String fileExtension = FileHelper.getFileExtension(file.getAbsolutePath());
        return supportExtension.contains(fileExtension);
    }
}
