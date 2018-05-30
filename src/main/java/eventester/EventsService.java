package eventester;

import org.onosproject.event.ListenerService;

public interface EventsService extends ListenerService<EventsTester, EventsTesterListner> {
    public void messageTest();
}
