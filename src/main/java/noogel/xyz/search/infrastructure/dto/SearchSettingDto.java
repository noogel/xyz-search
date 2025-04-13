package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@Data
public class SearchSettingDto {
    /**
     * 账户名不可更新
     */
    private String username;
    private String password;
    private String appConfig;
    private List<Pair<String, String>> configDesc;
}
