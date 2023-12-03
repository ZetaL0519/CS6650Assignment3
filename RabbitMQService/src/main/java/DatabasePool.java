import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabasePool {
    private final HikariDataSource connectionPool;

    public DatabasePool() {
        this.connectionPool = this.connect();
    }

    private HikariDataSource connect() {
        HikariDataSource connectionPool = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DatabaseConnectionConstants.dbUrl);
            config.setUsername(DatabaseConnectionConstants.username);
            config.setPassword(DatabaseConnectionConstants.password);
            config.setMinimumIdle(50);
            config.setMaximumPoolSize(100);

            connectionPool = new HikariDataSource(config);
        } catch (ClassNotFoundException exp) {
            System.err.println("JDBC Driver not found");
        }
        return connectionPool;
    }

    public HikariDataSource getConnectionPool() {
        return connectionPool;
    }

    public void close() {
        this.connectionPool.close();
    }
}