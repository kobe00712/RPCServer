package monitor.influxdbClient.impl;

import monitor.influxdbClient.InfluxDBClient;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 注意多线程 batchWrite 与 write 时需要注意同步
 */
public class InfluxClientImpl implements InfluxDBClient {

    private InfluxDB influxDB;

    private String url;
    private String username;
    private String password;
    private String dbName;
    private boolean testOnWrite = false;
    private int timeBetweenTestSeconds = -1;

    private long lastCheckTimestamp;
    private final int RETRY_PING_TIMES = 3;

    /**
     * 仅Spring容器启动时调用
     */
    private static final Logger logger = LoggerFactory.getLogger(InfluxClientImpl.class);
    public void initConnection() {
        logger.info("initConnection() : init connection to {}.", url);
        influxDB = InfluxDBFactory.connect(url, username, password);
        influxDB.setLogLevel(InfluxDB.LogLevel.NONE);
        boolean connectionStatus = ping(RETRY_PING_TIMES);
        lastCheckTimestamp = System.currentTimeMillis();
        if (connectionStatus) {
            influxDB.createDatabase(dbName);
        } else {
            logger.info("initDB failed");
        }
    }

    private boolean ping(int retries) {
        boolean connected = false;
        for (int i = 0; i < retries; i++) {
            Pong pong = influxDB.ping();
            if (pong != null) {
                logger.debug("ping() : ping ok : version = {}.", pong.getVersion());
                connected = true;
                break;
            } else {
                logger.info("ping() : ping rpcServer.server {} failed", url);
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                }
            }
        }
        return connected;
    }

    public void batchWrite(List<Point> points) {
        if (writable()) {
            BatchPoints batches = BatchPoints.database(dbName)
                    .retentionPolicy("autogen")
                    .build();
//            points.forEach(batches:: point);
//
            influxDB.write(batches);
        }
    }

    public void write(Point point) {
        if (writable()) {
            influxDB.write(dbName, "autogen", point);
            logger.info("influxDB write data success");
        }
    }



    private boolean writable() {
        if (testOnWrite) {//testOnWrite = true,则需要检测间隔时间
            long current = System.currentTimeMillis();
            if (current - lastCheckTimestamp >= timeBetweenTestSeconds * 1000) {
                boolean status = ping(RETRY_PING_TIMES);
                lastCheckTimestamp = current;
                return status;
            }
        }
        return true;
    }

    public void setInfluxDB(InfluxDB influxDB) {
        this.influxDB = influxDB;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public void setTestOnWrite(boolean testOnWrite) {
        this.testOnWrite = testOnWrite;
    }

    public void setTimeBetweenTestSeconds(int timeBetweenTestSeconds) {
        this.timeBetweenTestSeconds = timeBetweenTestSeconds;
    }

    public void setLastCheckTimestamp(long lastCheckTimestamp) {
        this.lastCheckTimestamp = lastCheckTimestamp;
    }
}
