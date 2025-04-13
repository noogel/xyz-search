package noogel.xyz.search.service;

import noogel.xyz.search.infrastructure.dto.SearchSettingDto;

public interface SettingService {

    /**
     * 获取最新的搜索配置
     *
     * @return
     */
    SearchSettingDto query();

    /**
     * 更新配置
     *
     * @param cfg
     * @return
     */
    SearchSettingDto update(SearchSettingDto cfg);

    /**
     * 测试链接
     *
     * @param cfg
     * @return
     */
    boolean connectTesting(SearchSettingDto cfg);
}
