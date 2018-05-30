package pathmanagerservice.impl;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import costservice.CostService;
import exceptionservice.api.JsonExceptionService;
import flowmanagerservice.api.AgentFlowService;
import initialconfigservice.InitConfigService;
import org.apache.felix.scr.annotations.*;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pathmanagerservice.*;
import pathmanagerservice.api.BdePathService;

import java.util.*;

/**
 * Created by root on 4/10/17.
 */
@Component(immediate = true)
@Service
public class BdePathServiceImpl implements BdePathService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InitConfigService initConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CostService costService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected AgentFlowService agentFlowService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected JsonExceptionService jsonExceptionService;

    SaveCalcPath saveCalcPath = new SaveCalcPath();
    Map<Long, List<PathIds>> t = new LinkedHashMap<>();

    Set<CalcPathInfo> pathInfo = new LinkedHashSet<>();
    Set<FlowsInfo> flowsInfo = new LinkedHashSet<>();

    long calcPathIds = 1;
    long installedPathIds = 1;
    @Activate
    protected void activate(ComponentContext context) {

        log.info("BDE Path Service Activated");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }


    @Override
    public void updatePath() {
        log.info("I am in update path");
        Multimap<DeviceId, ConnectPoint> multimap = initConfigService.gatewaysInfo();
        DeviceId dvcTwoId = multimap.keySet().iterator().next();
        pathInfo.iterator().forEachRemaining(n -> {
            if(n.getType().equals("Gateway")) {
                log.info("Gateway cost path updated");
                n.setAvailBW(Math.min(costService.getGwLinkCost(dvcTwoId.toString()),
                        costService.getDtnsLinkCost(n.getDtnIpAddress())));
            } else {
                log.info("host path cost updated");
                n.setAvailBW(Math.min(costService.getDtnsLinkCost(n.getDtnIpAddress2()),
                        costService.getDtnsLinkCost(n.getDtnIpAddress())));
            }
            //dtnsIps.add(n.getDtnIpAddress());
        });
       // for(int i=0; i< dtnsIps.size(); i++) {
       //     calcPath(dtnsIps.get(i));
       // }
    }

    @Override
    public void clearPathInformation() {

        log.info("Path Info DB size: {}", pathInfo.size());
        //pathInfo.clear();
        CalcPathInfo temp[] = new CalcPathInfo[pathInfo.size()];
        pathInfo.iterator().forEachRemaining(p -> {
            int i = 0;
            log.info("clear path information");
            log.info("Items: SessionID {}\n isPersistent {}\n SRC {}\n DST {}",
                    p.getSessionId(), p.isPersistent(), p.getDtnIpAddress(), p.getDtnIpAddress2());
            if(!p.isPersistent()) {
                log.info("Removing item here {}", p.getSessionId());
                //pathInfo.remove(p);
                temp[i] = p;
                i++;

            }
            log.info("After Remove!!!");

        });
        for(int i = 0; i < temp.length; i++) {
            log.info("Deleting {}");
            pathInfo.remove(temp[i]);
        }
        log.info("path info deleted");
        log.info("Path Info DB size after delete: {}", pathInfo.size());
    }

    @Override
    public void calcPath(String src, String dst,
                         String type, Long sessionId) {
        Multimap<DeviceId, ConnectPoint> multimap = initConfigService.gatewaysInfo();
        log.info("SRC {}", src);
        log.info("DST {}", dst);
        log.info("TYPE {}", type);
        log.info("Session ID: {}", sessionId);

        IpAddress oneIp = IpAddress.valueOf(src);
        IpAddress twoIp = IpAddress.valueOf(dst);
        DeviceId dvcOneId = null;
        DeviceId dvcTwoId = null;

        dvcOneId = hostService.getHostsByIp(oneIp).iterator()
                .next().location().deviceId();

        if(type.equals("Gateway")) {
            dvcTwoId = multimap.keySet().iterator().next();
            log.info("gateway dvsTwo set");
        } else {
            dvcTwoId = hostService.getHostsByIp(twoIp).iterator()
                    .next().location().deviceId();
        }

        if (deviceService.isAvailable(dvcTwoId)) {

            if (dvcOneId.toString().equals(dvcTwoId.toString())) {
                CalcPathInfo calcPathInfo = new CalcPathInfo();
                Set<DeviceId> devices = new LinkedHashSet<>();
                boolean isCalcPathInfo[] = new boolean[1];
                isCalcPathInfo[0] = false;

                pathInfo.iterator().forEachRemaining(n -> {
                    if(n.getDtnIpAddress().equals(src) &&
                            n.getDtnIpAddress2().equals(dst) &&
                            n.getType().equals(type)) {
                        isCalcPathInfo[0] = true;
                        log.info("isCalcPathInfo True");
                    }
                    log.info("\ncalcpathID = {} \nDTN1 IP = {}\nDTN2 IP = {}\nAvailBW = {}\n Devices = {}",
                            n.getCalcPathId(), n.getDtnIpAddress(), n.getDtnIpAddress2(),
                            n.getAvailBW(), n.getDeviceIds());

                });

                if(pathInfo.isEmpty() || !isCalcPathInfo[0]) {
                    devices.add(dvcOneId);
                    if(type.equals("Gateway")) {
                        log.info("GW Cost: {}\nSRC DTN Cost:{}", Math.min(
                                costService.getGwLinkCost(dvcTwoId.toString()),
                                costService.getDtnsLinkCost(src)));
                        calcPathInfo.setAvailBW(Math.min(costService.getGwLinkCost(dvcTwoId.toString()),
                                costService.getDtnsLinkCost(src)));
                        log.info("set cost: {}", calcPathInfo.getAvailBW());
                    } else {
                        calcPathInfo.setAvailBW(Math.min(costService.getDtnsLinkCost(dst),
                                costService.getDtnsLinkCost(src)));
                        log.info("src and dst cost");
                    }

                    //calcPathInfo.setCalcPathId(calcPathIds);
                    calcPathInfo.setPersistent(false);
                    calcPathInfo.setType(type);
                    calcPathInfo.setSessionId(sessionId);
                    calcPathInfo.setDtnIpAddress(src);
                    calcPathInfo.setDtnIpAddress2(dst);
                    calcPathInfo.setDeviceIds(devices);
                    pathInfo.add(calcPathInfo);
                    calcPathIds = calcPathIds + 1;
                    log.info("set cost: {}\n session ID: {}", calcPathInfo.getAvailBW(), calcPathInfo.getSessionId());
                }

            } else {
                CalcPathInfo calcPathInfo = new CalcPathInfo();
                CalcPathInfo temp[] = new CalcPathInfo[1];
                boolean isCalcPathInfo[] = new boolean[1];
                isCalcPathInfo[0] = false;
                DeviceId two[] = new DeviceId[1];
                two[0] = dvcTwoId;
                int i = 0;

                Iterator<TopologyEdge> edges = topologyService.getGraph(
                        topologyService.currentTopology()).getEdges().iterator();
                log.info("after edges");
                int size = topologyService.getGraph(
                        topologyService.currentTopology()).getEdges().size();
                final AgentGraph.Edge[] graph = new AgentGraph.Edge[size];
                Multimap<String, String> sd = ArrayListMultimap.create();
                log.info("after topology amoebanetmanager get graph");
                edges.forEachRemaining(n -> sd.put(n.src().toString(), n.dst().toString()));
                for (Map.Entry<String, String> entry : sd.entries()) {
                    graph[i] = new AgentGraph.Edge(entry.getKey(), entry.getValue(),
                            (int) costService.retriveCost(entry.getKey(), entry.getValue()));
                    i++;
                }
                log.info("after edges");
                final String start = dvcOneId.toString();
                final String end = dvcTwoId.toString();
                log.info("after start and end");
                AgentGraph g = new AgentGraph(graph);
                g.cleanPath();
                g.dijkstra(start);
                log.info("after start");
                g.printPath(end);
                log.info("after end");

                pathInfo.iterator().forEachRemaining(n -> {
                    if(n.getDtnIpAddress().equals(src) &&
                            n.getDtnIpAddress2().equals(dst) &&
                            n.getType().equals(type)) {
                        isCalcPathInfo[0] = true;
                        n.setDeviceIds(g.getDvcInPath().keySet().iterator().next());
                        if(type.equals("Gateway")) {
                            n.setAvailBW(Math.min(Math.min(costService.getGwLinkCost(two[0].toString()),
                                     costService.getDtnsLinkCost(src)), g.getDvcInPath()
                                    .values().iterator().next()
                                    .longValue()));
                        } else {
                            n.setAvailBW(Math.min(Math.min(costService.getDtnsLinkCost(dst),
                                    costService.getDtnsLinkCost(src)), g.getDvcInPath().values().iterator().next()
                                    .longValue()));
                        }

                        log.info("DTN IP Address in DB and Devices Calculated {}", g.getDvcInPath());
                    }
                    log.info("\ncalcpathID = {} \nDTN IP = {}\nAvailBW = {}\n Devices = {}", n.getCalcPathId(),
                            n.getDtnIpAddress(), n.getAvailBW(), n.getDeviceIds());

                });


                if(pathInfo.isEmpty() || !isCalcPathInfo[0]) {

                    //log.info("Devices in Path {}", g.getDvcInPath());
                    //log.info("Path inform {}", pathInfo);
                    long avialbwtemp;
                    calcPathInfo.setCalcPathId(calcPathIds);
                    calcPathInfo.setDtnIpAddress(src);
                    calcPathInfo.setDtnIpAddress2(dst);
                    calcPathInfo.setType(type);
                    calcPathInfo.setDeviceIds(g.getDvcInPath().keySet().iterator().next());
                    if(type.equals("Gateway")) {
                        avialbwtemp = Math.min(Math.min(costService.getGwLinkCost(dvcTwoId.toString()),
                                 costService.getDtnsLinkCost(src)), g.getDvcInPath()
                                .values().iterator().next().longValue());
                    } else {
                        avialbwtemp = Math.min(Math.min(costService.getDtnsLinkCost(dst),
                                 costService.getDtnsLinkCost(src)), g.getDvcInPath()
                                .values().iterator().next().longValue());
                    }

                    //log.info("Path Bandwidth {}", costService.getGwLinkCost(dvcTwoId.toString()));
                    calcPathInfo.setAvailBW(avialbwtemp);
                    calcPathInfo.setPersistent(false);
                    calcPathInfo.setSessionId(sessionId);
                    pathInfo.add(calcPathInfo);
                    log.info("Calculated PathId {}", calcPathIds);
                    calcPathIds = calcPathIds + 1;

                }

                pathInfo.iterator().forEachRemaining(h -> {
                    log.info("Path ID {}", h.getCalcPathId());
                    log.info("DTN IP {}", h.getDtnIpAddress());
                    log.info("BW  {}", h.getAvailBW());
                    log.info("Devices {}", h.getDeviceIds());
                    log.info("session ID {}", h.getSessionId());
                    log.info("Path Type {}", h.getType());
                });
            }
        }

    }

    @Override
    public long getPathBW(String ipAddress1, String ipAddress2, String type) {
        log.info("path info size: {}", pathInfo.size());
        long bandwidth[] = new long[1];
        pathInfo.iterator().forEachRemaining(n -> {

            if(n.getDtnIpAddress().equals(ipAddress1) &&
                    n.getDtnIpAddress2().equals(ipAddress2)
                    && n.getType().equals(type)) {
                bandwidth[0] = n.getAvailBW();
                log.info("Available BW condition true {}", n.getAvailBW());
            }
            log.info("Types : {}", n.getType());
        });
        return bandwidth[0];
    }

    @Override
    public long getCalcPathID(String ipAddress1, String ipAddress2, String type) {
        long calcPathID[] = new long[1];
        pathInfo.iterator().forEachRemaining(n -> {
            if(n.getDtnIpAddress().equals(ipAddress1)
                    && n.getDtnIpAddress2().equals(ipAddress2)
                    && n.getType().equals(type)) {
                calcPathID[0] = n.getSessionId();
            }
        });
        return calcPathID[0];
    }

    @Override
    public boolean checkPathId(Long pathId) {
        log.info("checking path");
        boolean calcPathID[] = new boolean[1];
        calcPathID[0]= false;
        if(pathInfo.isEmpty()) {
            log.info("path not found false");
            return false;
        }
        else {
            pathInfo.iterator().forEachRemaining(n -> {
                if(n.getSessionId() == pathId) {
                    log.info("path found true");
                    calcPathID[0] = true;
                }
            });
            return calcPathID[0];
        }

    }

    @Override
    public long isFlowsInstalled(long sessionId, String srcIP, String dstIP,
                                    String srcPort, String dstPort, Double rate,
                                 String vlanId, String oscarsVlanId, String trafficType) {
        log.info("isflow installed");
        long value[] = new long[1];
        value [0] = 0;
        log.info("isflow installed {}", flowsInfo.isEmpty());
        if(!flowsInfo.isEmpty()) {
            log.info("inside flow empty check");
            flowsInfo.iterator().forEachRemaining(f -> {
                if (f.getCalcPathId() == sessionId
                        && f.getSrcIP().equals(srcIP)
                        && f.getDstIP().equals(dstIP)
                        && f.getSrcPort().equals(srcPort)
                        && f.getDstPort().equals(dstPort)
                        && f.getRate() == rate) {
                    value[0] = f.getInstalledPathId();
                }
            });
        }
        return value[0];
    }

    @Override
    public Set<Long> getPathIds() {
        return t.keySet();
    }

    @Override
    public String getEndTime(Long installedPathID) {
        String temp[] = new String[1];
        temp[0] = null;
        flowsInfo.iterator().forEachRemaining(endTime ->{
            if(endTime.getInstalledPathId() == installedPathID) {
                temp[0] = endTime.getEndTime();
            }
        });
        return temp[0];
    }

    @Override
    public String getStartTime(Long installedPathID) {
        String temp[] = new String[1];
        temp[0] = null;
        flowsInfo.iterator().forEachRemaining(startTime ->{
            if(startTime.getInstalledPathId() == installedPathID) {
                temp[0] = startTime.getStartTime();
            }
        });
        return temp[0];
    }


    @Override
    public Boolean isRequiredSchedule(Long installedPathID) {
        Boolean temp[] = new Boolean[1];
        temp[0] = false;
        flowsInfo.iterator().forEachRemaining(startTime ->{
            if(startTime.getInstalledPathId() == installedPathID) {
                temp[0] = startTime.isRequiredSchedule();
            }
        });
        return temp[0];
    }

    @Override
    public void changeScheduledStatus(Long installedPathID) {
        flowsInfo.iterator().forEachRemaining(startTime ->{
            if(startTime.getInstalledPathId() == installedPathID) {
                startTime.setRequiredSchedule(false);
            }
        });

    }

    @Override
    public void setupPathSchedule(Long installedPathID) {
        log.info("setupPathSchedule!!!");
        Long sessionId[] = new Long[1];
        String srcIP[] = new String[1];
        String dstIP[] = new String[1];
        String srcPort[] = new String[1];
        String dstPort[] = new String[1];
        Double rate[] = new Double[1];
        String vlanId[] = new String[1];
        String oscarsVlanId[] = new String[1];
        String trafficType[] = new String[1];

        Multimap<DeviceId, Map<PortNumber, PortNumber>> portInfo = ArrayListMultimap.create();
        List<PathIds> g = Lists.newArrayList();
        Set<DeviceId> dvcs = new LinkedHashSet<>();
        IpAddress hostIp [] = new IpAddress[2];

        flowsInfo.iterator().forEachRemaining(startTime ->{
            if(startTime.getInstalledPathId() == installedPathID) {
                sessionId[0] = startTime.getCalcPathId();
                srcIP[0] = startTime.getSrcIP();
                dstIP[0] = startTime.getDstIP();
                srcPort[0] = startTime.getSrcPort();
                dstPort[0] = startTime.getDstPort();
                rate[0] = startTime.getRate();
                vlanId[0] = startTime.getVlanId();
                oscarsVlanId[0] = startTime.getOscarsVlanId();
                trafficType[0] = startTime.getTrafficType();
            }
        });

        pathInfo.iterator().forEachRemaining(n -> {
            if(n.getSessionId() == sessionId[0] &&
                    n.getDtnIpAddress().equals(srcIP[0]) &&
                    n.getDtnIpAddress2().equals(dstIP[0])) {
                hostIp[0] = IpAddress.valueOf(n.getDtnIpAddress());
                n.getDeviceIds().iterator().forEachRemaining(dvcs::add);
                n.setSessionId(sessionId[0]);
                log.info("devices {}", dvcs);
                if(n.getType().equals("Gateway")) {
                    log.info("Path Gateway detected");
                    hostIp[1] = null;
                } else {
                    log.info("Setup Path 2nd IP Address detected one {}\n two {}",
                            n.getDtnIpAddress(), n.getDtnIpAddress2());
                    hostIp[1] = IpAddress.valueOf(n.getDtnIpAddress2());
                }
            }
        });

        PortNumber hostPort, hostPort2;

        if(hostIp[1] == null) {
            HostId hostId = hostService.getHostsByIp(hostIp[0]).iterator().next().id();
            hostPort = hostService.getHost(hostId).location().port();
            hostPort2 = null;
        } else {
            HostId hostId = hostService.getHostsByIp(hostIp[0]).iterator().next().id();
            hostPort = hostService.getHost(hostId).location().port();
            HostId hostId2 = hostService.getHostsByIp(hostIp[1]).iterator().next().id();
            hostPort2 = hostService.getHost(hostId2).location().port();
        }

        portInfo = pathLinks(dvcs, hostPort, hostPort2);

        for (DeviceId items : portInfo.keySet()) {

            Set<Long> fId = Sets.newLinkedHashSet();
            PortNumber inPort;
            PortNumber outPort;
            log.info("items in devices {}", portInfo);
            log.info("size {}", portInfo.get(items).size());
            for (int i = 0; i < portInfo.get(items).size(); i++) {
                inPort = portInfo.get(items).iterator().next().keySet().iterator().next();
                outPort = portInfo.get(items).iterator().next().get(inPort);
                log.info("Device {}, inport {}, outport {}", items, inPort, outPort);
                if (initConfigService.gatewaysInfo().keySet().iterator().next().equals(items)
                        && hostIp[1] == null) {
                    log.info("before if when hostIP null");
                    agentFlowService.installFlows(items, inPort, outPort, srcIP[0],
                            dstIP[0], srcPort[0], dstPort[0], (rate[0] * 1000), fId, trafficType[0],
                            vlanId[0], oscarsVlanId[0]);
                    log.info("send vlan and oscars ids");
                } else {
                    log.info("In else before installFlows run");
                    agentFlowService.installFlows(items, inPort, outPort, srcIP[0],
                            dstIP[0], srcPort[0], dstPort[0], (rate[0] * 1000), fId, trafficType[0],
                            vlanId[0], "");
                    log.info("send only vlan id");
                }

            }
            g.add(new PathIds(items, fId));
        }

        t.replace(installedPathID, g);

    }

    @Override
    public long setupPath(Long sessionId, String srcIP, String dstIP,
                          String srcPort, String dstPort, Double rate,
                          String vlanId, String oscarsVlanId,
                          String trafficType, String endTime, String startTime,
                          boolean isScheduled) {

        log.info("inside setup path");
        FlowsInfo flowinfo = new FlowsInfo();
        List<PathIds> g = Lists.newArrayList();
        Multimap<DeviceId, Map<PortNumber, PortNumber>> portInfo = ArrayListMultimap.create();
        Set<DeviceId> dvcs = new LinkedHashSet<>();
        IpAddress hostIp [] = new IpAddress[2]; //IpAddress.valueOf(pathId);

        pathInfo.iterator().forEachRemaining(n -> {
            log.info("pathinfo itrator setup path {}, \nseesioId {}",
                     n.getSessionId(), sessionId);
            log.info("pathinfo n.DTN1 {}, \nsrcIP {}",
                     n.getDtnIpAddress(), srcIP);
            log.info("pathinfo n.DTN2 {}, \ndstIP {}",
                     n.getDtnIpAddress2(), dstIP);
            log.info("Session ID {} and Session ID {}",
                    n.getSessionId(), sessionId);
            if(n.getSessionId() == sessionId &&
                    n.getDtnIpAddress().equals(srcIP) &&
                    n.getDtnIpAddress2().equals(dstIP)) {
                log.info("before IP addess");
                hostIp[0] = IpAddress.valueOf(n.getDtnIpAddress());
                log.info("before dvcs");
                n.getDeviceIds().iterator().forEachRemaining(dvcs::add);
                n.setSessionId(sessionId);
                log.info("after dvcs");
                log.info("devices {}", dvcs);
                if(n.getType().equals("Gateway")) {
                    log.info("In Setup Path Gateway detected");
                    hostIp[1] = null;
                } else {
                    log.info("In Setup Path 2nd IP Address detected");
                    hostIp[1] = IpAddress.valueOf(n.getDtnIpAddress2());
                }

            }

        });

        if(!trafficType.equals("1")) {
            rate = 0.0;
        }
        PortNumber hostPort, hostPort2;

        if(hostIp[1]==null) {
            HostId hostId = hostService.getHostsByIp(hostIp[0]).iterator().next().id();
            hostPort = hostService.getHost(hostId).location().port();
            hostPort2 = null;
            //log.info("Host Port number {}", hostService.getHost(hostId).location().port());
        } else {
            HostId hostId = hostService.getHostsByIp(hostIp[0]).iterator().next().id();
            hostPort = hostService.getHost(hostId).location().port();
            HostId hostId2 = hostService.getHostsByIp(hostIp[1]).iterator().next().id();
            hostPort2 = hostService.getHost(hostId2).location().port();
        }


        if(!isScheduled) {
            portInfo = pathLinks(dvcs, hostPort, hostPort2);

            for (DeviceId items : portInfo.keySet()) {

                Set<Long> fId = Sets.newLinkedHashSet();
                PortNumber inPort;
                PortNumber outPort;
                log.info("items in devices {}", portInfo);
                log.info("size {}", portInfo.get(items).size());
                for (int i = 0; i < portInfo.get(items).size(); i++) {
                    inPort = portInfo.get(items).iterator().next().keySet().iterator().next();
                    outPort = portInfo.get(items).iterator().next().get(inPort);
                    log.info("Device {}, inport {}, outport {}", items, inPort, outPort);
                    if (initConfigService.gatewaysInfo().keySet().iterator().next().equals(items)
                            && hostIp[1] == null) {
                        agentFlowService.installFlows(items, inPort, outPort, srcIP,
                                dstIP, srcPort, dstPort, (rate * 1000), fId, trafficType,
                                vlanId, oscarsVlanId);
                        log.info("send vlan and oscars ids");
                    } else {
                        agentFlowService.installFlows(items, inPort, outPort, srcIP,
                                dstIP, srcPort, dstPort, (rate * 1000), fId, trafficType,
                                vlanId, "");
                        log.info("send only vlan id");
                    }

                }
                g.add(new PathIds(items, fId));

            }
        }

        pathInfo.iterator().forEachRemaining(n -> {
            if (n.getSessionId() == sessionId &&
                    n.getDtnIpAddress().equals(srcIP) &&
                    n.getDtnIpAddress2().equals(dstIP)) {
                n.setPersistent(true);
            }
        });

        t.put(installedPathIds, g);

        costService.changeCost(dvcs, rate, true);
        if(hostIp[1]==null) {
            costService.changeGwCost(initConfigService.gatewaysInfo().keySet()
                    .iterator().next().toString(), rate, true);
        } else {
            costService.changeDtnCost(hostIp[1].toString(), rate, true);
        }
        costService.changeDtnCost(hostIp[0].toString(), rate, true);
        flowinfo.setCalcPathId(sessionId);
        flowinfo.setInstalledPathId(installedPathIds);
        flowinfo.setSrcIP(srcIP);
        flowinfo.setDstIP(dstIP);
        flowinfo.setSrcPort(srcPort);
        flowinfo.setDstPort(dstPort);
        flowinfo.setRate(rate);
        flowinfo.setVlanId(vlanId);
        flowinfo.setOscarsVlanId(oscarsVlanId);
        flowinfo.setTrafficType(trafficType);
        flowinfo.setEndTime(endTime);
        flowinfo.setStartTime(startTime);
        flowinfo.setRequiredSchedule(isScheduled);
        flowsInfo.add(flowinfo);
        installedPathIds = installedPathIds + 1;
        return (installedPathIds - 1);
    }

    @Override
    public JsonObject allPathInformation() {
        JsonObject outer = new JsonObject();
        JsonArray middle = new JsonArray();
        flowsInfo.iterator().forEachRemaining(p -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("PathId", p.getInstalledPathId());
            obj.addProperty("Source IP", p.getSrcIP().toString());
            obj.addProperty("Destination IP", p.getDstIP().toString());
            middle.add(obj);
        });
        outer.add("PathInfo", middle);
        return outer;

    }

    @Override
    public boolean releasePathId(Long pathId) {
        boolean returnValue = false;
        Set<DeviceId> dvcs = new LinkedHashSet<>();
        FlowsInfo temp[] = new FlowsInfo[1];
        String dtnIp[] = new String[3];
        log.info("pathId {}", pathId);
        log.info("b4 delete path {}", t.keySet().isEmpty());
        if (t.keySet().contains(pathId) && !t.isEmpty()) {
            log.info("Installed Path ID in Map");
            t.get(pathId).iterator().forEachRemaining(p -> {
                agentFlowService.removePathId(p.dvcIds(), p.flwIds());
            });
            t.remove(pathId);

            log.info("flowsInfo size {}", flowsInfo.size());
            flowsInfo.iterator().forEachRemaining(n -> {
                log.info("FlowInfo {}", n.getSrcIP());
                log.info("getInstalledPathId {}\nPathId {}", n.getInstalledPathId(), pathId);
                if(n.getInstalledPathId() ==  pathId) {

                    pathInfo.iterator().forEachRemaining(m -> {
                        log.info("PathInfo Iterator Passed1 {}", m.getDtnIpAddress());
                        if(m.getSessionId() == n.getCalcPathId() &&
                                m.getDtnIpAddress().equals(n.getSrcIP()) &&
                                m.getDtnIpAddress2().equals(n.getDstIP())) {

                            m.getDeviceIds().iterator().forEachRemaining(dvcs::add);
                            dtnIp[0] = m.getDtnIpAddress();
                            dtnIp[1] = m.getDtnIpAddress2();
                            dtnIp[2] = m.getType();
                            m.setPersistent(false);
                            log.info("dtnip1 and dtnip2 got it");

                        }
                        log.info("PathInfo Iterator Passed2");
                    });

                    log.info("Before change costservice");
                    if(dtnIp[2].equals("Gateway")) {
                        log.info("gateway cost reduced");
                        costService.changeGwCost(initConfigService.gatewaysInfo().keySet().iterator().next().toString(),
                                n.getRate(), false);
                    } else {
                        log.info("dtn2 cost reduced");
                        costService.changeDtnCost(dtnIp[1], n.getRate(), false);
                    }
                    costService.changeDtnCost(dtnIp[0], n.getRate(), false);
                    costService.changeCost(dvcs, n.getRate(), false);
                    log.info("Before remove");
                    temp[0] = n;
                }
                log.info("FlowInfo {}", n);
                log.info("FlowInfo Iterator Passed1");
            });
            log.info("Removed or not {}", flowsInfo.remove(temp[0]));
            returnValue = true;
        }
        return returnValue;

    }

    public Multimap<DeviceId, Map<PortNumber, PortNumber>> pathLinks(Set<DeviceId> devices,
                                                                     PortNumber hostPort,
                                                                     PortNumber hostPort2) {
        DeviceId previousDevice = null;
        Set<TopologyEdge> edges = topologyService.getGraph(
                topologyService.currentTopology()).getEdges();
        Multimap<DeviceId, Map<PortNumber, PortNumber>> portsMap = ArrayListMultimap.create();
        Map<PortNumber, PortNumber> ppMap = new HashMap<>();
        PortNumber inport = hostPort;
        DeviceId gwDevice = null;
        Multimap<DeviceId, ConnectPoint> multimap = initConfigService.gatewaysInfo();
        log.info("Iam in for loop of pathLinks function \n Devices size {}", devices.size());

        if (devices.size() == 1) {
            if(hostPort2 == null) {
                gwDevice = multimap.keys().iterator().next();
                ppMap.put(inport, multimap.get(gwDevice).iterator().next().port());
                portsMap.put(gwDevice, ppMap);
            } else {
                ppMap.put(inport, hostPort2);
                portsMap.put(devices.iterator().next(), ppMap);
            }

        } else {
            for (DeviceId item : devices) {

                for (TopologyEdge edgeIterator : edges) {
                    //log.info("previous src {} current src {} ", previousDevice,
                    //        edges.next().src().deviceId());
                    if (edgeIterator.src().deviceId().equals(previousDevice) &&
                            edgeIterator.dst().deviceId().equals(item)) {
                        Map<PortNumber, PortNumber> pMap = new HashMap<>();
                        log.info("OutPort Number {}", edgeIterator.link().src().port());
                        log.info("InPort Number {}", edgeIterator.link().dst().port());

                        pMap.put(inport, edgeIterator.link().src().port());
                        inport = edgeIterator.link().dst().port();
                        gwDevice = edgeIterator.link().dst().deviceId();
                        portsMap.put(edgeIterator.link().src().deviceId(), pMap);
                        break;
                    }

                }
                previousDevice = item;

            }
            if(hostPort2==null) {
                ppMap.put(inport, multimap.get(gwDevice).iterator().next().port());
            } else {
                ppMap.put(inport, hostPort2);
            }

            portsMap.put(gwDevice, ppMap);
        }

        //log.info("\n In Out ports: {} ", portsMap);
        return  portsMap;

    }
}
