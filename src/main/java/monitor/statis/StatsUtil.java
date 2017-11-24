/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package monitor.statis;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import monitor.influxdbClient.InfluxDBClient;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author wangfei11
 * @version
 */
public class StatsUtil {
    private static final Logger logger = LoggerFactory.getLogger(StatsUtil.class);
    public static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    protected static final ConcurrentMap<String, AccessStatisticItem> accessStatistics = new ConcurrentHashMap<String, AccessStatisticItem>();
    private static InfluxDBClient influxDBClient;
    public static void staticRedis()
    {
        logger.info("begin to statics accessTime");
        // access statistic
        logAccessStatistic(true);
        logger.info("end to statice accessTime");
    }

    public static void accessStatistic(String name, long currentTimeMillis, long costTimeMillis,long bizProcessTime) {
        if (name == null || name.isEmpty()) {
            return;
        }
        try {
            AccessStatisticItem item = getStatisticItem(name, currentTimeMillis);
            item.statistic(currentTimeMillis, costTimeMillis, bizProcessTime);
        } catch (Exception e) {
        }
    }

    public static AccessStatisticItem getStatisticItem(String name, long currentTime) {
        AccessStatisticItem item = accessStatistics.get(name);
        if (item == null) {
            accessStatistics.putIfAbsent(name, new AccessStatisticItem(name, currentTime));
            item = accessStatistics.get(name);
        }
        return item;
    }

    public static ConcurrentMap<String, AccessStatisticResult> getTotalAccessStatistic() {
        return getTotalAccessStatistic(20);
    }

    public static ConcurrentMap<String, AccessStatisticResult> getTotalAccessStatistic(int peroid) {
        if (peroid > 60) {
            throw new RuntimeException("peroid need <= " + 60);
        }
        long currentTimeMillis = System.currentTimeMillis();
        ConcurrentMap<String, AccessStatisticResult> totalResults = new ConcurrentHashMap<String, AccessStatisticResult>();
        for (Map.Entry<String, AccessStatisticItem> entry : accessStatistics.entrySet()) {
            AccessStatisticItem item = entry.getValue();
            AccessStatisticResult result = item.getStatisticResult(currentTimeMillis, 60);
            String key = entry.getKey();
            AccessStatisticResult appResult = totalResults.get(key);
            if (appResult == null) {
                totalResults.putIfAbsent(key, new AccessStatisticResult());
                appResult = totalResults.get(key);
            }
            appResult.totalCount += result.totalCount;
            appResult.bizExceptionCount += result.bizExceptionCount;
            appResult.slowCount += result.slowCount;
            appResult.costTime += result.costTime;
            appResult.bizTime += result.bizTime;
            appResult.otherExceptionCount += result.otherExceptionCount;
        }
        return totalResults;
    }

    public static void logAccessStatistic(boolean clear) {
        DecimalFormat mbFormat = new DecimalFormat("#0.00");
        long currentTimeMillis = System.currentTimeMillis();
        ConcurrentMap<String, AccessStatisticResult> totalResults = new ConcurrentHashMap<String, AccessStatisticResult>();
        for (Map.Entry<String, AccessStatisticItem> entry : accessStatistics.entrySet()) {
            AccessStatisticItem item = entry.getValue();
            AccessStatisticResult result = item.getStatisticResult(currentTimeMillis, 60);
            if (clear) {
                item.clearStatistic(currentTimeMillis, 60);
            }
            String key = entry.getKey();
            AccessStatisticResult appResult = totalResults.get(key);
            if (appResult == null) {
                totalResults.putIfAbsent(key, new AccessStatisticResult());
                appResult = totalResults.get(key);
            }
            appResult.totalCount += result.totalCount;
            appResult.bizExceptionCount += result.bizExceptionCount;
            appResult.slowCount += result.slowCount;
            appResult.costTime += result.costTime;
            appResult.bizTime += result.bizTime;
            appResult.otherExceptionCount += result.otherExceptionCount;
            Snapshot snapshot =
                    InternalMetricsFactory.getRegistryInstance(key).histogram(MetricRegistry.name(AccessStatisticItem.class, "costTimeMillis")).getSnapshot();
            setAccessStatisticsInfluxDbWrite(result,snapshot,key);
        }

        if (!totalResults.isEmpty()) {
            for (Map.Entry<String, AccessStatisticResult> entry : totalResults.entrySet()) {
                AccessStatisticResult totalResult = entry.getValue();
                Snapshot snapshot =
                        InternalMetricsFactory.getRegistryInstance(entry.getKey()).histogram(MetricRegistry.name(AccessStatisticItem.class, "costTimeMillis")).getSnapshot();
            }
        }

    }
    public enum AccessStatus {
        NORMAL, BIZ_EXCEPTION, OTHER_EXCEPTION
    }
    private static void setAccessStatisticsInfluxDbWrite(AccessStatisticResult totalResult,Snapshot snapshot,String name)
    {
        if(totalResult.totalCount==0)
        {
            return;
        }
        //influxdb 只能是int 或者double
        DecimalFormat mbFormat = new DecimalFormat("#0.00");
        Point point1 = Point.measurement(name)
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .tag("host", GetLocalIP.getLocalHostName())
                .addField("count",totalResult.totalCount)
                .addField("p75Access",snapshot.get75thPercentile())
                .addField("p95Access",snapshot.get95thPercentile())
                .addField("maxAccess",(int)snapshot.getMax())
                .addField("meanAccess",(double)totalResult.costTime/totalResult.totalCount)
                .addField("TPS",totalResult.totalCount / 60)
                .addField("slowQuery",totalResult.slowCount)
                .build();
        influxDBClient.write(point1);

    }
    void setInfluxDBClient(InfluxDBClient influxDBClient)
    {
        StatsUtil.influxDBClient = influxDBClient;
    }
}


class AccessStatisticItem {
    private String name;
    private int currentIndex;
    private AtomicInteger[] costTimes = null;
    private AtomicInteger[] bizProcessTimes = null;
    private AtomicInteger[] totalCounter = null;
    private AtomicInteger[] slowCounter = null;
    private AtomicInteger[] bizExceptionCounter = null;
    private AtomicInteger[] otherExceptionCounter = null;
    private Histogram histogram = null;
    private int length;

    public AccessStatisticItem(String name, long currentTimeMillis) {
        this(name, currentTimeMillis, 60 * 2);
    }

    public AccessStatisticItem(String name, long currentTimeMillis, int length) {
        this.name = name;
        this.costTimes = initAtomicIntegerArr(length);
        this.bizProcessTimes = initAtomicIntegerArr(length);
        this.totalCounter = initAtomicIntegerArr(length);
        this.slowCounter = initAtomicIntegerArr(length);
        this.bizExceptionCounter = initAtomicIntegerArr(length);
        this.otherExceptionCounter = initAtomicIntegerArr(length);
        this.length = length;
        this.currentIndex = getIndex(currentTimeMillis, length);
        this.histogram = InternalMetricsFactory.getRegistryInstance(name)
                        .histogram(MetricRegistry.name(AccessStatisticItem.class, "costTimeMillis"));
    }

    private AtomicInteger[] initAtomicIntegerArr(int size) {
        AtomicInteger[] arrs = new AtomicInteger[size];
        for (int i = 0; i < arrs.length; i++) {
            arrs[i] = new AtomicInteger(0);
        }

        return arrs;
    }

    /**
     * currentTimeMillis: 此刻记录的时间 (ms) costTimeMillis: 这次操作的耗时 (ms)
     *
     * @param currentTimeMillis
     * @param costTimeMillis
     * @param bizProcessTime
     */
    void statistic(long currentTimeMillis, long costTimeMillis, long bizProcessTime) {
        int tempIndex = getIndex(currentTimeMillis, length);
        if (currentIndex != tempIndex) {
            synchronized (this) {
                // 这一秒的第一条统计，把对应的存储位的数据置0
                if (currentIndex != tempIndex) {
                    reset(tempIndex);
                    currentIndex = tempIndex;
                }
            }
        }
        costTimes[currentIndex].addAndGet((int) costTimeMillis);
        bizProcessTimes[currentIndex].addAndGet((int) bizProcessTime);
        totalCounter[currentIndex].incrementAndGet();
        //慢查询
        if (costTimeMillis >= 5) {
            slowCounter[currentIndex].incrementAndGet();
        }

        histogram.update(costTimeMillis);
        InternalMetricsFactory.getRegistryInstance(name).histogram(MetricRegistry.name(AccessStatisticItem.class, "costTimeMillis"))
                .update(costTimeMillis);
    }

    private int getIndex(long currentTimeMillis, int periodSecond) {
        return (int) ((currentTimeMillis / 1000) % periodSecond);
    }

    private void reset(int index) {
        costTimes[index].set(0);
        totalCounter[index].set(0);
        bizProcessTimes[index].set(0);
        slowCounter[index].set(0);
        bizExceptionCounter[index].set(0);
        otherExceptionCounter[index].set(0);
    }

    AccessStatisticResult getStatisticResult(long currentTimeMillis, int peroidSecond) {
        long currentTimeSecond = currentTimeMillis / 1000;
        currentTimeSecond--; // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移peroidSecond
        int startIndex = getIndex(currentTimeSecond * 1000, length);
        AccessStatisticResult result = new AccessStatisticResult();

        for (int i = 0; i < peroidSecond; i++) {
            int currentIndex = (startIndex - i + length) % length;

            result.costTime += costTimes[currentIndex].get();
            result.bizTime += bizProcessTimes[currentIndex].get();
            result.totalCount += totalCounter[currentIndex].get();
            result.slowCount += slowCounter[currentIndex].get();
            result.bizExceptionCount += bizExceptionCounter[currentIndex].get();
            result.otherExceptionCount += otherExceptionCounter[currentIndex].get();

            if (totalCounter[currentIndex].get() > result.maxCount) {
                result.maxCount = totalCounter[currentIndex].get();
            } else if (totalCounter[currentIndex].get() < result.minCount || result.minCount == -1) {
                result.minCount = totalCounter[currentIndex].get();
            }
        }

        return result;
    }

    void clearStatistic(long currentTimeMillis, int peroidSecond) {
        long currentTimeSecond = currentTimeMillis / 1000;
        currentTimeSecond--; // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移peroidSecond
        int startIndex = getIndex(currentTimeSecond * 1000, length);

        for (int i = 0; i < peroidSecond; i++) {
            int currentIndex = (startIndex - i + length) % length;
            reset(currentIndex);
        }
    }
}
