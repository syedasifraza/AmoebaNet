package rmqservice.api;

import org.onosproject.event.AbstractEvent;

/**
 * Created by root on 4/8/17.
 */
public class RmqEvents extends AbstractEvent<RmqEvents.Type, String> {
    public enum Type {
        RMQ_MSG_RECIEVED
    }

    public RmqEvents(Type type, String msg) {
        super(type, msg);
    }
}
