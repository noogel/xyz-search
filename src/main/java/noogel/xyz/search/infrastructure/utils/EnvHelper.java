package noogel.xyz.search.infrastructure.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EnvHelper {
    public static final String INIT_MODE = Optional.ofNullable(System.getenv("INIT_MODE"))
            .filter(StringUtils::isNoneBlank).orElse("dev").toLowerCase();

    @AllArgsConstructor
    @Getter
    public enum FuncEnv {
        AUTH("是否开启鉴权", "true"),
        FTS_IDX("自定义索引", ""),
        SUB_TITLE("副标题", ""),
        ;

        private final String desc;
        private final String defaultValue;

        /**
         * 获取功能性环境变量配置
         *
         * @return
         */
        public String getEnv() {
            String envName = "FUNC_" + name();
            return Optional.ofNullable(System.getenv(envName)).filter(StringUtils::isNotBlank).orElse(defaultValue);
        }

        /**
         * 获取所有子参数
         *
         * @return
         */
        public Map<FuncEnv, String> getSubEnv() {
            return Arrays.stream(values()).filter(t -> t.name().startsWith(name() + "_"))
                    .collect(Collectors.toMap(t -> t, FuncEnv::getEnv));
        }

    }
}
