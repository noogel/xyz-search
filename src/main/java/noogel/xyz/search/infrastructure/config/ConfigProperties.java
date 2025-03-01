package noogel.xyz.search.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.consts.CommonsConsts;
import noogel.xyz.search.infrastructure.utils.ConfigNote;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Slf4j
public class ConfigProperties {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CONFIG_PROPERTIES_FILE_NAME = "properties-config.json";
    private static final String DB_FILE_NAME = "search.xyz";

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 基本配置，不可直接修改
     */
    private Base base;
    /**
     * 运行时数据，不可直接修改
     */
    private Runtime runtime;
    /**
     * 应用数据，用户可直接修改
     */
    private App app;

    /**
     * 保存到文件
     */
    public void overrideToFile() {
        getBase().configFilePath().toFile().mkdirs();
        getBase().indexerFilePath().toFile().mkdirs();
        getBase().vectoryFilePath().toFile().mkdirs();
        getBase().tmpFilePath().toFile().mkdirs();

        String pCfgPath = getBase().propertiesConfigPath().toString();
        File pCfgFile = new File(pCfgPath);
        try {
            ConfigProperties propertyConfig = new ConfigProperties();
            BeanUtils.copyProperties(this, propertyConfig);
            OBJECT_MAPPER.writeValue(pCfgFile, propertyConfig);
        } catch (IOException e) {
            log.error("writeConfigErr path: {}", pCfgPath, e);
        }
    }

    @Data
    public static class CollectItem {
        @ConfigNote(desc = "collectDirectories:资源收集来源目录")
        private List<String> fromList;
        @ConfigNote(desc = "collectDirectories:资源收集到的目录")
        private String to;
        @ConfigNote(desc = "collectDirectories:资源收集过滤规则（正则）（可选）")
        private String filterRegex;
        @ConfigNote(desc = "collectDirectories:资源收集后自动删除文件")
        private Boolean autoDelete;
    }

    @Data
    public static class NotifyEmail {
        @ConfigNote(desc = "senderEmail:邮件发送方")
        private String senderEmail;
        @ConfigNote(desc = "emailHost:邮件地址")
        private String emailHost;
        @ConfigNote(desc = "emailPort:邮件端口")
        private Integer emailPort;
        @ConfigNote(desc = "emailPass:邮件密码")
        private String emailPass;
        @ConfigNote(desc = "notifyEmail:邮件接收人")
        private List<String> receivers;
    }

    @Data
    public static class PaddleOcr {
        @ConfigNote(desc = "paddleOcr:OCR服务地址")
        private String url;
        @ConfigNote(desc = "paddleOcr:超时时间(毫秒)")
        private Integer timeout;
    }

    @Data
    public static class IndexItem {
        @ConfigNote(desc = "目录")
        private String directory;
        @ConfigNote(desc = "排除目录")
        private List<String> excludesDirectories;
        @ConfigNote(desc = "排除的文件处理类")
        private List<String> excludeFileProcessClass;
    }

    @Data
    public static class LinkItem {
        @ConfigNote(desc = "描述")
        private String desc;
        @ConfigNote(desc = "外链地址")
        private String searchUrl;
    }

    @Data
    public static class Chat {
        @ConfigNote(desc = "是否开启")
        private boolean enable;
        @ConfigNote(desc = "ollama")
        private Ollama ollama;
    }

    @Data
    public static class Ollama {
        /**
         * #spring.ai.ollama.base-url=http://localhost:11434
         * #spring.ai.ollama.chat.model=deepseek-r1:1.5b
         * spring.ai.ollama.chat.options.temperature=0.7
         * spring.ai.ollama.chat.options.num-predict=10000
         * spring.ai.ollama.init.embedding.additional-models=jinaai/jina-embeddings-v2-base-zh
         * spring.ai.ollama.init.pull-model-strategy=never
         */
        private String baseUrl;
        private String chatModel;
        private String chatOptionNumCtx;
        private String chatOptionTemperature;
        private String chatOptionNumPredict;
        private String pullModelStrategy;
    }

    @Data
    public static class App {
        //目录配置
        @ConfigNote(desc = "索引目录")
        private List<IndexItem> indexDirectories;
        @ConfigNote(desc = "资源收集目录映射")
        private List<CollectItem> collectDirectories;
        @ConfigNote(desc = "OPDS 资源目录，如果存在则开启")
        private String opdsDirectory;
        @ConfigNote(desc = "上传文件所在目录")
        private String uploadFileDirectory;
        @ConfigNote(desc = "标记删除文件转移到的目录")
        private String markDeleteDirectory;
        @ConfigNote(desc = "访问邮件通知")
        private NotifyEmail notifyEmail;
        @ConfigNote(desc = "外部 OCR 服务")
        private PaddleOcr paddleOcr;
        @ConfigNote(desc = "外链配置")
        private List<LinkItem> linkItems;
        @ConfigNote(desc = "Chat 配置")
        private Chat chat;

        public static App init() {
            App app = new App();
            app.setIndexDirectories(new ArrayList<>());
            app.setNotifyEmail(new NotifyEmail());
            app.getNotifyEmail().setReceivers(new ArrayList<>());
            return app;
        }

        public static List<Pair<String, String>> getNotes() {
            return Arrays.stream(App.class.getDeclaredFields()).map(t -> {
                String name = "[" + t.getType().getSimpleName() + "] " + t.getName();
                ConfigNote annotation = t.getAnnotation(ConfigNote.class);
                return Pair.of(name, annotation.desc());
            }).collect(Collectors.toList());
        }

        @JsonIgnore
        public List<String> indexDirectories() {
            return indexDirectories.stream().map(IndexItem::getDirectory).distinct().toList();
        }

        @JsonIgnore
        public List<String> excludesDirectories() {
            return indexDirectories.stream().map(IndexItem::getExcludesDirectories)
                    .filter(Objects::nonNull).flatMap(List::stream)
                    .filter(StringUtils::isNotBlank).distinct().toList();
        }

        /**
         * 是否开启了 ocr 服务
         *
         * @return
         */
        @JsonIgnore
        public boolean supportPaddleOcr() {
            return Objects.nonNull(paddleOcr) && StringUtils.isNotBlank(paddleOcr.url);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Runtime {
        /**
         * 是否已经初始化索引
         */
        private Long defaultSleepMs;

        public static Runtime init() {
            Runtime rt = new Runtime();
            rt.setDefaultSleepMs(CommonsConsts.DEFAULT_SLEEP_MS);
            return rt;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Base {
        /**
         * 配置文件路径
         */
        private String dataPath;
        /**
         * 配置文件目录
         */
        private String configSubPath;
        /**
         * 索引数据子目录
         */
        private String indexerSubPath;
        /**
         * 向量库子目录
         */
        private String vectorSubPath;
        /**
         * 临时数据子目录
         */
        private String tmpSubPath;
        /**
         * 索引名称
         */
        private String ftsIndexName;
        /**
         * 用户名
         */
        private String username;
        /**
         * 密码
         */
        private String password;

        public Path configFilePath() {
            return Paths.get(dataPath).resolve(Paths.get(configSubPath));
        }

        public Path indexerFilePath() {
            return Paths.get(dataPath).resolve(Paths.get(indexerSubPath));
        }

        public Path vectoryFilePath() {
            return Paths.get(dataPath).resolve(Paths.get(vectorSubPath));
        }

        public Path tmpFilePath() {
            return Paths.get(dataPath).resolve(Paths.get(tmpSubPath));
        }

        /**
         * 配置文件绝对路径
         *
         * @return
         */
        public Path propertiesConfigPath() {
            return configFilePath().resolve(CONFIG_PROPERTIES_FILE_NAME);
        }

        /**
         * 配置文件绝对路径
         *
         * @return
         */
        public Path dbPath() {
            return Paths.get(dataPath).resolve(DB_FILE_NAME);
        }

    }
}
