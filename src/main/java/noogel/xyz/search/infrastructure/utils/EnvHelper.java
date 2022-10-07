package noogel.xyz.search.infrastructure.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class EnvHelper {
    public static final String DEPLOY_ENV = Optional.ofNullable(System.getenv("DEPLOY_ENV"))
            .filter(StringUtils::isNoneBlank).orElse("dev").toLowerCase();
}
