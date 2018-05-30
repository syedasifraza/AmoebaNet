package initialconfigservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;

import java.util.Set;

public class InitAppConfig extends Config<ApplicationId> {
    private static final String GATEWAYS = "gateways";
    private static final String NAME = "name";
    private static final String INTERFACE = "interfaces";

    public Set<InitConfig> gateways() {
        Set<InitConfig> gateways = Sets.newHashSet();

        JsonNode gatewayNode = object.get(GATEWAYS);

        if (gatewayNode == null) {
            return gateways;
        }

        gatewayNode.forEach(jsonNode -> {
            String name = jsonNode.get(NAME).asText();

            Set<String> ifaces = Sets.newHashSet();
            JsonNode gatewayIfaces = jsonNode.path(INTERFACE);
            if (gatewayIfaces.toString().isEmpty()) {
                gatewayIfaces = ((ObjectNode) jsonNode).putArray(INTERFACE);
            }
            gatewayIfaces.forEach(ifacesNode -> ifaces.add(ifacesNode.asText()));


            gateways.add(new InitConfig(name,
                                     ifaces));
        });

        return gateways;
    }

}
