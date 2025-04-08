package noogel.xyz.search.infrastructure.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 数据重置事件
 */
@Getter
public class DataResetEvent extends ApplicationEvent {

    public DataResetEvent(Object source) {
        super(source);
    }

}
