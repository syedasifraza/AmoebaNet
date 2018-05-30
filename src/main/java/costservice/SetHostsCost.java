package costservice;

import java.util.Objects;

/**
 * Created by root on 5/30/17.
 */
public class SetHostsCost {
    private String dtnIp;
    private long dtnCost;

    public void setDtnIp(String dtnIp) {
        this.dtnIp = dtnIp;
    }

    public void setDtnCost(long dtnCost) {
        this.dtnCost = dtnCost;
    }

    public String getDtnIp() {
        return dtnIp;
    }

    public long getDtnCost() {
        return dtnCost;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SetHostsCost) {
            SetHostsCost that = (SetHostsCost) obj;
            return Objects.equals(dtnIp, that.dtnIp) &&
                    Objects.equals(dtnCost, that.dtnCost);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dtnIp, dtnCost);
    }
}
