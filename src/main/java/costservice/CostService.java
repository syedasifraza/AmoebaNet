package costservice;

import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Created by root on 5/1/17.
 */
public interface CostService {
    public long retriveCost(String src, String dst);
    public void changeCost(Set<DeviceId> devices, Double rate, boolean incORdec);
    public void setDtnsLinkCost(String dtnIp);
    public long getDtnsLinkCost(String dtnIp);
    public boolean checkDtnsLinkCost(String dtnIp);
    public void addGwCost();
    public long getGwLinkCost(String deviceId);
    public void changeGwCost(String GwId, Double rate, boolean incORdec);
    public void changeDtnCost(String DtnId, Double rate, boolean incORdec);


}
