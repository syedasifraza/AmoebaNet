package amoebanetmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RunnableDemo implements Runnable {
    private Thread t;
    private String threadName;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private SessionIDStore sessionIDStore = new SessionIDStore();

    RunnableDemo( String name) {
        threadName = name;
        log.info("Creating " +  threadName );
    }

    public void run() {
        log.info("Running " +  threadName );
        try {

            Thread.sleep(50000);
            sessionIDStore.setSessionId(null);
            sessionIDStore.setQueryId(false);


        } catch (InterruptedException e) {
            log.info("Thread " +  threadName + " interrupted.");
        }
        log.info("Thread " +  threadName + " exiting.");
    }

    public void start () {
        log.info("Starting " +  threadName );
        if (t == null) {
            t = new Thread (this, threadName);
            t.start ();

        }
    }

    public void stop() {
        if (t != null) {
            log.info("thread stop expilicity");
            t.stop();
            t = null;
        }
    }
}
