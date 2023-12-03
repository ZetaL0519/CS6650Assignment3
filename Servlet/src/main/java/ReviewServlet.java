import com.google.gson.Gson;
import com.rabbitmq.client.Channel;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@WebServlet(name="ReviewServlet", value="/review/*")
public class ReviewServlet extends HttpServlet {
    private final static String EXCHANGE_NAME = "REVIEW";
    private final RabbitMQService rabbitMQService = new RabbitMQService();
    private ConcurrentLinkedDeque<Channel> channelPool;

    @Override
    public void init() {
        String HOST = "";
        String EXCHANGE_TYPE = "direct";
        channelPool = this.rabbitMQService.createChannelPool(HOST, EXCHANGE_NAME, EXCHANGE_TYPE);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        String urlPath = req.getPathInfo();

        if (urlPath == null || urlPath.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("Missing Parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");

        String action = urlParts[1];
        String albumIdString = urlParts[2];
        if (!action.equals("like") && !action.equals("dislike")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid or missing inputs");
            return;
        }

        Map<String, String> message = new HashMap<>();
        message.put("albumId", albumIdString);
        message.put("action", action);
        String messageToSend = new Gson().toJson(message);

        Channel channel = null;
        try {
            channel= this.channelPool.removeFirst();
            channel.basicPublish(EXCHANGE_NAME, action, null, messageToSend.getBytes(StandardCharsets.UTF_8));

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("Review sent");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (channel != null) {
                this.channelPool.add(channel);
            }
        }
    }

    @Override
    public void destroy() {
        this.rabbitMQService.closeChannelPool(this.channelPool);
    }
}
