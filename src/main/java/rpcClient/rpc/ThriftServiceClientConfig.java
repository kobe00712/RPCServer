package rpcClient.rpc;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zookeeper.ThriftServer;

/*
 * 获取连接池中的一个thrift client
 */
public class ThriftServiceClientConfig{
	private static final Logger logger = LoggerFactory.getLogger(ThriftServiceClientConfig.class);
	private Integer maxActive = 500;// 最大活跃连接数
	private Integer minActive = 50;// 最小活跃连接数
	public Integer getMinActive() {
		return minActive;
	}
	public void setMinActive(Integer minActive) {
		this.minActive = minActive;
	}

	private Integer idleTime = 10000;
	private Integer timeout = 10;
	private ThriftServer serverAddressProvider;
	private ThriftClientPool clientPool;

	public void load() throws Exception {
		//异步执行
		thriftThread thread = new thriftThread();
		thread.start();
	}

	private class thriftThread extends Thread {
		public thriftThread() {
			super("async thread for thrift");
		}

		public void run() {
			try {
				logger.info(Thread.currentThread().getName());
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				// 加载Client.Factory类 // *) 内部类的表, 不用'.', 而使用'$'分割
				String service = "com.sina.algo.engine.idl.AlgoEngine";
				@SuppressWarnings("unchecked")
				Class<TServiceClientFactory<TServiceClient>> fi = (Class<TServiceClientFactory<TServiceClient>>) classLoader.loadClass(service + "$rpcClient.Client$Factory");
				TServiceClientFactory<TServiceClient> clientFactory = fi.newInstance();
				GenericObjectPoolConfig poolConfig = getThriftPoolConfig();
				//获取client连接池
				ThriftClientPool clientPool = new ThriftClientPool(poolConfig, serverAddressProvider, clientFactory, timeout);
				setClientPool(clientPool);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		if (serverAddressProvider != null) {
			serverAddressProvider.close();
		}
	}
	/*
	 * 满足高并发，maxwait time
	 */
	private GenericObjectPoolConfig getThriftPoolConfig() {
		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
		// controls the maximum number of objects that can be allocated by the
		// pool (checked out to clients, or idle awaiting checkout) at a given
		// time.When non-positive, there is no limit to the number of objects
		// that can be managed by the pool at one time. When maxActive is
		// reached, the pool is said to be exhausted.
		//最大激活数
		poolConfig.setMaxTotal(maxActive);
		// controls the maximum number of objects that can sit idle in the pool
		// at any time. When negative, there is no limit to the number of
		// objects that may be idle at one time.
		poolConfig.setMaxIdle(maxActive);
//		poolConfig.maxIdle = maxActive;
		poolConfig.setMinIdle(minActive);
//		poolConfig.minIdle = 0;
		// borrowObject() will block (invoke Object.wait()) until a new or idle
		// object is available. If a positive maxWait value is supplied, then
		// borrowObject() will block for at most that many milliseconds, after
		// which a NoSuchElementException will be thrown. If maxWait is
		// non-positive, the borrowObject() method will block indefinitely.
		//设置原则 qt<N,请求失败时，会返回异常，利用保底的策略
		poolConfig.setMaxWaitMillis(10);
//		poolConfig.maxWait = 1;
		poolConfig.setBlockWhenExhausted(true);
//		poolConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
		// indicates how long the eviction thread should sleep before "runs" of
		// examining idle objects. When non-positive, no eviction thread will be
		// launched.
		poolConfig.setTimeBetweenEvictionRunsMillis(idleTime*2L);
//		poolConfig.timeBetweenEvictionRunsMillis = idleTime * 2L;
		// specifies the minimum amount of time that an object may sit idle in
		// the pool before it is eligible for eviction due to idle time. When
		// non-positive, no object will be dropped from the pool due to idle
		// time alone. This setting has no effect unless
		// timeBetweenEvictionRunsMillis > 0.
		poolConfig.setMinEvictableIdleTimeMillis(idleTime);
//		poolConfig.minEvictableIdleTimeMillis = idleTime;
		// determines the number of objects examined in each run of the idle
		// object evictor.
		poolConfig.setNumTestsPerEvictionRun(8);
//		poolConfig.numTestsPerEvictionRun = 8;
		// indicates whether or not idle objects should be validated using the
		// factory's PoolableObjectFactory.validateObject(java.lang.Object)
		// method. Objects that fail to validate will be dropped from the pool.
		// This setting has no effect unless timeBetweenEvictionRunsMillis > 0.
		// The default setting for this parameter is false.
		poolConfig.setTestOnBorrow(false);
		poolConfig.setTestOnCreate(false);
		poolConfig.setTestWhileIdle(true);
//		poolConfig.testWhileIdle = true;// false;// true;
		// the pool will attempt to validate each object before it is returned
		// from the borrowObject() method.Objects that fail to validate will be
		// dropped from the pool, and a different object will be borrowed.
		// 借出对象时测试
//		poolConfig.testOnBorrow = false;// false;// true;
		// the pool will attempt to validate each object before it is returned
		// to the pool in the returnObject(java.lang.Object) method.Objects that
		// fail to validate will be dropped from the pool.
		//返回对象时测试
		poolConfig.setTestOnReturn(false);
//		poolConfig.testOnReturn = false;// true;
		return poolConfig;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public ThriftClientPool getClientPool() {
		return clientPool;
	}

	public void setClientPool(ThriftClientPool clientPool) {
		this.clientPool = clientPool;
	}
	public void setMaxActive(Integer maxActive) {
		this.maxActive = maxActive;
	}

	public void setIdleTime(Integer idleTime) {
		this.idleTime = idleTime;
	}

	public void setServerAddressProvider(ThriftServer serverAddressProvider) {
		this.serverAddressProvider = serverAddressProvider;
	}


}
