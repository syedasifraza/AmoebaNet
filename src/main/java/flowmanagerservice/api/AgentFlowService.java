package flowmanagerservice.api;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Set;

/**
 * Created by root on 5/15/17.
 */
public interface AgentFlowService {
    public void installFlows(DeviceId deviceId, PortNumber inPort, PortNumber outPort, String srcIP, String dstIP,
                             String srcPort, String dstPort, Double rate, Set<Long> fId, String trafficType,
                             String vlanId, String oscarsVlanId);

    public void removeFlowsByAppId();

    public void removePathId(DeviceId deviceId, Set<Long> flowId);
}
