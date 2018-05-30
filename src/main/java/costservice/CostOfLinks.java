package costservice;

import org.onosproject.net.Link;

import java.util.Map;

/**
 * Created by root on 5/1/17.
 */
public class CostOfLinks {
    private static Map<Link, Long> cost;
    private static Map<String, Long> dtnsCost;
    private Map<String, Long> gwCost;

    public static Map<Link, Long> getCost() {
        return cost;
    }

    public void setCost(Map<Link, Long> cost) {
        this.cost = cost;
    }

    public Map<String, Long> getGwCost() {
        return gwCost;
    }

    public void setGwCost(Map<String, Long> gwCost) {
        this.gwCost = gwCost;
    }

}
