package noogel.xyz.search.application.scheduler;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.CommonsConsts;
import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.service.FileDbService;
import noogel.xyz.search.service.FileProcessService;
import noogel.xyz.search.service.SynchronizeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CollectServiceScheduler {

    @Resource
    private SynchronizeService synchronizeService;
    @Resource
    private FileDbService fileDbService;
    @Resource
    private FileProcessService fileProcessService;


    /**
     * diff 一遍文件
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void asyncScanFsFiles() {
        synchronizeService.asyncDirectories();
    }

    /**
     * 处理异常记录
     */
    @Scheduled(cron = "0 30 0 * * *")
    public void asyncCleanDbErrorFiles() {
        for (int i = 0; i < 1000; i++) {
            List<FileResReadDto> errorRecords = fileDbService.scanFileResByState(FileStateEnum.ERROR);
            if (errorRecords.isEmpty()) {
                break;
            }
            log.info("asyncCleanDbErrorFiles {}", errorRecords.size());
            // 恢复记录状态
            errorRecords.forEach(t -> fileDbService.updateFileState(t.getFieldId(), FileStateEnum.VALID));
        }
    }

    /**
     * 转移收集的文件
     */
    @Scheduled(cron = "0 0 0,11,18 * * *")
    public void asyncCollectFileIfNotExist() {
        CommonsConsts.SHORT_EXECUTOR_SERVICE.submit(fileProcessService::syncCollectFileIfNotExist);
    }

}
