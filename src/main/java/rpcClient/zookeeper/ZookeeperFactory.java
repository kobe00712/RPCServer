package rpcClient.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class ZookeeperFactory  {
	private String zkHosts;
	// session超时
	private int sessionTimeout = 30000;
	//连接超时
	private int connectionTimeout = 30000;
	// 共享一个zk链接
	private boolean singleton = true;
	// 全局path前缀,常用来区分不同的应用
	private String namespace;

	private CuratorFramework zkClient;

	public void setZkHosts(String zkHosts) {
		this.zkHosts = zkHosts;
	}

	public void setSessionTimeout(int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public void setZkClient(CuratorFramework zkClient) {
		this.zkClient = zkClient;
	}

	public CuratorFramework getObject() throws Exception {
		if (singleton) {
			if (zkClient == null) {
				zkClient = create();
				zkClient.start();
			}
			return zkClient;
		}
		return create();
	}

	public CuratorFramework create() throws Exception {
		return create(zkHosts, sessionTimeout, connectionTimeout, namespace);
	}
	//node: /namespace/service/version/
	public static CuratorFramework create(String connectString, int sessionTimeout, int connectionTimeout, String namespace) {
		CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
		return builder.connectString(connectString).sessionTimeoutMs(sessionTimeout).connectionTimeoutMs(connectionTimeout).retryPolicy(new ExponentialBackoffRetry(1000, Integer.MAX_VALUE))
				.canBeReadOnly(true).namespace(namespace).defaultData(null).build();
	}
	public void close() {
		if (zkClient != null) {
			zkClient.close();
		}
	}
}
