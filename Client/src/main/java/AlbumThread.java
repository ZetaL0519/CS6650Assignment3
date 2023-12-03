import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.api.LikeApi;
import io.swagger.client.model.AlbumsProfile;
import io.swagger.client.model.ImageMetaData;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class AlbumThread extends Thread {
    private static final int MAX_RETRIES = 5;
    private static final String LIKE = "like";
    private static final String DISLIKE = "dislike";
    private final Integer numberOfRequests;
    private final CountDownLatch doneSignal;
    private final String serverIP;

    public AlbumThread(Integer numberOfRequests, CountDownLatch doneSignal, String serverIP) {
        this.numberOfRequests = numberOfRequests;
        this.doneSignal = doneSignal;
        this.serverIP = serverIP;
    }

    @Override
    public void run() {
        ApiClient client = new ApiClient();
        client.setBasePath(serverIP);

        DefaultApi defaultApi = new DefaultApi(client);
        LikeApi likeApi = new LikeApi(client);
        AlbumsProfile albumsProfile = new AlbumsProfile().artist("w223").year("1438").title("huhu");
        File image = new File("/Users/zoelee/Documents/neu/cs6650/Assignment1/HW1Servlet/textimage.png");


        for (int i = 0; i < 2 * numberOfRequests; i++) {
            boolean postSuccess = false;
            // boolean getSuccess = false;

            int postRetries = 1;
            // int getRetries = 1;
            long PoststartTime = System.currentTimeMillis();
            String albumID = null;

            while (!postSuccess && postRetries <= MAX_RETRIES) {
                try {
                    // Send POST request
                    ImageMetaData postRes = defaultApi.newAlbum(image, albumsProfile);
                    albumID = postRes.getAlbumID();
                    if (albumID != null) {
                        long PostendTime = System.currentTimeMillis();
                        MetricsReport.recordList.add(new Record(PoststartTime, "POST",
                                PostendTime - PoststartTime, 200));
                        Client.counter.incrementSuccessfulReq(1); // Count the POST request as successful
                        postSuccess = true;
                    }
//                    int statusCode = postRes.getStatusCode();
//                    System.out.println(statusCode);
//                    if (statusCode == 200 || statusCode == 201) {
//                        long PostendTime = System.currentTimeMillis();
//                        MetricsReport.recordList.add(new Record(PoststartTime, "POST",
//                                PostendTime - PoststartTime, statusCode));
//                        Client.counter.incrementSuccessfulReq(1); // Count the POST request as successful
//                        postSuccess = true;
//                    }
                } catch (ApiException e) {
                    postRetries++;
                    System.out.println("Exception when calling DefaultAPI (POST). Tried " + postRetries + " times");
                    e.printStackTrace();
                }
            }

//            long getStartTime = System.currentTimeMillis();
//            while (!getSuccess && getRetries <= MAX_RETRIES) {
//                try {
//                    ApiResponse<AlbumInfo> getRes = api.getAlbumByKeyWithHttpInfo("1");
//                    if (getRes.getStatusCode() == 200 || getRes.getStatusCode() == 201) {
//                        long getEndTime = System.currentTimeMillis();
//                        MetricsReport.recordList.add(new Record(getStartTime, "GET",
//                                getEndTime - getStartTime, getRes.getStatusCode()));
//                        Client.counter.incrementSuccessfulReq(1); // Count the GET request as successful
//                        getSuccess = true;
//                    }
//                } catch (ApiException e) {
//                    getRetries++;
//                    System.out.println("Exception when calling DefaultAPI (GET). Tried " + getRetries + " times");
//                    e.printStackTrace();
//                }
//            }
            // If either POST or GET requests fail all retry attempts, count it as a failed request
            if (!postSuccess) {
                System.out.println("fail");
                Client.counter.incrementFailedReq(4); // Count POST failure as a failed request
            } else {
                postReview(likeApi, LIKE, albumID);
                postReview(likeApi, LIKE, albumID);
                postReview(likeApi, DISLIKE, albumID);
            }


//            if (!getSuccess) {
//                Client.counter.incrementFailedReq(1); // Count GET failure as a failed request
//            }
        }

        doneSignal.countDown(); //signal that this thread has completed
    }

    public static void postReview(LikeApi api, String review, String albumID) {
        boolean postSuccess = false;
        int retries = 1;
        long start = System.currentTimeMillis();
        while (retries <= MAX_RETRIES) {
            try {
                ApiResponse<Void> response = api.reviewWithHttpInfo(review, albumID);
                int statusCode = response.getStatusCode();
                if (statusCode == 200 || statusCode == 201) {
                    long end = System.currentTimeMillis();
                    MetricsReport.recordList.add(new Record(start, "POST",
                            end - start , statusCode));
                    Client.counter.incrementSuccessfulReq(1); // Count the POST request as successful
                }
                postSuccess = true;
                break;
            } catch (ApiException e) {
                retries++;
                System.out.println("Exception when calling LIKEAPI (REVIEW). Tried " + retries + " times");
                e.printStackTrace();
            }
        }

        if (!postSuccess) {
            System.out.println("fail");
            Client.counter.incrementFailedReq(1); // Count POST failure as a failed request
        }
    }
}