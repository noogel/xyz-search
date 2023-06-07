package noogel.xyz.search.infrastructure.utils;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigNote {
    /**
     * 字段描述
     * @return
     */
    String desc();
}
