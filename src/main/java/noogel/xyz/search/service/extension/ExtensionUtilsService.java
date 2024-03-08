package noogel.xyz.search.service.extension;

import noogel.xyz.search.infrastructure.dto.ResRelationInfoDto;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Set;

public interface ExtensionUtilsService {

    /**
     * 是否支持的扩展
     *
     * @param supportExtension
     * @param file
     * @return
     */
    boolean supportFileExtension(Set<String> supportExtension, File file);

    /**
     * 自动寻找关联信息
     *
     * @param file
     * @return
     */
    @Nullable
    ResRelationInfoDto autoFindRelationInfo(File file);
}
