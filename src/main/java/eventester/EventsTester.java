package eventester;

import org.onosproject.event.AbstractEvent;


public class EventsTester extends AbstractEvent<EventsTester.Type, String> {
    public enum Type {
        RMQ_MSG_RECIEVED
    }

    public EventsTester(Type type, String msg) {
        super(type, msg);
    }
}
