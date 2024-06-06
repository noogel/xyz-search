package noogel.xyz.search.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileProcessService {
    /**
     * 上传文件
     *
     * @param file
     */
    void uploadFile(MultipartFile file, String fileDirectory);

    /**
     * 收集文件
     */
    void syncCollectFileIfNotExist();

    /**
     * 标记删除文件
     *
     * @param resId
     */
    void fileMarkDelete(String resId);
}
