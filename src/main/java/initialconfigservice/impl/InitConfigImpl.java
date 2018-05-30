package initialconfigservice.impl;

import com.google.common.collect.*;
import initialconfigservice.Constants;
import initialconfigservice.InitAppConfig;
import initialconfigservice.InitConfigService;
import org.apache.felix.scr.annotations.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.*;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(immediate = true)
@Service
public class InitConfigImpl implements InitConfigService {


    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    private InitAppConfig initAppConfig = null;

    private SetMultimap<String, String> ifacesOfGateway = HashMultimap.create();
    private SetMultimap<String, String> oldIfacesOfGateway = HashMultimap.create();
    private SetMultimap<String, Interface> gatewayIfaces = HashMultimap.create();

    private final InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();

    private ConfigFactory<ApplicationId, InitAppConfig> gatewayConfigFactory =
            new ConfigFactory<ApplicationId, InitAppConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, InitAppConfig.class, Constants.CONFIG) {
                @Override
                public InitAppConfig createConfig() {
                    return new InitAppConfig();
                }
            };

    private ApplicationId gatewayAppId;

    @Activate
    protected void active() {
        configService.addListener(configListener);
        gatewayAppId = coreService.registerApplication(Constants.CONFIG_APP);
        registry.registerConfigFactory(gatewayConfigFactory);
        loadConfiguration();
        log.info("Started");
    }

    @Deactivate
    protected  void deactive() {
        registry.unregisterConfigFactory(gatewayConfigFactory);
        configService.removeListener(configListener);
        log.info("Stopped");
    }

    //returns gateways and its connection point information
    @Override
    public Multimap<DeviceId, ConnectPoint> gatewaysInfo() {
        SetMultimap<String, Interface> networkInterfaces = ImmutableSetMultimap.copyOf(gatewayIfaces);

        Multimap<DeviceId, ConnectPoint> multimap = ArrayListMultimap.create();

        networkInterfaces.asMap().forEach((gatewayName, interfaces) -> {
            interfaces.forEach(intf -> {

                multimap.put(intf.connectPoint().deviceId(), intf.connectPoint());


            });
        });
        log.info("Gateways Info: {}", multimap);
        return multimap;
    }

    @Override
    public ApplicationId getAppId() {
        return gatewayAppId;
    }


    private void loadConfiguration() {
        loadAppId();

        initAppConfig = configService.getConfig(gatewayAppId, InitAppConfig.class);

        if (initAppConfig == null) {
            log.warn(Constants.CONFIG_NULL);
            initAppConfig = configService.addConfig(gatewayAppId, InitAppConfig.class);
        }

        oldIfacesOfGateway = ifacesOfGateway;
        ifacesOfGateway = getConfigInterfaces();
        gatewayIfaces = getConfigCPointsFromIfaces();

        log.debug(Constants.CONFIG_CHANGED, ifacesOfGateway);
    }


    private void loadAppId() {
        gatewayAppId = coreService.getAppId(Constants.CONFIG_APP);
        if (gatewayAppId == null) {
            log.warn(Constants.APP_ID_NULL);
        }
    }


    private SetMultimap<String, String> getConfigInterfaces() {
        SetMultimap<String, String> confIntfByGateway =
                HashMultimap.create();

        initAppConfig.gateways().forEach(gateway -> {
            if (gateway.ifaces().isEmpty()) {
                confIntfByGateway.put(gateway.name(), Constants.EMPTY);
            } else {
                gateway.ifaces().forEach(iface -> confIntfByGateway.put(gateway.name(), iface));
            }
        });

        return confIntfByGateway;
    }


    private SetMultimap<String, Interface> getConfigCPointsFromIfaces() {
        log.debug(Constants.CHECK_CONFIG);

        SetMultimap<String, Interface> confCPointsByIntf =
                HashMultimap.create();

        ifacesOfGateway.entries().forEach(gateway -> {
            interfaceService.getInterfaces()
                    .stream()
                    .filter(intf -> intf.ipAddressesList().isEmpty())
                    .filter(intf -> intf.name().equals(gateway.getValue()))
                    .forEach(intf -> confCPointsByIntf.put(gateway.getKey(), intf));
        });

        return confCPointsByIntf;
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass() == CONFIG_CLASS) {
                log.debug(Constants.NET_CONF_EVENT, event.configClass());
                switch (event.type()) {
                    case CONFIG_ADDED:
                    case CONFIG_UPDATED:
                    case CONFIG_REMOVED:
                        loadConfiguration();
                        break;
                    default:
                        break;
                }
            }
        }
    }
}