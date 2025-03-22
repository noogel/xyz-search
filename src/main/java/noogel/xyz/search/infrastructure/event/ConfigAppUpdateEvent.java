package noogel.xyz.search.infrastructure.event;

import lombok.Getter;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

@Getter
public class ConfigAppUpdateEvent extends ApplicationEvent {

    /**
     * 变更前
     */
    private ConfigProperties.App oldApp;
    /**
     * 变更后
     */
    private ConfigProperties.App newApp;

    public ConfigAppUpdateEvent(Object source) {
        super(source);
    }

    public ConfigAppUpdateEvent(Object source, Clock clock) {
        super(source, clock);
    }

    public static ConfigAppUpdateEvent of(Object source,
                                          ConfigProperties.App oldApp,
                                          ConfigProperties.App newApp) {
        ConfigAppUpdateEvent event = new ConfigAppUpdateEvent(source);
        event.oldApp = oldApp;
        event.newApp = newApp;
        return event;
    }

}
