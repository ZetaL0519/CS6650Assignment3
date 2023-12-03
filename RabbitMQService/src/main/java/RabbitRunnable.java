import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RabbitRunnable implements Runnable{
    private final Gson gson = new Gson();
    private final Connection connection;
    private final String exchangeName;
    private final String exchangeType;
    private final String queue;

    public RabbitRunnable(Connection connection, String exchangeName, String exchangeType, String queue) {
        this.connection = connection;
        this.exchangeName = exchangeName;
        this.exchangeType = exchangeType;
        this.queue = queue;
    }


    @Override
    public void run() {
        try {
            Channel channel = this.connection.createChannel();

            channel.exchangeDeclare(this.exchangeName, this.exchangeType);
            channel.queueDeclare(this.queue, false, false, false, null);
            channel.queueBind(this.queue, this.exchangeName, this.queue);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                processMessage(delivery, channel);
            };

            // Process messages
            boolean autoAck = true;
            channel.basicConsume(this.queue, autoAck, deliverCallback, consumerTag -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void processMessage(Delivery delivery, Channel channel) {
        try {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            JsonObject reviewJsonBody = gson.fromJson(message, JsonObject.class);

            String albumId = reviewJsonBody.get("albumId").getAsString();
            String reviewType = reviewJsonBody.get("reviewType").getAsString();

            // Increment review type value in DB
            try (java.sql.Connection connection = RabbitConsumer.connectionPool.getConnection()) {
                if (reviewType.equals("like")) {
                    addLike(connection, albumId);
                } else if (reviewType.equals("dislike")) {
                    addDislike(connection, albumId);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addLike(java.sql.Connection connection, String albumId) throws SQLException {
        String updateQuery = "UPDATE albumInfo SET numberOfLikes = numberOfLikes + 1 WHERE AlbumID = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(updateQuery);

        preparedStatement.setString(1, albumId);

        preparedStatement.executeUpdate();
    }

    public void addDislike(java.sql.Connection connection, String uuid) throws SQLException {
        String updateQuery = "UPDATE albumRequests SET numberOfDislikes = numberOfDislikes + 1 WHERE AlbumID = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(updateQuery);

        // Set values for the prepared statement
        preparedStatement.setString(1, uuid);

         preparedStatement.executeUpdate();
    }
}
