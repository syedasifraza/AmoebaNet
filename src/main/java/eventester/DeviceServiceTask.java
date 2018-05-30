package eventester;

import amoebanetmanager.VirtualPathID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pathmanagerservice.api.BdePathService;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class DeviceServiceTask {
    private final Logger log = LoggerFactory.getLogger(getClass());


    class Task extends TimerTask {


        public BdePathService getDeviceService() {
            return getpath;
        }

        @Override
        public void run() {

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date currentTime = new Date();
            Date pathEndTime[]= new Date[1];
            Date pathStartTime[]= new Date[1];
            getDeviceService().getPathIds().iterator().forEachRemaining(p->{
                try {
                    if(!getpath.getEndTime(p).equals("") || !getpath.getEndTime(p).equals(null)) {
                        pathEndTime[0] = dateFormat.parse(getpath.getEndTime(p));
                        //log.info("Path ID get end time {}", pathEndTime[0]);
                    }
                    //log.info("GetStart Time {}\n isReqSch {}",
                    //        getpath.getStartTime(p).equals("direct"),
                    //        getpath.isRequiredSchedule(p));
                    if((!getpath.getStartTime(p).equals("direct")
                            && getpath.isRequiredSchedule(p))
                            || (!getpath.getStartTime(p).equals("immediate")
                            && !getpath.getStartTime(p).equals("direct")
                            && getpath.isRequiredSchedule(p))) {
                        pathStartTime[0] = dateFormat.parse(getpath.getStartTime(p));
                        //log.info("Path ID get Start Time {}", pathStartTime[0]);

                    }
                } catch (ParseException e) {
                    log.info("Timer Exception");
                    e.printStackTrace();
                }
                //log.info("virtualPathIDs size {}", virtualPathIDS.size());
                VirtualPathID vpid [] = new VirtualPathID[virtualPathIDS.size()];
                virtualPathIDS.iterator().forEachRemaining(n -> {
                    int i = 0;
                    //log.info("virtual Path ID {}", n.getVirtPathID());
                    if(dateFormat.format(n.getEndTime()).equals(dateFormat.format(currentTime))) {
                        vpid[i] = n;
                        i++;
                    }
                });
                for(int i = 0; i < vpid.length; i++) {
                    //log.info("Deleting {}");
                    virtualPathIDS.remove(vpid[i]);
                }

                //log.info("virtualPathIDs size {}", virtualPathIDS.size());

                if(dateFormat.format(pathEndTime[0]).equals(dateFormat.format(currentTime))) {
                    //log.info("Deleting Path ID {}", getpath.getEndTime(p));
                    getpath.releasePathId(p);
                    getpath.updatePath();
                    getpath.clearPathInformation();
                }
                if(getpath.isRequiredSchedule(p) && !getpath.getStartTime(p).equals("direct")) {
                    if(dateFormat.format(pathStartTime[0]).equals(dateFormat.format(currentTime))) {
                        //log.info("Is required to schedule {}", getpath.isRequiredSchedule(p));
                        getpath.setupPathSchedule(p);
                        getpath.changeScheduledStatus(p);
                    }
                }
                //pathEndTime[0].compareTo(currentTime) >= 0

            });

            //log.info("Current date and time {}", currentTime);


        }
    }

    public void schedule() {
        this.getTimer().schedule(new Task(), 1, 10000);
    }

    public Timer getTimer() {
        return timer;
    }

    private Timer timer = new Timer(true);

    public void setDeviceService(BdePathService getpath) {
        this.getpath = getpath;
    }

    public void setVirtualPathIDS(Set<VirtualPathID> virtualPathIDS) {this.virtualPathIDS = virtualPathIDS;}


    protected BdePathService getpath;

    protected Set<VirtualPathID> virtualPathIDS;




}
