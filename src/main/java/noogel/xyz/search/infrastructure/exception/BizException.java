package noogel.xyz.search.infrastructure.exception;

import lombok.Getter;

public class BizException extends RuntimeException{

    @Getter
    private final int code;

    public BizException(String msg, int code, Throwable cause) {
        super(msg, cause);
        this.code = code;
    }
}
