/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rmqservice.util;

import com.google.gson.JsonObject;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * MQ utility class for constructing server url, packet message, device message,
 * topology message and link message.
 */
public final class MQUtil {

    private static final Logger log = LoggerFactory.getLogger(MQUtil.class);

    private MQUtil() {
    }

    /**
     * Returns a JSON representation of the given topology event.
     *
     * @param  event the topology event
     * @return       the topology event json message
     */
    public static JsonObject json(TopologyEvent event) {
        Topology topology = event.subject();
        JsonObject jo = new JsonObject();
        jo.addProperty("topology_type", TopologyEvent.Type.TOPOLOGY_CHANGED.name());
        jo.addProperty("cluster_count", topology.clusterCount());
        jo.addProperty("compute_cost", topology.computeCost());
        jo.addProperty("creation_time", new Date(topology.creationTime()).toString());
        jo.addProperty("device_count", topology.deviceCount());
        jo.addProperty("link_count", topology.linkCount());
        jo.addProperty("available_time", new Date(topology.time()).toString());
        return jo;
    }

}
