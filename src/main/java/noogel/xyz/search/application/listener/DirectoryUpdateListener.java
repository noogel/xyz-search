package noogel.xyz.search.application.listener;

import jakarta.annotation.Resource;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.event.ConfigAppUpdateEvent;
import noogel.xyz.search.service.SynchronizeService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
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
    }

}
