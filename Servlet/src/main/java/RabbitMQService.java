import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeoutException;

public class RabbitMQService {
    private final static int CHANNEL_POOL_SIZE = 120;

    public RabbitMQService() {}

    public ConcurrentLinkedDeque<Channel> createChannelPool(String host, String exchangeName, String exchangeType) {
        ConcurrentLinkedDeque<Channel> channelPool = new ConcurrentLinkedDeque<>();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);

        try {
            Connection connection = factory.newConnection();
            for (int i = 0; i < CHANNEL_POOL_SIZE; i++) {
                Channel channel = connection.createChannel();
                channel.exchangeDeclare(exchangeName, exchangeType);
                channelPool.add(channel);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }

        return channelPool;
    }

    public void closeChannelPool(ConcurrentLinkedDeque<Channel> channels) {
        for (Channel channel: channels) {
            try {
                channel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
