package initialconfigservice;

import com.google.common.collect.Multimap;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;


public interface InitConfigService {
    Class<InitAppConfig> CONFIG_CLASS = InitAppConfig.class;



    Multimap<DeviceId, ConnectPoint> gatewaysInfo();
    ApplicationId getAppId();



}
