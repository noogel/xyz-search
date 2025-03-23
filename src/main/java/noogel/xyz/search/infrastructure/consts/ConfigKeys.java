package noogel.xyz.search.infrastructure.consts;

public class ConfigKeys {
    
    // 邮件配置
    public static final String EMAIL_HOST = "email.host";
    public static final String EMAIL_PORT = "email.port";
    public static final String EMAIL_USERNAME = "email.username";
    public static final String EMAIL_PASSWORD = "email.password";
    public static final String EMAIL_FROM = "email.from";
    public static final String EMAIL_TO = "email.to";
    
    // 系统配置
    public static final String SYSTEM_NAME = "system.name";
    public static final String SYSTEM_VERSION = "system.version";
    public static final String SYSTEM_DESCRIPTION = "system.description";
    
    // 其他配置
    public static final String MAX_UPLOAD_SIZE = "max.upload.size";
    public static final String ALLOWED_FILE_TYPES = "allowed.file.types";
    
    private ConfigKeys() {
        // 私有构造函数，防止实例化
    }
} 