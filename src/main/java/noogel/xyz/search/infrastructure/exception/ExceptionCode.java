package noogel.xyz.search.infrastructure.exception;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ExceptionCode {
    FILE_ACCESS_ERROR("读写错误：%s", 1001),
    CONFIG_ERROR("配置错误：%s", 1002),
    PARAM_ERROR("参数错误：%s", 1003),
    RUNTIME_ERROR("执行错误：%s", 1004),
    ;

    private final String msg;
    private final int code;

    public BizException throwExc(Throwable cause) {
        return new BizException(String.format(msg, cause.getMessage()), code, cause);
    }

    public BizException throwExc(String causeMsg) {
        return new BizException(String.format(msg, causeMsg), code, null);
    }

    public void throwOn(boolean condition, String causeMsg) {
        if (condition) {
            throw new BizException(String.format(msg, causeMsg), code, null);
        }
    }

}
