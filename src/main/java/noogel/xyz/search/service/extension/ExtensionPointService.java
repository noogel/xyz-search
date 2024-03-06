package noogel.xyz.search.service.extension;

import noogel.xyz.search.infrastructure.dto.TaskDto;
import noogel.xyz.search.infrastructure.dto.dao.FileFsDto;
import noogel.xyz.search.infrastructure.model.ResourceModel;

import javax.annotation.Nullable;
import java.io.File;

public interface ExtensionPointService {

    /**
     * 是否支持文件处理
     * @param file
     * @return
     */
    boolean supportFile(File file);

    /**
     * 解析文件
     * @param file
     * @return
     */
    @Deprecated
    ResourceModel parseFile(File file, TaskDto taskDto);

    /**
     * 解析文件
     * @param file
     * @return
     */
    @Nullable
    FileFsDto parseFile(File file);
}
