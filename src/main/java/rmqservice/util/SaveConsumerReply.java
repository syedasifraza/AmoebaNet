package rmqservice.util;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

/**
 * Created by root on 5/16/17.
 */
public class SaveConsumerReply {
    private static AMQP.BasicProperties replyProp;
    private static AMQP.BasicProperties properties;
    private static Envelope envelope;

    public void setReplyProp(AMQP.BasicProperties replyProp) {
        this.replyProp = replyProp;
    }

    public void setProperties(AMQP.BasicProperties properties) {
        this.properties = properties;
    }

    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }

    public static AMQP.BasicProperties getReplyProp() {
        return replyProp;
    }

    public static AMQP.BasicProperties getProperties() {
        return properties;
    }

    public static Envelope getEnvelpe() {
        return envelope;
    }
}
