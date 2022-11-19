package noogel.xyz.search.infrastructure.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class EnvHelper {
    public static final String DEPLOY_ENV = Optional.ofNullable(System.getenv("DEPLOY_ENV"))
            .filter(StringUtils::isNoneBlank).orElse("dev").toLowerCase();
    public static final Integer DEPLOY_VER = Integer.parseInt(Optional.ofNullable(System.getenv("DEPLOY_VER"))
            .filter(StringUtils::isNoneBlank).orElse("1"));
}
