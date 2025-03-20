package noogel.xyz.search.application.listener;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.event.ConfigAppUpdateEvent;
import noogel.xyz.search.service.SynchronizeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class DirectoryUpdateListener {

    @Resource
    private SynchronizeService synchronizeService;

    /**
     * 监听目录变化
     *
     * @param event
     */
    @EventListener(ConfigAppUpdateEvent.class)
    public void configAppUpdate(ConfigAppUpdateEvent event) {
        ConfigProperties.App newApp = event.getNewApp();
        ConfigProperties.App oldApp = event.getOldApp();

        // 计算并更新目录
        List<ConfigProperties.IndexItem> oldDirList = new ArrayList<>(oldApp.getIndexDirectories());
        // 新增目录
        List<ConfigProperties.IndexItem> newDirList = new ArrayList<>(newApp.getIndexDirectories());
        // 把新目录移除 = 剩下旧目录
        oldDirList.removeAll(newApp.getIndexDirectories());
        // 把旧目录移除 = 剩下新增的目录
        newDirList.removeAll(oldApp.getIndexDirectories());
        // 如果有变化则更新
        if (!CollectionUtils.isEmpty(oldDirList) || !CollectionUtils.isEmpty(newDirList)) {
            // 同步新目录
            synchronizeService.asyncDirectories(newDirList, oldDirList);
        }

        // 遍历目录配置，对于不存在的目录创建
        List<String> willCheckDirectories = new ArrayList<>(newApp.excludesDirectories());
        Optional.ofNullable(newApp.getUploadFileDirectory()).ifPresent(willCheckDirectories::add);
        Optional.ofNullable(newApp.getMarkDeleteDirectory()).ifPresent(willCheckDirectories::add);
        newApp.getCollectDirectories().forEach(l -> willCheckDirectories.add(l.getTo()));
        // 创建目录
        for (String willCheckDirectory : willCheckDirectories) {
            if (StringUtils.isNotEmpty(willCheckDirectory)) {
                File file = new File(willCheckDirectory);
                if (!file.exists()) {
                    file.mkdirs();
                    log.info("创建不存在的目录：{}", file.getAbsolutePath());
                }
            }
        }
    }

}
