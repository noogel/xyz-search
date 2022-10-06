package noogel.xyz.search.service;

import java.util.Map;

public interface SettingService {

    /**
     * 更新配置
     * @param cfg
     * @return
     */
    boolean update(Map<String, String> cfg);
}
