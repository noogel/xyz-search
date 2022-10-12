package noogel.xyz.search.service.extension;

import java.io.File;
import java.util.Set;

public interface ExtensionUtilsService {

    /**
     * 是否支持的扩展
     * @param supportExtension
     * @param file
     * @return
     */
    boolean supportFileExtension(Set<String> supportExtension, File file);
}
