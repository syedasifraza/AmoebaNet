package pathmanagerservice;

import java.util.Objects;

/**
 * Created by root on 6/2/17.
 */
public class FlowsInfo {

    private long calcPathId;
    private long installedPathId;
    private String srcIP;
    private String dstIP;
    private String srcPort;
    private String dstPort;
    private double rate;
    private String endTime;
    private String startTime;
    private boolean isRequiredSchedule;
    private String vlanId;
    private String oscarsVlanId;
    private String trafficType;

    public String getVlanId() {
        return vlanId;
    }

    public void setVlanId(String vlanId) {
        this.vlanId = vlanId;
    }

    public String getOscarsVlanId() {
        return oscarsVlanId;
    }

    public void setOscarsVlanId(String oscarsVlanId) {
        this.oscarsVlanId = oscarsVlanId;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public boolean isRequiredSchedule() {
        return isRequiredSchedule;
    }

    public void setRequiredSchedule(boolean requiredSchedule) {
        isRequiredSchedule = requiredSchedule;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSrcIP() {
        return srcIP;
    }

    public String getDstIP() {
        return dstIP;
    }

    public String getSrcPort() {
        return srcPort;
    }

    public String getDstPort() {
        return dstPort;
    }

    public double getRate() {
        return rate;
    }

    public void setSrcIP(String srcIP) {
        this.srcIP = srcIP;
    }

    public void setDstIP(String dstIP) {
        this.dstIP = dstIP;
    }

    public void setSrcPort(String srcPort) {
        this.srcPort = srcPort;
    }

    public void setDstPort(String dstPort) {
        this.dstPort = dstPort;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public long getInstalledPathId() {
        return installedPathId;
    }

    public void setInstalledPathId(long installedPathId) {
        this.installedPathId = installedPathId;
    }

    public long getCalcPathId() {
        return calcPathId;
    }

    public void setCalcPathId(long calcPathId) {
        this.calcPathId = calcPathId;
    }

    @Override

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FlowsInfo) {
            FlowsInfo that = (FlowsInfo) obj;
            return Objects.equals(srcIP, that.srcIP) &&
                    Objects.equals(dstIP, that.dstIP) &&
                    Objects.equals(srcPort, that.srcPort) &&
                    Objects.equals(dstPort, that.dstPort) &&
                    Objects.equals(rate, that.rate) &&
                    Objects.equals(calcPathId, that.calcPathId) &&
                    Objects.equals(endTime, that.endTime) &&
                    Objects.equals(startTime, that.startTime) &&
                    Objects.equals(vlanId, that.vlanId) &&
                    Objects.equals(oscarsVlanId, that.oscarsVlanId) &&
                    Objects.equals(trafficType, that.trafficType) &&
                    Objects.equals(isRequiredSchedule, that.isRequiredSchedule) &&
                    Objects.equals(installedPathId, that.installedPathId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcIP, dstIP, srcPort, srcPort,
                endTime, startTime, rate, installedPathId,
                calcPathId, isRequiredSchedule, vlanId,
                oscarsVlanId, trafficType);
    }
}
