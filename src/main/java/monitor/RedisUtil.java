package monitor;

import redis.clients.jedis.Jedis;

/**
 * Created by wangfei
 */
public class RedisUtil {
    private Jedis jedis;
    private String host;
    private int port;
    private int timeout;
    private String password;
    private String queueName;

    public void setJedis(Jedis jedis) {
        this.jedis = jedis;
    }

    public Jedis getJedis()
    {
        return this.jedis;
    }

    public void setHost(String host) {
        this.host = host;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    /**
     * Default constructor for convenient dependency injection via setters.
     */
    public RedisUtil() {
    }

    public RedisUtil(final String host, final int port, final int timeout,
                     final String password, final String queueName
                          ) throws Exception {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.password = password;
        this.queueName = queueName;
        this.jedis = createJedis();
    }

    private Jedis createJedis() throws Exception {
        final Jedis jedis = new Jedis(this.host, this.port, this.timeout);
        jedis.connect();
        if (null != this.password) {
            jedis.auth(this.password);
        }
        return jedis;
    }
}
