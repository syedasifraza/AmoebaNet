package amoebanetmanager;

import java.util.Date;
import java.util.Objects;
import java.util.Set;

public class VirtualPathID {
    private Set<Long> pathIDs;
    private Long virtPathID;
    private Date endTime;

    public Set<Long> getPathIDs() {
        return pathIDs;
    }

    public void setPathIDs(Set<Long> pathIDs) {
        this.pathIDs = pathIDs;
    }

    public Long getVirtPathID() {
        return virtPathID;
    }

    public void setVirtPathID(Long virtPathID) {
        this.virtPathID = virtPathID;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof VirtualPathID) {
            VirtualPathID that = (VirtualPathID) obj;
            return Objects.equals(pathIDs, that.pathIDs) &&
                    Objects.equals(virtPathID, that.virtPathID)
                    && Objects.equals(endTime, that.endTime);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathIDs, virtPathID, endTime);
    }

}
