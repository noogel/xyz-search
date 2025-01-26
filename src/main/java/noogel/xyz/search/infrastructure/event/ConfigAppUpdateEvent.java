package noogel.xyz.search.infrastructure.event;

import lombok.Getter;
import noogel.xyz.search.infrastructure.config.SearchPropertiesConfig;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

public class ConfigAppUpdateEvent extends ApplicationEvent {

    /**
     * 变更前
     */
    @Getter
    private SearchPropertiesConfig.AppConfig oldApp;
    /**
     * 变更后
     */
    @Getter
    private SearchPropertiesConfig.AppConfig newApp;

    public ConfigAppUpdateEvent(Object source) {
        super(source);
    }

    public ConfigAppUpdateEvent(Object source, Clock clock) {
        super(source, clock);
    }

    public static ConfigAppUpdateEvent of(Object source,
                                          SearchPropertiesConfig.AppConfig oldApp,
                                          SearchPropertiesConfig.AppConfig newApp) {
        ConfigAppUpdateEvent event = new ConfigAppUpdateEvent(source);
        event.oldApp = oldApp;
        event.newApp = newApp;
        return event;
    }

}
