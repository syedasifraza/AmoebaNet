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
package rmqservice.impl;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerManager;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rmqservice.api.RmqEvents;
import rmqservice.api.RmqManagerService;
import rmqservice.api.RmqMsgListener;
import rmqservice.api.RmqService;
import rmqservice.util.SaveConsumerReply;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Default implementation of {@link RmqService}.
 */
@Component(immediate = true)
@Service
public class RmqServiceImpl
        extends AbstractListenerManager<RmqEvents, RmqMsgListener>
        implements RmqService {

    private static final String E_CREATE_CHAN =
            "Error creating the RabbitMQ channel";
    private static final String E_PUBLISH_CHAN =
            "Error in publishing to the RabbitMQ channel";
    private static final Logger log = LoggerFactory.getLogger(RmqServiceImpl.class);
    private static final int RECOVERY_INTERVAL = 15000;

    private final BlockingQueue<MessageContext> msgOutQueue =
            new LinkedBlockingQueue<>(10);

    private final BlockingQueue<MessageContext> msgInQueue =
            new LinkedBlockingQueue<>(10);

    private RmqManagerService rmqManagerConsumer;

    private RmqManagerService rmqManagerPublisher;

    private String exchangeNameC;
    private String routingKeyC;
    private String queueNameC;
    private String exchangeNameP;
    private String routingKeyP;
    private String queueNameP;
    private String url;
    private String type;

    private Channel channelC;
    private Channel channelP;




    private String correlationId;

    private static final String APP_NAME = "BDE-AmoebaNet";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    //protected ExecutorService eventExecutor;

    private ApplicationId appId;
    SaveConsumerReply saveConsumerReply = new SaveConsumerReply();


    @Activate
    protected void activate(ComponentContext context) {
        eventDispatcher.addSink(RmqEvents.class, listenerRegistry);
        appId = coreService.getAppId(APP_NAME);
        //eventExecutor = newSingleThreadScheduledExecutor(
        //        groupedThreads("onos/deviceevents", "events-%d", log));
        initializeProducers(context);
        log.info("RMQ Service Provider Started");
    }

    @Deactivate
    protected void deactivate() {
        uninitializeProducers();
        eventDispatcher.removeSink(RmqEvents.class);
        //eventExecutor.shutdownNow();
        //eventExecutor = null;
        log.info("Stopped");
    }

    private void initializeProducers(ComponentContext context) {
        java.util.Properties prop = getProp(context);
        URL configUrl = null;

        File currentDirectory = new File(System.getProperty("user.home") + "/config/client_cacerts.jks");
        try {
            configUrl = currentDirectory.toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }


        //log.info("Properties {}", prop.getProperty("amqp.sender.queue.info"));
        correlationId = prop.getProperty("amqp.col.id.info");
        url = prop.getProperty("amqp.protocol.info") + "://" +
                prop.getProperty("amqp.hostname.ip.info") + ":" +
                prop.getProperty("amqp.port.info") +
                prop.getProperty("amqp.vhost.info") + "%2F";
        type = prop.getProperty("amqp.type.info");

        //consumer setting from properties file
        exchangeNameC = prop.getProperty("amqp.consumer.exchange.info");
        routingKeyC = prop.getProperty("amqp.consumer.routingkey.info");
        queueNameC = prop.getProperty("amqp.consumer.queue.info");

        //publisher setting from properties file
        exchangeNameP = prop.getProperty("amqp.publisher.exchange.info");
        routingKeyP = prop.getProperty("amqp.publisher.routingkey.info");
        queueNameP = prop.getProperty("amqp.publisher.queue.info");

        //log.info("URL {}", url);
        rmqManagerConsumer = new RmqManagerImpl(msgOutQueue, exchangeNameC,
                routingKeyC, queueNameC, url, type);
        rmqManagerPublisher = new RmqManagerImpl(msgOutQueue, exchangeNameP,
                routingKeyP, queueNameP, url, type);
            /*correlationId = "onos->rmqserver";
            exchangeName =  "onos_exchg_wr_to_rmqs";
            routingKey = "abc.zxy";
            queueName = "onos_recieve_queue";*/
            //url = "amqps://yosemite.fnal.gov:5671/%2F";


        try {
            InputStream is = configUrl.openStream();
            InputStream is1 = configUrl.openStream();
            channelC = rmqManagerConsumer.start(is);
            log.info("Consumer connection established");
            //channelP = rmqManagerPublisher.start(is1);
            log.info("Publisher connection established");
        } catch (Exception e) {
            log.error(ExceptionUtils.getFullStackTrace(e));
            throw new RuntimeException(e);
        }

        consumer();

    }

    private void uninitializeProducers() {
        log.info("RMQ Serivce Stoped");
        rmqManagerConsumer.stop();
        log.info("Consumer stop");
        rmqManagerPublisher.stop();
        log.info("Publisher Stop");
    }

    private byte[] bytesOf(JsonObject jo) {
        return jo.toString().getBytes();
    }


    @Override
    public void publish(byte[] body) {

        processAndPublishMessage(body);
    }

    /*
     * Constructs message context and publish it to rabbit mq server.
     *
     * @param body Byte stream of the event's JSON data
     */
    private void processAndPublishMessage(byte[] body) {
        Map<String, Object> props = Maps.newHashMap();
        props.put("correlation_id", correlationId);
        MessageContext mc = new MessageContext(body, props);
        try {
            msgOutQueue.put(mc);
            String message = new String(body, "UTF-8");
            log.info(" [x] Again Sent '{}'", message);
        } catch (InterruptedException | UnsupportedEncodingException e) {
            log.error(ExceptionUtils.getFullStackTrace(e));
        }
        publisher();
    }

    public void publisher() {
        try {
            MessageContext input = msgOutQueue.poll();
            channelP.basicPublish(exchangeNameP, routingKeyP,
                    new AMQP.BasicProperties.Builder()
                            .correlationId(correlationId).build(),
                    input.getBody());
            String message1 = new String(input.getBody(), "UTF-8");
            log.info(" [x] Sent: '{}'", message1);
        } catch (Exception e) {
            log.error(E_PUBLISH_CHAN, e);
        }
    }

    public void consumer() {

        try {
            Consumer consumer = new DefaultConsumer(channelC) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body)
                        throws IOException {


                    AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                            .Builder()
                            .correlationId(properties.getCorrelationId())
                            .build();
                    String message = new String(body, "UTF-8");
                    log.info(" [x] Recieved: '{}'", message);
                    processRecievedMessage(body);
                    saveConsumerReply.setReplyProp(replyProps);
                    saveConsumerReply.setProperties(properties);
                    saveConsumerReply.setEnvelope(envelope);
                    post(new RmqEvents(RmqEvents.Type.RMQ_MSG_RECIEVED, message));
                    log.info(" [x] Recieved after post: '{}'", message);
                    //consumerResponse(replyProps, properties, envelope);

                }
            };
            channelC.basicConsume(queueNameC, false, consumer);
            log.info("After first message recieved");
        } catch (Exception e) {
            log.error(E_PUBLISH_CHAN, e);
        }
    }

    public void consumerResponse(AMQP.BasicProperties replyProps,
                                 AMQP.BasicProperties properties,
                                 Envelope envelope) {
        MessageContext input = msgInQueue.poll();
        byte[] body = null;
        JsonObject outer = new JsonObject();
        outer.addProperty("cmd", "sdn_response--new");
        body = bytesOf(outer);

        try {
            channelC.basicPublish("", properties.getReplyTo(),
                    replyProps, body);
            channelC.basicAck(envelope.getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("Reply sent back to client");
        log.info("Polled msg {}", input);

    }


    public void consumerResponse1(byte[] body) {

        log.info("Reply sent back to client response1");
        try {
            channelC.basicPublish("", saveConsumerReply.getProperties().getReplyTo(),
                    saveConsumerReply.getReplyProp(), body);
            channelC.basicAck(saveConsumerReply.getEnvelpe().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("Reply sent back to client response1");

    }

    @Override
    public void consumerResponse(byte[] body) {

        try {
            channelC.basicPublish("", saveConsumerReply.getProperties().getReplyTo(),
                    saveConsumerReply.getReplyProp(), body);
            channelC.basicAck(saveConsumerReply.getEnvelpe().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("Reply sent back to client");

    }


    private void processRecievedMessage(byte[] body) {
        log.info("I am in process messages size{}", msgInQueue.size());
        MessageContext mc = new MessageContext(body);
        try {
            msgInQueue.put(mc);
            String message = new String(body, "UTF-8");
            log.info(" [x] Recieved msqInQueue: '{}'", message);
        } catch (InterruptedException | UnsupportedEncodingException e) {
            log.error(ExceptionUtils.getFullStackTrace(e));
        }
    }

    @Override
    public String consume() {
        try {
            MessageContext input = msgInQueue.poll();
            return new String(input.getBody(), "UTF-8");
        } catch (Exception e) {
            log.error(E_PUBLISH_CHAN, e);
        }
        return null;

    }


    /*public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void start(InputStream filepath) {
        SSLContext c = null;
        try {
            char[] pass = "changeit".toCharArray();
            KeyStore tks = KeyStore.getInstance("JKS");
            tks.load(filepath, pass);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(tks);

            c = SSLContext.getInstance("TLSv1.2");
            c.init(null, tmf.getTrustManagers(), null);
        } catch (Exception e) {
            log.error(E_CREATE_CHAN, e);
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(RECOVERY_INTERVAL);
        factory.useSslProtocol(c);
        try {
            factory.setUri(url);
            if (executorService != null) {
                conn = factory.newConnection(executorService);
            } else {
                conn = factory.newConnection();
            }
            channel = conn.createChannel();
            channel.exchangeDeclare(exchangeName, type, true);
            /*
             * Setting the following parameters to queue
             * durable    - true
             * exclusive  - false
             * autoDelete - false
             * arguments  - null
             */
    /*        channel.queueDeclare(queueName, true, false, true, null);
            channel.queueBind(queueName, exchangeName, routingKey);
        } catch (Exception e) {
            log.error(E_CREATE_CHAN, e);
        }
        log.info("Connection started");
    }


    public void stop() {
        try {
            channel.close();
            conn.close();
        } catch (IOException e) {
            log.error("Error closing the rabbit MQ connection", e);
        } catch (TimeoutException e) {
            log.error("Timeout exception in closing the rabbit MQ connection",
                    e);
        }
    }*/


    public static java.util.Properties getProp(ComponentContext context) {
        URL configUrl;
        try {
            //configUrl = context.getBundleContext().getBundle().getResource("rabbitmq.properties");
            File currentDirectory = new File(System.getProperty("user.home") + "/config/rabbitmq.properties");
            configUrl = currentDirectory.toURL();
            log.info("URL of properties {}", currentDirectory);

        } catch (Exception ex) {
            // This will be used only during junit test case since bundle
            // context will be available during runtime only.test.xml
            File file = new File(
                    RmqServiceImpl.class.getClassLoader().getResource("rabbitmq.properties")
                            .getFile());
            try {
                configUrl = file.toURL();
            } catch (MalformedURLException e) {
                log.error(ExceptionUtils.getFullStackTrace(e));
                throw new RuntimeException(e);
            }
        }

        java.util.Properties properties;
        try {
            InputStream is = configUrl.openStream();
            properties = new java.util.Properties();
            properties.load(is);
        } catch (Exception e) {
            log.error(ExceptionUtils.getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        return properties;
    }

}
