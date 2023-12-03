import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.zaxxer.hikari.HikariDataSource;
import io.swagger.client.model.ImageMetaData;
import org.apache.commons.io.IOUtils;

import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.UUID;
import java.util.stream.Collectors;

@WebServlet(name = "AlbumServlet", value = "/albums/*")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 10,    // 10 MB
        maxFileSize = 1024 * 1024 * 50,        // 50 MB
        maxRequestSize = 1024 * 1024 * 100)    // 100 MB
public class AlbumServlet extends HttpServlet {
    private DatabasePool databasePool;
    private HikariDataSource connectionPool;
    private String insert_sql;

    @Override
    public void init() throws ServletException {
        this.databasePool = new DatabasePool();
        this.connectionPool = databasePool.getConnectionPool();
        insert_sql = "INSERT INTO albumInfo (AlbumID, ImageData, AlbumProfile, NumberOfLikes, NumberOfDislikes) VALUES (?, ?, ?, 0, 0)";
    }

//    @Override
//    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
//        res.setContentType("application/json");
//        res.setCharacterEncoding("UTF-8");
//        String urlPath = req.getPathInfo();
//        Gson gson = new Gson();
//
//        if (urlPath == null || urlPath.isEmpty()) {
//            res. setStatus(HttpServletResponse.SC_NOT_FOUND);
//            res.getWriter().write(gson.toJson(new ResponseMsg("Missing Parameter")));
//            return;
//        }
//
//        String[] urlParts = urlPath.split("/");
//        if (urlPath.length() != 2) {
//            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
//            ResponseMsg msg = new ResponseMsg("Wrong URL Address");
//            res.getWriter().write(gson.toJson(msg));
//            return;
//        }
//
//        String albumID = urlParts[1];
//
//        try (Connection connection = this.connectionPool.getConnection()) {
//            // Retrieve data from the database
//            String sql = "SELECT artist, title, year FROM albums WHERE album_id = ?";
//            try (PreparedStatement statement = connection.prepareStatement(sql)) {
//                statement.setString(1, albumID);
//
//                try (ResultSet resultSet = statement.executeQuery()) {
//                    if (resultSet.next()) {
//                        // Construct response JSON
//                        String jsonResponse = String.format("{\"artist\": \"%s\", \"title\": \"%s\", \"year\": \"%s\"}",
//                                resultSet.getString("artist"),
//                                resultSet.getString("title"),
//                                resultSet.getString("year"));
//                        // Send response
//                        res.setStatus(HttpServletResponse.SC_OK);
//                        res.setContentType("application/json");
//                        res.getWriter().write(jsonResponse);
//                    } else {
//                        // If albumID not found
//                        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
//                        res.setContentType("application/json");
//                        res.getWriter().write(gson.toJson(new ResponseMsg("Album not found")));
//                    }
//                }
//                try {
//                    connection.close();
//                    statement.close();
//                } catch (SQLException se) {
//                    se.printStackTrace();
//                }
//            }
//        } catch (Exception e) {
//            // Handle exceptions and send error response
//            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            res.setContentType("application/json");
//            res.getWriter().write(gson.toJson(new ResponseMsg("Bad Connection to Database")));
//            e.printStackTrace();
//        }
//    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        Gson gson = new Gson();

        if (req.getPathInfo() != null) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write(gson.toJson(new ResponseMsg("Wrong URL Address")));
        }

        // check content type to be multipart/form-data
        if (!req.getContentType().startsWith("multipart/form-data")) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write(gson.toJson(new ResponseMsg("Invalid content type")));
            return;
        }

        Part imagePart = req.getPart("image");
        byte[] imageData = null;
        if (imagePart != null) {
            try (InputStream is = imagePart.getInputStream()) {
                imageData = IOUtils.toByteArray(is);
            }
        }

        Part albumProfilePart = req.getPart("profile");
        if (albumProfilePart == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write(gson.toJson(new ResponseMsg("Missing Profile")));
            return;
        }

        AlbumInfo newAlbumInfo = extractAlbumInfo(albumProfilePart, gson);
        String profile_json = gson.toJson(newAlbumInfo);
        String uuid = String.valueOf(UUID.randomUUID());
//        AlbumProfileDao albumProfileDao = new AlbumProfileDao();
//        albumProfileDao.createAlbumProfile(artist, title, year, imageData);
        try (Connection connection = this.connectionPool.getConnection()){
            PreparedStatement stmt = connection.prepareStatement(insert_sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, uuid);
            stmt.setBytes(2, imageData);
            stmt.setString(3, profile_json);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating album failed, no rows affected.");
            }
            System.out.println(affectedRows);

            if (affectedRows > 0) {
                ImageMetaData metaData = new ImageMetaData();
                metaData.setAlbumID(String.valueOf(uuid));
                metaData.setImageSize(String.valueOf(imageData.length));
                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().write(gson.toJson(metaData));
            } else {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().write("Error when posting to database");
            }
            try {
                connection.close();
                stmt.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write(gson.toJson(new ResponseMsg("Database error")));
        }
    }

    private static AlbumInfo extractAlbumInfo(Part albumProfilePart, Gson gson) throws IOException {
        AlbumInfo res = new AlbumInfo();
        albumProfilePart.getInputStream().toString();
        String albumInfoString = new BufferedReader(new InputStreamReader(
                albumProfilePart.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));

        String[] albumInfoArray = albumInfoString.split("\n");
        if (albumInfoArray.length > 1) {
            // SwaggerHub API.
            for (String line: albumInfoArray) {
                if (line.contains("artist")) {
                    res.setArtist(line.split("artist: ")[1]);
                } else if (line.contains("title")) {
                    res.setTitle(line.split("title: ")[1]);
                } else if (line.contains("year")) {
                    res.setYear(line.split("year: ")[1]);
                }
            }
        } else {
            // Postman
            try {
                res = gson.fromJson(albumInfoString, AlbumInfo.class);
            } catch (JsonSyntaxException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }

        return res;
    }
}
