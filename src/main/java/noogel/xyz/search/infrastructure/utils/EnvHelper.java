package noogel.xyz.search.infrastructure.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EnvHelper {
    public static final String DEPLOY_ENV = Optional.ofNullable(System.getenv("DEPLOY_ENV"))
            .filter(StringUtils::isNoneBlank).orElse("dev").toLowerCase();
    public static final Integer DEPLOY_VER = Integer.parseInt(Optional.ofNullable(System.getenv("DEPLOY_VER"))
            .filter(StringUtils::isNoneBlank).orElse("1"));

    @AllArgsConstructor
    @Getter
    public enum FuncEnv {
        AUTH("是否开启鉴权", "true"),
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
