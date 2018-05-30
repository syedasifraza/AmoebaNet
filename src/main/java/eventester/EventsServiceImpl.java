package eventester;

import org.apache.felix.scr.annotations.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerManager;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;

@Component(immediate = true)
@Service
public class EventsServiceImpl extends
        AbstractListenerManager<EventsTester, EventsTesterListner>
        implements EventsService{

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private static final Logger log = LoggerFactory.getLogger(EventsServiceImpl.class);

    private static final String APP_NAME = "BDE-AmoebaNet";

    protected ExecutorService eventExecutor;
    private ApplicationId appId;

    @Activate
    protected void activate(ComponentContext context) {
        eventDispatcher.addSink(EventsTester.class, listenerRegistry);
        log.info("Events Tester Service Started");
    }

    @Deactivate
    protected void deactivate() {
        appId = coreService.getAppId(APP_NAME);
        eventDispatcher.removeSink(EventsTester.class);
        eventExecutor = null;
        log.info("Stopped");
    }

    @Override
    public void messageTest() {
        log.info("messageTest function started");
        post(new EventsTester(EventsTester.Type.RMQ_MSG_RECIEVED, "EventTester Listener Service"));
        log.info("After call EventsTester post");
    }

}
