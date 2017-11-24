package monitor.influxdbClient;

import org.influxdb.dto.Point;

import java.util.List;


/**
 * Created by wangfei11 on 21/11/26.
 */
public interface InfluxDBClient {

    void batchWrite(List<Point> points);

    void write(Point point);
}
