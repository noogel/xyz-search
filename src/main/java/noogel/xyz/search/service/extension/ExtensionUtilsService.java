package noogel.xyz.search.service.extension;

import noogel.xyz.search.infrastructure.model.ResourceModel;

import java.io.File;
import java.util.Set;

public interface ExtensionUtilsService {

    /**
     * 检查是否存在
     * @param resPath
     * @param resSize
     * @param modifiedAt
     * @return
     */
    ResourceModel findExistResource(String resPath, Long resSize, Long modifiedAt);

    /**
     * 是否支持的扩展
     * @param supportExtension
     * @param file
     * @return
     */
    boolean supportFileExtension(Set<String> supportExtension, File file);
}
