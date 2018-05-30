package amoebanetmanager;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import costservice.CostService;
import eventester.DeviceServiceTask;
import eventester.EventsService;
import eventester.EventsTester;
import eventester.EventsTesterListner;
import exceptionservice.api.JsonExceptionService;
import flowmanagerservice.api.AgentFlowService;
import initialconfigservice.InitConfigService;
import org.apache.felix.scr.annotations.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pathmanagerservice.api.BdePathService;
import rmqservice.api.RmqEvents;
import rmqservice.api.RmqMsgListener;
import rmqservice.api.RmqService;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Created by root on 4/1/17.
 */
@Component(immediate = true)
public class AmoebaNetManager {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String APP_NAME = "BDE-AmoebaNet";

    private static final String NET_CONF_EVENT =
            "Received NetworkConfigEvent {}";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RmqService rmqService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventsService eventsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BdePathService getpath;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InitConfigService initConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CostService costService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected AgentFlowService agentFlowService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected JsonExceptionService jsonExceptionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterService meterService;

    protected ExecutorService eventExecutor;

    private final InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();

    private final RmqMsgListener rmqMsgListener =
            new InternalRmqMsgListener();

    private final EventsTesterListner eventsTesterListner =
            new InternalEventsTesterListener();

    protected  SessionIDStore sessionIDStore = new SessionIDStore();

    private ApplicationId appId;


    //protected TimerTask timerTask = new SessionIDTimer();
    protected Timer timer = new Timer(true);

    DeviceServiceTask deviceServiceTask = new DeviceServiceTask();

    RunnableDemo R1;

    Set<VirtualPathID> virtualPathID = new LinkedHashSet<>();


    @Activate
    protected void activate(ComponentContext context) {
        appId = coreService.getAppId(APP_NAME);
        eventExecutor = newSingleThreadScheduledExecutor(
                groupedThreads("onos/sdnagentevents", "events-%d", log));


        configService.addListener(configListener);
        rmqService.addListener(rmqMsgListener);
        eventsService.addListener(eventsTesterListner);
        //setupConnectivity(false);
        agentFlowService.removeFlowsByAppId();
        sessionIDStore.setSessionId(null);
        sessionIDStore.setQueryId(false);
        log.info("Service Check Started");

        //timer.scheduleAtFixedRate(timerTask, 1000, 10000);
        deviceServiceTask.setDeviceService(getpath);
        deviceServiceTask.setVirtualPathIDS(virtualPathID);
        //deviceServiceTask.setIniatConfig(initConfigService);
        deviceServiceTask.schedule();
        //eventsService.messageTest();
        //testDate();
        //jsonConverter(rmqService.consume());


    }

    @Deactivate
    protected void deactivate() {
        agentFlowService.removeFlowsByAppId();
        rmqService.removeListener(rmqMsgListener);
        configService.removeListener(configListener);
        eventsService.removeListener(eventsTesterListner);
        eventExecutor.shutdownNow();
        eventExecutor = null;
        sessionIDStore.setSessionId(null);
        timer.cancel();
        deviceServiceTask.getTimer().cancel();
        log.info("Stopped");

    }

    private void testDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        JsonObject checkDate = new JsonObject();
        checkDate.addProperty("date", "2017-08-19 16:04");
        String aa = checkDate.get("date").toString().replaceAll("\"", "");
        log.info("String here: {}", String.valueOf(aa));
        Date date = new Date();

        try {
            String datestr = aa;
            DateFormat formatter;
            Date date1;
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            date1 = formatter.parse(datestr);
            log.info("new date: {}", formatter.format(date1));

            if(formatter.format(date1).equals(dateFormat.format(date))) {
                log.info("Date is equal now: {}", date1);
            } else {
                log.info("Date is not equal");
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

        log.info("Current date and time: {}", dateFormat.format(date));

    }

    private void setupConnectivity(boolean isNetworkConfigEvent) {
        Multimap<DeviceId, ConnectPoint> multimap = initConfigService.gatewaysInfo();
        log.info("Gateway ID: " + multimap);
        if(isNetworkConfigEvent) {
            costService.addGwCost();
        }

    }

    private void msgRecieved() {

        //byte[] body = null;
        //log.info("Consumer Msg of Client {}", rmqService.consume());
        //rmqService.consumerResponse(body);

        if(initConfigService.gatewaysInfo().isEmpty()) {
            log.info("expecption GW not defined");
            jsonExceptionService.gatewayNotFound();
        } else {
            if(sessionIDStore.getSessionId() == null
                    || !sessionIDStore.isQueryId()) {
                log.info("new session id or it is null here");
                sessionIDStore.setSessionId(new Random().nextLong());
            }
            log.info("Calc Path from msgRecieved");
            jsonConverter(rmqService.consume());
            log.info("after JsonConverter");
        }

        //getpath.calcPath(consume);

    }


    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass() == InitConfigService.CONFIG_CLASS) {
                log.debug(NET_CONF_EVENT, event.configClass());
                switch (event.type()) {
                    case CONFIG_UPDATED:
                        log.info("Updated");
                        setupConnectivity(true);
                        break;
                    case CONFIG_REMOVED:
                        log.info("Removed");
                        setupConnectivity(false);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private class InternalRmqMsgListener implements RmqMsgListener {

        @Override
        public void event(RmqEvents rmqEvents) {

            switch (rmqEvents.type()) {
                case RMQ_MSG_RECIEVED:
                    log.info("dispatch");
                    msgRecieved();
                    log.info("After RMQ Event Dispatcher");
                    break;
                default:
                    log.info("No Msg recieved");
                    break;
            }
        }
    }


    private class InternalEventsTesterListener implements EventsTesterListner {

        @Override
        public void event(EventsTester eventsTester) {
            log.info("EventsTester Listener called event function");
            switch (eventsTester.type()) {
                case RMQ_MSG_RECIEVED:
                    log.info("I received Tester Events");
                    break;
                default:
                    log.info("No Msg recieved");
                    break;
            }
        }
    }

    private void jsonConverter(String messegeRecieved) {
        log.info("Recieved in jsonCoverter {}", messegeRecieved);
        Map<String, Long> publishPathInfo = new HashMap<>();
        JsonParser parser = new JsonParser();
        JsonObject json = (JsonObject) parser.parse(messegeRecieved);
        JsonNode routeType = null;
        //log.info("Command = " + json.get("cmd"));

        //Timer task for testing
        //TimerTask timerTask = new SessionIDTimer();
        //Timer timer = new Timer(true);
        log.info("SessionID {}", sessionIDStore.getSessionId());
        if (json.has("cmd") && json.get("cmd").toString()
                .replaceAll("\"", "").equals("probe_request") &&
                !sessionIDStore.isQueryId()) {

            try {
                getpath.clearPathInformation();

                if(json.has("lock") &&
                        json.get("lock").toString().replaceAll("\"", "").equals("1")) {
                    sessionIDStore.setQueryId(true);
                    log.info("Yes lock setted");
                    //timer.schedule(timerTask, 0);
                    //timer.schedule(timerTask, 0, 500);
                    //timer.scheduleAtFixedRate(timerTask, 0, 500);
                    R1 = new RunnableDemo( "Locking");
                    R1.start();
                    log.info("TimerTask started");


                    //Timer task ended....
                } else {
                    log.info("No lock not setted");
                    sessionIDStore.setQueryId(false);
                }


                JsonArray jsonArray = (JsonArray) json.get("hosts");
                for (int i = 0; i < jsonArray.size(); i++) {
                    //log.info("DTNs = " + jsonArray.get(i).getAsJsonObject().get("ip"));
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode hostIp1 = null;
                    JsonNode hostIp2 = null;

                    hostIp1 = mapper.readTree(jsonArray.get(i).getAsJsonObject().toString()).get("ipAddress1");
                    hostIp2 = mapper.readTree(jsonArray.get(i).getAsJsonObject().toString()).get("ipAddress2");
                    routeType = mapper.readTree(jsonArray.get(i).getAsJsonObject().toString()).get("routeType");

                    if(costService.checkDtnsLinkCost(hostIp1.asText())) {
                        log.info("Yes!! DTN cost in list {}", costService.getDtnsLinkCost(hostIp1.asText()));
                    } else {
                        costService.setDtnsLinkCost(hostIp1.asText());
                        log.info("No!! DTN cost is not in list: {}",
                        costService.getDtnsLinkCost(hostIp1.asText()));
                    }

                    if(routeType.asText().equals("h2h")) {
                        log.info("h2h if condition");
                        if (costService.checkDtnsLinkCost(hostIp2.asText())) {
                            log.info("Yes!! DTN cost in list");
                        } else {
                            costService.setDtnsLinkCost(hostIp2.asText());
                            log.info("No!! DTN cost is not in list");
                        }

                        getpath.calcPath(hostIp1.asText(), hostIp2.asText(),
                                "Local", sessionIDStore.getSessionId());
                        publishPathInfo.put(hostIp1.asText(),
                                getpath.getPathBW(hostIp1.asText(), hostIp2.asText(),
                                        "Local"));

                    } else if(routeType.asText().equals("h2g")) {
                        log.info("h2g if condition");
                        getpath.calcPath(hostIp1.asText(), hostIp2.asText(),
                                "Gateway", sessionIDStore.getSessionId());
                        publishPathInfo.put(hostIp1.asText(),
                                getpath.getPathBW(hostIp1.asText(), hostIp2.asText(),
                                        "Gateway"));

                    } else {
                        log.info("else if condition");
                        sessionIDStore.setQueryId(false);
                        sessionIDStore.setSessionId(null);
                        //timer.cancel();
                        jsonExceptionService.cmdNotFound();
                        return;
                    }

                    //cancel after sometime
                    //timer.cancel();
                    log.info("TimerTask cancelled");
                    //log.info("pulishpathinfo done here....");

                }

                if(routeType.asText().equals("h2h")) {
                    log.info("h2h if condition jsonpublisher");
                    jsonPublishCoverter(sessionIDStore.getSessionId(), publishPathInfo);

                } else if(routeType.asText().equals("h2g")) {
                    log.info("h2g if condition jsonpublisher");
                    jsonPublishCoverter(sessionIDStore.getSessionId(), publishPathInfo);
                } else {
                    log.info("else if condition jsonpublisher");
                    sessionIDStore.setQueryId(false);
                    sessionIDStore.setSessionId(null);
                    //timer.cancel();
                    jsonExceptionService.cmdNotFound();
                    return;
                }


            } catch (Exception e) {
                log.info("exception of h2h and gw");
                sessionIDStore.setQueryId(false);
                sessionIDStore.setSessionId(null);
                getpath.clearPathInformation();
                //timer.cancel();
                jsonExceptionService.cmdNotFound();
                e.printStackTrace();
                return;
            }


            log.info("leave probe after running thread");
            //publishPathInfo.put("10.0.0.2", 90.00);

        } else if (json.has("cmd") && json.get("cmd").toString()
                .replaceAll("\"", "").equals("reserve_request")
                && sessionIDStore.getSessionId()!= null &&
                !json.get("start").toString().replaceAll("\"", "").equals("direct")) {
            log.info("Reserve command {}", messegeRecieved);
            byte[] body = null;
            try {
                log.info("star try block in reserve request");
                if (json.has("sessionId") &&
                        getpath.checkPathId(json.get("sessionId").getAsLong())) {
                    log.info("sessionID if statement");
                    long installedPathId;
                    long tokenId = json.get("sessionId").getAsLong();
                    String srcIp = "";
                    String dstIp = "";
                    String srcPort = "";
                    String dstPort = "";
                    String vlanId = "";
                    String oscarsVlanId = "";
                    String trafficType = "";
                    String endTime = "";
                    String startTime = "";
                    double rate = 0;

                    log.info("befer if statements");
                    if(json.get("dtns").getAsJsonObject().has("srcIp")) {
                        srcIp = json.get("dtns").getAsJsonObject().get("srcIp")
                                .toString().replaceAll("\"", "");
                    }

                    if(json.get("dtns").getAsJsonObject().has("dstIp")) {
                        dstIp = json.get("dtns").getAsJsonObject().get("dstIp")
                                .toString().replaceAll("\"", "");
                    }

                    if(json.get("dtns").getAsJsonObject().has("srcPort")) {
                        srcPort = json.get("dtns").getAsJsonObject().get("srcPort")
                                .toString().replaceAll("\"", "");
                    }

                    if(json.get("dtns").getAsJsonObject().has("dstPort")) {
                        dstPort = json.get("dtns").getAsJsonObject().get("dstPort")
                                .toString().replaceAll("\"", "");
                    }

                    if(json.get("dtns").getAsJsonObject().has("rate")) {
                        rate = json.get("dtns").getAsJsonObject().get("rate").getAsDouble();
                    }
                    if(json.get("dtns").getAsJsonObject().has("vlanId")) {
                        vlanId = json.get("dtns").getAsJsonObject().get("vlanId")
                                .toString().replaceAll("\"", "");
                    }
                    if(json.get("dtns").getAsJsonObject().has("oscarsVlanId")) {
                        oscarsVlanId = json.get("dtns").getAsJsonObject().get("oscarsVlanId")
                                .toString().replaceAll("\"", "");
                    }
                    if(json.get("dtns").getAsJsonObject().has("trafficType")) {
                        trafficType = json.get("dtns").getAsJsonObject().get("trafficType")
                                .toString().replaceAll("\"", "");
                    }
                    log.info("before end and start time");
                    if(json.has("end")) {
                        endTime = json.get("end").toString().replaceAll("\"", "");
                    }
                    if(json.has("start")) {
                        startTime = json.get("start").toString().replaceAll("\"", "");
                    }

                    log.info("start date checking");
                    try {
                        JsonObject outer = new JsonObject();
                        Date currentTime = new Date();
                        Date EndTime;
                        Date StartTime;
                        log.info("in date check try block");
                        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        log.info("in date check try block1");
                        EndTime = sdf.parse(endTime);
                        if(!startTime.equals("immediate")) {
                            StartTime = sdf.parse(startTime);
                            if(!(currentTime.compareTo(EndTime) <= 0) ||
                                    !(currentTime.compareTo(StartTime) <= 0) ||
                                    !(StartTime.compareTo(EndTime) <= 0)) {
                                jsonExceptionService.startEndTimeNotFound();
                                log.info("date is not equal to current date and time");
                                return;
                            } else {
                                log.info("In else condition of start time!!");
                                long id = getpath.setupPath(tokenId, srcIp, dstIp,
                                        srcPort, dstPort, rate, vlanId, oscarsVlanId,
                                        trafficType, endTime, startTime, true);
                                log.info("Scheduled PathID {}", id);
                                getpath.updatePath();
                                outer.addProperty("PathSatus", "1");
                                outer.addProperty("InstalledPathId", id);
                                body = bytesOf(outer);
                                rmqService.consumerResponse(body);
                                return;
                            }
                        } else if(!(currentTime.compareTo(EndTime) <= 0)) {
                            jsonExceptionService.startEndTimeNotFound();
                            log.info("date is not equal to current date and time");
                            return;
                        }


                        log.info("date is immediate");

                    } catch (Exception e) {
                        log.info("date try block exception");
                        e.printStackTrace();
                        jsonExceptionService.startEndTimeNotFound();
                        return;
                    }



                    log.info("after if check of members");

                    if(!trafficType.equals("1")) {
                        rate = 0;
                    }
                    JsonObject outer = new JsonObject();
                    long pId = getpath.isFlowsInstalled(tokenId, srcIp, dstIp,
                            srcPort, dstPort, rate, vlanId, oscarsVlanId, trafficType);
                    log.info("after isflow");
                    if( pId == 0) {
                        log.info("pid 0");

                        installedPathId = getpath.setupPath(tokenId, srcIp, dstIp,
                                srcPort, dstPort, rate, vlanId, oscarsVlanId, trafficType,
                                endTime, startTime, false);

                        getpath.updatePath();
                        //log.info("Oath ID {}", json.get("pathId"));
                        //log.info("BW of New path {}", getpath.getPathBW(json.get("pathId")
                        // .toString().replaceAll("\"", "")));

                        outer.addProperty("PathSatus", "1");
                        outer.addProperty("InstalledPathId", installedPathId);
                        body = bytesOf(outer);
                        rmqService.consumerResponse(body);
                        R1.stop();
                        sessionIDStore.setQueryId(false);
                        sessionIDStore.setSessionId(null);
                        getpath.clearPathInformation();
                    } else {
                        jsonExceptionService.pathAlreadyInstalled(pId);
                        return;
                    }

                    log.info("pathId ok 1");

                } else {
                    log.info("reserve else json exeption");
                    jsonExceptionService.sessionIdNotFound();
                    return;
                }


            } catch (Exception e) {
                log.info("reserve error");
                jsonExceptionService.jsonException();
                return;
            }

        } else if (json.has("cmd") && json.get("cmd").toString()
                .replaceAll("\"", "").equals("reserve_request")
                && sessionIDStore.getSessionId()!= null
                && json.get("start").toString().replaceAll("\"", "").equals("direct")) {


            log.info("I am in direct path reservation query");

            if (sessionIDStore.isQueryId()) {
                log.info("Direct path reservation failed");
                jsonExceptionService.sessionOccupied();
                return;
            }
            try {

                sessionIDStore.setQueryId(true);
                log.info("after true");


                    log.info("for loop starts");
                    String hostIp1 = null;
                    String hostIp2 = null;
                    String routeT = null;

                    hostIp1 = json.get("dtns").getAsJsonObject().get("srcIp")
                            .toString().replaceAll("\"", "");
                    hostIp2 = json.get("dtns").getAsJsonObject().get("dstIp")
                            .toString().replaceAll("\"", "");
                    routeT = json.get("dtns").getAsJsonObject().get("routeType")
                            .toString().replaceAll("\"", "");


                    if(costService.checkDtnsLinkCost(hostIp1)) {
                        log.info("Yes!! DTN cost in list {}", costService.getDtnsLinkCost(hostIp1));
                    } else {
                        costService.setDtnsLinkCost(hostIp1);
                        log.info("No!! DTN cost is not in list: {}",
                                costService.getDtnsLinkCost(hostIp1));
                    }

                    if(routeT.equals("h2h")) {
                        log.info("h2h if condition");
                        if (costService.checkDtnsLinkCost(hostIp2)) {
                            log.info("Yes!! DTN cost in list");
                        } else {
                            costService.setDtnsLinkCost(hostIp2);
                            log.info("No!! DTN cost is not in list");
                        }

                        getpath.calcPath(hostIp1, hostIp2,
                                "Local", sessionIDStore.getSessionId());
                        publishPathInfo.put(hostIp1,
                                getpath.getPathBW(hostIp1, hostIp2,
                                        "Local"));
                        log.info("Available Bandwidth {}", getpath.getPathBW(hostIp1, hostIp2, "Local"));

                    } else if(routeT.equals("h2g")) {
                        log.info("h2g if condition");
                        getpath.calcPath(hostIp1, hostIp2,
                                "Gateway", sessionIDStore.getSessionId());
                        publishPathInfo.put(hostIp1,
                                getpath.getPathBW(hostIp1, hostIp2,
                                        "Gateway"));
                        log.info("Available Bandwidth {}", getpath.getPathBW(hostIp1, hostIp2, "Gateway"));

                    } else {
                        log.info("else if condition in direct reserve");
                        sessionIDStore.setQueryId(false);
                        sessionIDStore.setSessionId(null);
                        jsonExceptionService.cmdNotFound();
                        return;
                    }

                    log.info("direct path calc end");
                    //log.info("pulishpathinfo done here....");



            } catch (Exception e) {
                log.info("exception direct path calc");
                sessionIDStore.setQueryId(false);
                sessionIDStore.setSessionId(null);
                getpath.clearPathInformation();
                jsonExceptionService.cmdNotFound();
                e.printStackTrace();
                return;
            }

            byte[] body = null;
            try {
                log.info("star try block in direct reserve request");
                if (getpath.checkPathId(sessionIDStore.getSessionId())) {
                    log.info("sessionID if statement");
                    long installedPathId;
                    long tokenId = sessionIDStore.getSessionId();
                    String srcIp = "";
                    String dstIp = "";
                    String srcPort = "";
                    String dstPort = "";
                    String vlanId = "";
                    String oscarsVlanId = "";
                    String trafficType = "";
                    String endTime = "";
                    String startTime = "";
                    double rate = 0;

                    log.info("befer if statements");
                    if(json.get("dtns").getAsJsonObject().has("srcIp")) {
                        srcIp = json.get("dtns").getAsJsonObject().get("srcIp")
                                .toString().replaceAll("\"", "");
                    }

                    if(json.get("dtns").getAsJsonObject().has("dstIp")) {
                        dstIp = json.get("dtns").getAsJsonObject().get("dstIp")
                                .toString().replaceAll("\"", "");
                    }

                    if(json.get("dtns").getAsJsonObject().has("srcPort")) {
                        srcPort = json.get("dtns").getAsJsonObject().get("srcPort")
                                .toString().replaceAll("\"", "");
                    }

                    if(json.get("dtns").getAsJsonObject().has("dstPort")) {
                        dstPort = json.get("dtns").getAsJsonObject().get("dstPort")
                                .toString().replaceAll("\"", "");
                    }

                    if(json.get("dtns").getAsJsonObject().has("rate")) {
                        rate = json.get("dtns").getAsJsonObject().get("rate").getAsDouble();
                    }
                    if(json.get("dtns").getAsJsonObject().has("vlanId")) {
                        vlanId = json.get("dtns").getAsJsonObject().get("vlanId")
                                .toString().replaceAll("\"", "");
                    }
                    if(json.get("dtns").getAsJsonObject().has("oscarsVlanId")) {
                        oscarsVlanId = json.get("dtns").getAsJsonObject().get("oscarsVlanId")
                                .toString().replaceAll("\"", "");
                    }
                    if(json.get("dtns").getAsJsonObject().has("trafficType")) {
                        trafficType = json.get("dtns").getAsJsonObject().get("trafficType")
                                .toString().replaceAll("\"", "");
                    }

                    if(json.has("end")) {
                        endTime = json.get("end").toString().replaceAll("\"", "");
                    }

                    if(json.has("start")) {
                        startTime = json.get("start").toString().replaceAll("\"", "");
                    }

                    log.info("before time check");
                    try {
                        Date currentTime = new Date();
                        Date EndTime;
                        log.info("in date check try block");
                        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        log.info("parse endTime");
                        EndTime = sdf.parse(endTime);
                        if(!(currentTime.compareTo(EndTime) <= 0)) {
                            sessionIDStore.setQueryId(false);
                            sessionIDStore.setSessionId(null);
                            getpath.clearPathInformation();
                            jsonExceptionService.startEndTimeNotFound();
                            log.info("date is not greater or equal to current dat and time");
                            return;
                        }
                        log.info("date is direct");

                    } catch (Exception e) {
                        log.info("date try block exception");
                        sessionIDStore.setQueryId(false);
                        sessionIDStore.setSessionId(null);
                        getpath.clearPathInformation();
                        jsonExceptionService.startEndTimeNotFound();
                        e.printStackTrace();
                        return;
                    }

                    log.info("after if check of members");

                    if(!trafficType.equals("1")) {
                        rate = 0;
                    }
                    JsonObject outer = new JsonObject();
                    long pId = getpath.isFlowsInstalled(tokenId, srcIp, dstIp,
                            srcPort, dstPort, rate, vlanId, oscarsVlanId, trafficType);
                    log.info("after isflow");
                    if( pId == 0) {
                        log.info("pid 0");

                        installedPathId = getpath.setupPath(tokenId, srcIp, dstIp,
                                srcPort, dstPort, rate, vlanId, oscarsVlanId, trafficType,
                                endTime, startTime, false);
                        getpath.updatePath();
                        outer.addProperty("PathSatus", "1");
                        outer.addProperty("InstalledPathId", installedPathId);
                        body = bytesOf(outer);
                        rmqService.consumerResponse(body);
                    } else {
                        sessionIDStore.setQueryId(false);
                        getpath.clearPathInformation();
                        sessionIDStore.setSessionId(null);
                        jsonExceptionService.pathAlreadyInstalled(pId);
                    }

                    log.info("pathId ok 1");
                } else {
                    log.info("reserve else json exeption");
                    sessionIDStore.setQueryId(false);
                    getpath.clearPathInformation();
                    sessionIDStore.setSessionId(null);
                    jsonExceptionService.jsonException();
                }
            } catch (Exception e) {
                log.info("reserve error");
                sessionIDStore.setQueryId(false);
                getpath.clearPathInformation();
                sessionIDStore.setSessionId(null);
                jsonExceptionService.jsonException();
                return;
            }

            sessionIDStore.setQueryId(false);
            getpath.clearPathInformation();
            sessionIDStore.setSessionId(null);

        } else if (json.has("cmd") && json.get("cmd").toString()
                .replaceAll("\"", "").equals("create_virtual_netslice")
                && sessionIDStore.getSessionId()!= null
                && json.get("start").toString().replaceAll("\"", "").equals("direct")) {

            log.info("I am in virtual network slice path creation");

            sessionIDStore.setQueryId(true);
            log.info("after true");

            JsonArray jsonArray = (JsonArray) json.get("hosts");
            byte[] body = null;
            Set<Long> slicePathIDs = new HashSet<>();
            Date EndTime=null;

            for (int i = 0; i < jsonArray.size(); i++) {
                String hostIp1 = jsonArray.get(i).getAsString();
                for (int x = i; x < jsonArray.size(); x++) {
                    if(x != i) {
                        log.info("\nHost1 {}\nHost2 {}", hostIp1, jsonArray.get(x));
                        String hostIp2 = null;
                        hostIp2 = jsonArray.get(x).toString().replaceAll("\"", "");

                        if(costService.checkDtnsLinkCost(hostIp1)) {
                            log.info("Yes!! DTN cost in list {}", costService.getDtnsLinkCost(hostIp1));
                        } else {
                            costService.setDtnsLinkCost(hostIp1);
                            log.info("No!! DTN cost is not in list: {}",
                                    costService.getDtnsLinkCost(hostIp1));
                            log.info("After HostIP1 {}", hostIp1);
                        }

                        if(costService.checkDtnsLinkCost(hostIp2)) {
                            log.info("Yes!! DTN cost in list {}", costService.getDtnsLinkCost(hostIp2));
                        } else {
                            costService.setDtnsLinkCost(hostIp2);
                            log.info("No!! DTN cost is not in list: {}",
                                    costService.getDtnsLinkCost(hostIp2));
                            log.info("After HostIP2 {}", hostIp2);
                        }

                        getpath.calcPath(hostIp1, hostIp2,
                                "Local", sessionIDStore.getSessionId());
                        long availBW = getpath.getPathBW(hostIp1, hostIp2, "Local");
                        if(json.get("rate").getAsDouble() > availBW) {
                            log.info("Available Bandwidth is less than {}",
                                    json.get("rate").getAsDouble());
                            if(!slicePathIDs.isEmpty()) {
                                slicePathIDs.iterator().forEachRemaining(sPath -> {
                                    getpath.releasePathId(sPath);
                                });
                            }
                            sessionIDStore.setQueryId(false);
                            getpath.clearPathInformation();
                            sessionIDStore.setSessionId(null);
                            jsonExceptionService.gatewayNotFound();

                        } else {
                            log.info("Available Bandwidth is greater than requested rate {}",
                                    availBW);

                            try {
                                log.info("star try block in direct reserve request");
                                if (getpath.checkPathId(sessionIDStore.getSessionId())) {
                                    log.info("sessionID if statement");
                                    long installedPathId;
                                    long tokenId = sessionIDStore.getSessionId();
                                    String srcPort = "";
                                    String dstPort = "";
                                    String vlanId = "";
                                    String endTime = "";
                                    String startTime = "";
                                    double rate = 0;


                                    if(json.has("srcPort")) {
                                        srcPort = json.get("srcPort")
                                                .toString().replaceAll("\"", "");
                                    }

                                    if(json.has("dstPort")) {
                                        dstPort = json.get("dstPort")
                                                .toString().replaceAll("\"", "");
                                    }

                                    if(json.has("rate")) {
                                        rate = json.get("rate").getAsDouble();
                                    }
                                    if(json.has("vlanId")) {
                                        vlanId = json.get("vlanId")
                                                .toString().replaceAll("\"", "");
                                    }

                                    if(json.has("end")) {
                                        endTime = json.get("end").toString().replaceAll("\"", "");
                                    }

                                    if(json.has("start")) {
                                        startTime = json.get("start").toString().replaceAll("\"", "");
                                    }

                                    try {
                                        Date currentTime = new Date();
                                        log.info("I amin date check try block");
                                        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                                        log.info("parse endTime");
                                        EndTime = sdf.parse(endTime);
                                        if(!(currentTime.compareTo(EndTime) <= 0)) {
                                            sessionIDStore.setQueryId(false);
                                            sessionIDStore.setSessionId(null);
                                            getpath.clearPathInformation();
                                            jsonExceptionService.startEndTimeNotFound();
                                            log.info("date not greater or equal to current dat and time");
                                            return;
                                        }
                                        log.info("date is direct");

                                    } catch (Exception e) {
                                        log.info("end date try block exception");
                                        sessionIDStore.setQueryId(false);
                                        sessionIDStore.setSessionId(null);
                                        getpath.clearPathInformation();
                                        jsonExceptionService.startEndTimeNotFound();
                                        e.printStackTrace();
                                        return;
                                    }

                                    log.info("In else condition of start time!!");
                                    installedPathId = getpath.setupPath(tokenId, hostIp1, hostIp2,
                                            srcPort, dstPort, rate, vlanId, "",
                                            "1", endTime, startTime, true);
                                    slicePathIDs.add(installedPathId);
                                    getpath.updatePath();
                                    log.info("Start Time {}", startTime);

                                }

                            } catch (Exception e) {
                                log.info("Virtual slice create error");
                                sessionIDStore.setQueryId(false);
                                getpath.clearPathInformation();
                                sessionIDStore.setSessionId(null);
                                jsonExceptionService.jsonException();
                                if(!slicePathIDs.isEmpty()) {
                                    slicePathIDs.iterator().forEachRemaining(sPath -> {
                                        getpath.releasePathId(sPath);
                                    });
                                }
                                return;
                            }
                        }
                    }
                }
            }

            log.info("Scheduled Slice PathIDs {}", slicePathIDs);

            if(!slicePathIDs.isEmpty()) {
                slicePathIDs.iterator().forEachRemaining(sPaths -> {
                    getpath.setupPathSchedule(sPaths);
                    getpath.changeScheduledStatus(sPaths);
                });
                VirtualPathID vpid = new VirtualPathID();
                vpid.setVirtPathID(new Random().nextLong());
                vpid.setPathIDs(slicePathIDs);
                vpid.setEndTime(EndTime);
                log.info("\nsetVirtPathId {}\nsetPathIDs {}\nsetEndTime {}", vpid.getVirtPathID(),
                        vpid.getPathIDs(), vpid.getEndTime());
                virtualPathID.add(vpid);
                JsonObject outer = new JsonObject();
                outer.addProperty("PathSatus", "Activated");
                outer.addProperty("VirtualSliceId", vpid.getVirtPathID());
                body = bytesOf(outer);
                rmqService.consumerResponse(body);
            } else {
                jsonExceptionService.cmdNotFound();
                log.info("after else statement");
                sessionIDStore.setQueryId(false);
                getpath.clearPathInformation();
                sessionIDStore.setSessionId(null);
            }

            sessionIDStore.setQueryId(false);
            getpath.clearPathInformation();
            sessionIDStore.setSessionId(null);


        } else if (json.has("cmd") && json.get("cmd").toString()
                .replaceAll("\"", "").equals("path_request")) {

            //log.info("Release command {}", messegeRecieved);
            byte[] body = null;

            try {
                JsonObject outer = new JsonObject();
                outer = getpath.allPathInformation();
                body = bytesOf(outer);
                rmqService.consumerResponse(body);

            } catch (Exception e) {
                log.info("path request problem");
                jsonExceptionService.jsonException();
                return;
            }
        } else if (json.has("cmd") && json.get("cmd").toString()
                .replaceAll("\"", "").equals("release_request")) {

            //log.info("Release command {}", messegeRecieved);
            boolean pathStatus;
            try {
                pathStatus = getpath.releasePathId(json.get("installedPathId").getAsLong());
                byte[] body = null;
                JsonObject outer = new JsonObject();
                if (pathStatus) {
                    outer.addProperty("PathSatus", "1");
                    body = bytesOf(outer);
                    getpath.updatePath();
                    log.info("No in in updatePath");
                    getpath.clearPathInformation();
                    log.info("No error in clearPath");
                    rmqService.consumerResponse(body);

                } else {
                    outer.addProperty("PathSatus", "0");
                    body = bytesOf(outer);
                    rmqService.consumerResponse(body);
                }
            } catch (Exception e) {
                log.info("release not working");
                getpath.clearPathInformation();
                jsonExceptionService.jsonException();
                return;
            }
        } else if (json.has("cmd") && json.get("cmd").toString()
                .replaceAll("\"", "").equals("delete_virtual_netslice")) {

            //log.info("Release command {}", messegeRecieved);
            Boolean pathStatus[] = new Boolean[1];
            VirtualPathID vpid[] = new VirtualPathID[virtualPathID.size()];
            try {
                virtualPathID.iterator().forEachRemaining(n -> {
                    int i = 0;
                    if(n.getVirtPathID().equals(json.get("virtualPathId").getAsLong())) {
                        n.getPathIDs().iterator().forEachRemaining(p -> {
                            pathStatus[0] = getpath.releasePathId(p);
                            getpath.updatePath();
                            getpath.clearPathInformation();
                            log.info("in virt delete PathID {}", p);
                        });
                        vpid[i] = n;
                        i++;
                    }
                });

                byte[] body = null;
                JsonObject outer = new JsonObject();
                if (pathStatus[0]) {
                    outer.addProperty("VirtualPathSatus", "1");
                    body = bytesOf(outer);
                    rmqService.consumerResponse(body);
                    for(int i = 0; i < vpid.length; i++) {
                        //log.info("Deleting {}");
                        virtualPathID.remove(vpid[i]);
                    }

                } else {
                    outer.addProperty("VirtualPathSatus", "0");
                    body = bytesOf(outer);
                    rmqService.consumerResponse(body);
                }
            } catch (Exception e) {
                log.info("release not working");
                getpath.clearPathInformation();
                jsonExceptionService.jsonException();
                return;
            }
        } else if (json.has("cmd") && json.get("cmd").toString()
                .replaceAll("\"", "").equals("probe_request")  &&
                        sessionIDStore.isQueryId() && sessionIDStore.getSessionId()!= null) {
            log.info("Session already occupied");
            jsonExceptionService.sessionOccupied();

        } else {
            log.info("isQueryId: {}\n sessionID: {}", sessionIDStore.isQueryId(), sessionIDStore.getSessionId());
            log.info("Final else condition called.....");
            getpath.clearPathInformation();
            jsonExceptionService.cmdNotFound();
        }

    }

    private void jsonPublishCoverter(Long sessionId, Map<String, Long> dtns) {
        byte[] body = null;
        JsonObject outer = new JsonObject();
        JsonArray middle = new JsonArray();
        log.info("DTNs size {}", dtns.size());
        outer.addProperty("cmd", "reserve_response");
        for (Map.Entry items : dtns.entrySet()) {
            JsonObject obj = new JsonObject();
            //log.info("Keys IPs {}", items.getKey().toString().replaceAll("\"", ""));
            //log.info("Value {}", items.getValue().toString());
            //obj.addProperty("DtnIP", items.getKey().toString().replaceAll("\"", ""));
            obj.addProperty("AvailBW", items.getValue().toString());
            obj.addProperty("SessionId", sessionId);
            middle.add(obj);
        }

        outer.add("dtns", middle);
        body = bytesOf(outer);
        log.info("before consumer");
        rmqService.consumerResponse(body);
        log.info("Publish msg {}", outer);
        //log.info("CMD recieved {}", cmd);
        //log.info("TaskID recieved {}", taskId);
        //log.info("DTNs Information {}", dtns.keySet().iterator().next());
    }

    private byte[] bytesOf(JsonObject jo) {
        return jo.toString().getBytes();
    }
}

