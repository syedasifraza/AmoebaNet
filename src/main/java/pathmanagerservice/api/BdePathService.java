package pathmanagerservice.api;

import com.google.gson.JsonObject;

import java.util.Set;

/**
 * Created by root on 4/10/17.
 */
public interface BdePathService {
    public void calcPath(String src, String dst, String type, Long sessionId);
    public long getPathBW(String ipAddress1, String ipAddress2, String type);
    public boolean checkPathId(Long pathId);
    public long setupPath(Long pathId, String srcIP, String dstIP,
                          String srcPort, String dstPort, Double rate,
                          String vlanId, String oscarsVlanId, String trafficType,
                          String endTime, String startTime, boolean isScheduled);
    public void setupPathSchedule(Long installedPathID);
    public boolean releasePathId(Long pathId);

    public long getCalcPathID(String ipAddress1, String ipAddress2, String type);
    public void updatePath();
    public long isFlowsInstalled(long calPathId, String srcIP, String dstIP,
                                 String srcPort, String dstPort, Double rate,
                                 String vlanId, String oscarsVlanId, String trafficType);
    public void clearPathInformation();

    public JsonObject allPathInformation();
    public Set<Long> getPathIds();

    public String getEndTime(Long installedPathID);

    public String getStartTime(Long installedPathID);

    public Boolean isRequiredSchedule(Long installedPathID);

    public void changeScheduledStatus(Long installedPathID);
}
