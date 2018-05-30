package pathmanagerservice;

import org.onosproject.net.DeviceId;

import java.util.Objects;
import java.util.Set;

/**
 * Created by root on 6/1/17.
 */
public class CalcPathInfo {
    private long calcPathId;
    private boolean isPersistent;
    private String DtnIpAddress;
    private String DtnIpAddress2;
    private long availBW;
    private Set<DeviceId> deviceIds;
    private long sessionId;
    private String type;

    public boolean isPersistent() {
        return isPersistent;
    }

    public void setPersistent(boolean persistent) {
        isPersistent = persistent;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getCalcPathId() {
        return calcPathId;
    }

    public String getDtnIpAddress() {
        return DtnIpAddress;
    }

    public String getDtnIpAddress2() {
        return DtnIpAddress2;
    }

    public long getAvailBW() {
        return availBW;
    }

    public Set<DeviceId> getDeviceIds() {
        return deviceIds;
    }

    public void setCalcPathId(long calcPathId) {
        this.calcPathId = calcPathId;
    }

    public void setDtnIpAddress(String dtnIpAddress) {
        DtnIpAddress = dtnIpAddress;
    }

    public void setDtnIpAddress2(String dtnIpAddress2) {
        DtnIpAddress2 = dtnIpAddress2;
    }

    public void setAvailBW(long availBW) {
        this.availBW = availBW;
    }

    public void setDeviceIds(Set<DeviceId> deviceIds) {
        this.deviceIds = deviceIds;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof CalcPathInfo) {
            CalcPathInfo that = (CalcPathInfo) obj;
            return Objects.equals(deviceIds, that.deviceIds) &&
                    Objects.equals(calcPathId, that.calcPathId) &&
                    Objects.equals(DtnIpAddress, that.DtnIpAddress) &&
                    Objects.equals(DtnIpAddress2, that.DtnIpAddress2) &&
                    Objects.equals(type, that.type) &&
                    Objects.equals(sessionId, that.sessionId) &&
                    Objects.equals(isPersistent, that.isPersistent) &&
                    Objects.equals(availBW, that.availBW);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceIds, calcPathId, DtnIpAddress2,
                DtnIpAddress, sessionId, type, isPersistent, availBW);
    }
}
