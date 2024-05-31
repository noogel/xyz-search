package noogel.xyz.search.infrastructure.event;

import lombok.Getter;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

public class ConfigAppUpdateEvent extends ApplicationEvent {

    /**
     * 变更前
     */
    @Getter
    private SearchPropertyConfig.AppConfig oldApp;
    /**
     * 变更后
     */
    @Getter
    private SearchPropertyConfig.AppConfig newApp;

    public ConfigAppUpdateEvent(Object source) {
        super(source);
    }

    public ConfigAppUpdateEvent(Object source, Clock clock) {
        super(source, clock);
    }

    public static ConfigAppUpdateEvent of(Object source,
                                          SearchPropertyConfig.AppConfig oldApp,
                                          SearchPropertyConfig.AppConfig newApp) {
        ConfigAppUpdateEvent event = new ConfigAppUpdateEvent(source);
        event.oldApp = oldApp;
        event.newApp = newApp;
        return event;
    }

}
