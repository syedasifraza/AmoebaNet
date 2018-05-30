package costservice;

import java.util.Objects;

/**
 * Created by root on 5/31/17.
 */
public class SetGatewaysCost {
    private String gwId;
    private long gwCost;

    public void setGwId(String gwId) {
        this.gwId = gwId;
    }

    public void setGwCost(long gwCost) {
        this.gwCost = gwCost;
    }

    public String getGwId() {
        return gwId;
    }

    public long getGwCost() {
        return gwCost;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SetGatewaysCost) {
            SetGatewaysCost that = (SetGatewaysCost) obj;
            return Objects.equals(gwId, that.gwId) &&
                    Objects.equals(gwCost, that.gwCost);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gwId, gwCost);
    }
}
