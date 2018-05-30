package pathmanagerservice;

import org.onosproject.net.DeviceId;

import java.util.Objects;
import java.util.Set;

/**
 * Created by root on 5/19/17.
 */
public class PathIds {

    private final DeviceId deviceIds;
    private final Set<Long> flowIds;


    public PathIds(DeviceId deviceIds, Set<Long> flowIds) {
        this.deviceIds = deviceIds;
        this.flowIds = flowIds;

    }

    public DeviceId dvcIds() {
        return deviceIds;
    }


    public Set<Long> flwIds() {
        return flowIds;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PathIds) {
            PathIds that = (PathIds) obj;
            return Objects.equals(deviceIds, that.deviceIds) &&
                    Objects.equals(flowIds, that.flowIds);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceIds, flowIds);
    }

}
