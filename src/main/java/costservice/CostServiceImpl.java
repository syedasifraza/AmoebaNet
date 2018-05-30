package costservice;

import com.google.common.collect.Multimap;
import initialconfigservice.InitConfigService;
import org.apache.felix.scr.annotations.*;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by root on 5/1/17.
 */
@Component(immediate = true)
@Service
public class CostServiceImpl implements CostService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InitConfigService initConfigService;

    Map<Link, Long> m = new HashMap<>();
    Set<SetHostsCost> setHostsCosts = new LinkedHashSet<>();
    Set<SetGatewaysCost> setGatewaysCosts = new LinkedHashSet<>();

    @Activate
    protected void activate(ComponentContext context) {
        costAtStatup();
        log.info("Cost Service Activated");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    //returns the cost of each link/edge/connection point between two devices.
    @Override
    public long retriveCost(String src, String dst) {

        CostOfLinks cl = new CostOfLinks();
        final long[] cost = new long[1];
        Iterator<Map.Entry<Link, Long>> compare = cl.getCost().entrySet().iterator();

        compare.forEachRemaining(n -> {
            if (n.getKey().src().deviceId().toString().equals(src)
                    && n.getKey().dst().deviceId().toString().equals(dst)) {
                cost[0] = n.getValue();
            }
        });
        return cost[0];

    }

    //Change the cost of devices involved in path, perform increase or decrease of cost
    @Override
    public void changeCost(Set<DeviceId> devices, Double rate, boolean incORdec) {
        log.info("In change cost {}", rate);

        CostOfLinks cl = new CostOfLinks();
        long incDecCost;
        Set<TopologyEdge> edges = topologyService.getGraph(
                topologyService.currentTopology()).getEdges();
        Link links;

        DeviceId previousDevice = null;
        for (DeviceId item : devices) {

            for (TopologyEdge edgeIterator : edges) {

                if ((edgeIterator.src().deviceId().equals(previousDevice) &&
                        edgeIterator.dst().deviceId().equals(item)) ||
                        (edgeIterator.src().deviceId().equals(item) &&
                                edgeIterator.dst().deviceId().equals(previousDevice))) {
                    log.info("Counting links {}", edgeIterator.link());
                    if(incORdec) {
                        incDecCost = (m.get(edgeIterator.link()) - rate.longValue());
                    } else {
                        incDecCost = (m.get(edgeIterator.link()) + rate.longValue());
                    }

                    m.replace(edgeIterator.link(), incDecCost);

                }

            }
            previousDevice = item;

        }
        log.info("changeCost {}", cl.getCost());

    }

    //Change the cost of gateway's port, perform increase or decrease of cost
    @Override
    public void changeGwCost(String GwId, Double rate, boolean incORdec) {
        if(incORdec) {
            setGatewaysCosts.iterator().forEachRemaining(n -> {
                if (n.getGwId().equals(GwId)) {
                    n.setGwCost((n.getGwCost() - rate.longValue()));
                }
            });
        } else {
            setGatewaysCosts.iterator().forEachRemaining(n -> {
                if (n.getGwId().equals(GwId)) {
                    n.setGwCost((n.getGwCost() + rate.longValue()));
                }
            });
        }

    }

    //Change the cost of host's port, perform increase or decrease of cost
    @Override
    public void changeDtnCost(String DtnId, Double rate, boolean incORdec) {
        if(incORdec) {
            setHostsCosts.iterator().forEachRemaining(n -> {
                if (n.getDtnIp().equals(DtnId)) {
                    n.setDtnCost((n.getDtnCost() - rate.longValue()));
                }
            });
        } else {
            setHostsCosts.iterator().forEachRemaining(n -> {
                if (n.getDtnIp().equals(DtnId)) {
                    n.setDtnCost((n.getDtnCost() + rate.longValue()));
                }
            });
        }

    }

    //Set the cost of hosts when no cost initialized.
    @Override
    public void setDtnsLinkCost(String dtnIp) {
        SetHostsCost setHostsCost = new SetHostsCost();
        IpAddress oneIp = IpAddress.valueOf(dtnIp);
        DeviceId dvcOneId = hostService.getHostsByIp(oneIp).iterator()
                .next().location().deviceId();
        double portspeed;
        portspeed = deviceService.getPort(dvcOneId,
                hostService.getHostsByIp(oneIp).iterator().next().location().port()).portSpeed();
        setHostsCost.setDtnIp(dtnIp);
        setHostsCost.setDtnCost((long) portspeed);
        setHostsCosts.add(setHostsCost);
        //log.info("exiting setDTNCost");
    }

    //retrive the cost of host's port.
    @Override
    public long getDtnsLinkCost(String dtnIp) {
        final long[] tmpCost = new long[1];
        setHostsCosts.iterator().forEachRemaining(n -> {
            if(n.getDtnIp().equals(dtnIp)) {
                tmpCost[0] = n.getDtnCost();
            }
        });
        return tmpCost[0];
    }

    //retrive the cost of gateway's port.
    @Override
    public long getGwLinkCost(String deviceId) {
        final long[] tmpCost = new long[1];
        setGatewaysCosts.iterator().forEachRemaining(n -> {
            if(n.getGwId().equals(deviceId)) {
                tmpCost[0] = n.getGwCost();
            }
        });
        return tmpCost[0];
    }

    //check if host's port cost is defined.
    @Override
    public boolean checkDtnsLinkCost(String dtnIp) {
        log.info("DTN IP {}", dtnIp);
        final boolean[] tmp = new boolean[1];
        tmp[0] = false;
        setHostsCosts.iterator().forEachRemaining(n -> {
            if(n.getDtnIp().equals(dtnIp)) {
                tmp[0] = true;
                log.info("Condition true");
            }
            log.info("\n DTN IP {} DTN Cost {}", n.getDtnIp(), n.getDtnCost());
        });
        return tmp[0];
    }

    //Add gateway's port cost if no gateway initailized or new gateway added.
    @Override
    public void addGwCost() {
        Multimap<DeviceId, ConnectPoint> multimap = initConfigService.gatewaysInfo();
        if(setGatewaysCosts.isEmpty()) {
            ifInitConfigEmpty(multimap);
            log.info("Empty GWs");
        } else if(!setGatewaysCosts.iterator().next().getGwId()
                .equals(initConfigService.gatewaysInfo().keySet().iterator().next().toString())) {
            setGatewaysCosts.clear();
            ifInitConfigEmpty(multimap);
            log.info("New GWs added");
        }

        log.info("GW Cost {}", setGatewaysCosts.iterator().next().getGwId());

    }

    public void costAtStatup() {
        Multimap<DeviceId, ConnectPoint> multimap = initConfigService.gatewaysInfo();
        CostOfLinks cl = new CostOfLinks();
        Iterator<TopologyEdge> edges = topologyService.getGraph(
                topologyService.currentTopology()).getEdges().iterator();
        //Map<Link, Long> m = new HashMap<>();
        edges.forEachRemaining(n -> {
            m.put(n.link(), calcPortSpeed(n.link()));
            cl.setCost(m);
        });

        if(!multimap.keySet().isEmpty()) {
            ifInitConfigEmpty(multimap);
            log.info("Setting GW portspeed");

        }
        //log.info("Links Cost Assigned {}", cl.getCost());

    }

    private long calcPortSpeed(Link link) {
        long portSpeed;
        portSpeed = (deviceService.getPort(link.src().deviceId(),
                link.src().port()).portSpeed());
        return portSpeed;
    }

    private void ifInitConfigEmpty(Multimap<DeviceId, ConnectPoint> multimap) {
        DeviceId dvcTwoId = multimap.keySet().iterator().next();
        SetGatewaysCost setGatewaysCost = new SetGatewaysCost();
        setGatewaysCost.setGwId(dvcTwoId.toString());
        setGatewaysCost.setGwCost(deviceService.getPort(dvcTwoId,
                multimap.get(dvcTwoId).iterator().next().port()).portSpeed());
        setGatewaysCosts.add(setGatewaysCost);
        log.info("Gateway Cost set {}", setGatewaysCost.getGwCost());
    }

}
