package pathmanagerservice;

import java.util.Objects;

public class FlowsInfoSch {

    private long calcPathId;
    private long installedPathId;
    private String srcIP;
    private String dstIP;
    private String srcPort;
    private String dstPort;
    private double rate;
    private String endTime;
    private String startTime;

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
        if (obj instanceof FlowsInfoSch) {
            FlowsInfoSch that = (FlowsInfoSch) obj;
            return Objects.equals(srcIP, that.srcIP) &&
                    Objects.equals(dstIP, that.dstIP) &&
                    Objects.equals(srcPort, that.srcPort) &&
                    Objects.equals(dstPort, that.dstPort) &&
                    Objects.equals(rate, that.rate) &&
                    Objects.equals(calcPathId, that.calcPathId) &&
                    Objects.equals(endTime, that.endTime) &&
                    Objects.equals(startTime, that.startTime) &&
                    Objects.equals(installedPathId, that.installedPathId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcIP, dstIP, srcPort, srcPort,
                endTime, startTime, rate, installedPathId, calcPathId);
    }
}
