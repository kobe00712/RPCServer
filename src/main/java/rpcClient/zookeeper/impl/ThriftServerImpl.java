package rpcClient.zookeeper.impl;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpcClient.zookeeper.ThriftServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;

/*
 * 获取server服务列表，并且选择一个最合适的发送服务。
 */
public class ThriftServerImpl implements ThriftServer {
	private static final Logger logger = LoggerFactory.getLogger(ThriftServerImpl.class);
	// 注册服务
	private String service;
	private PathChildrenCache cachedPath;
	private CuratorFramework zkClient;
	// 用来保存当前provider所接触过的地址记录
	// 当zookeeper集群故障时,可以使用trace中地址,作为"备份"
	//用来保存获取得到的thrift服务器IP和port
	private final List<InetSocketAddress> container = new ArrayList<InetSocketAddress>();
	//用来随机加权
	private Queue<InetSocketAddress> inner = new LinkedList<InetSocketAddress>();

	private Object lock = new Object();
	// 默认权重
	private static final Integer DEFAULT_WEIGHT = 1;

	public void setService(String service) {
		this.service = service;
	}


	public ThriftServerImpl() {
	}

	public ThriftServerImpl(CuratorFramework zkClient) {
		this.zkClient = zkClient;
	}

	public void setZkClient(CuratorFramework zkClient) {
		this.zkClient = zkClient;
	}
	/*
	 * (non-Javadoc)
	 * 这个方法将在所有的属性被初始化后调用,初始化操作
	 */
	public void zkStart() throws Exception {
		// 如果zk尚未启动,则启动
		if (zkClient.getState() == CuratorFrameworkState.LATENT) {
			zkClient.start();
		}
		if(zkClient.checkExists().forPath(getServicePath())==null)
		{
			logger.info("begin to add listener to Node");
			zkClient.create().creatingParentsIfNeeded().forPath(getServicePath());
			logger.info("end to add listener to Node");
		}
		buildPathChildrenCache(zkClient, getServicePath(), true);
		cachedPath.start(StartMode.POST_INITIALIZED_EVENT);
	}

	private String getServicePath(){
		return "/" + service;
	}
	/*
	 * 持续watcher节点,并将节点的数据变更即时的在本地反应出来.recpise提供了PathChildrenCache
	 */
	private void buildPathChildrenCache(final CuratorFramework client, String path, Boolean cacheData) throws Exception {
		cachedPath = new PathChildrenCache(client, path, cacheData);
		cachedPath.getListenable().addListener(new PathChildrenCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
				PathChildrenCacheEvent.Type eventType = event.getType();
				switch (eventType) {
				case CONNECTION_RECONNECTED:
					logger.info("Connection is reconection.");
					break;
				case CONNECTION_SUSPENDED:
					logger.info("Connection is suspended.");
					return;
				case CONNECTION_LOST:
					logger.warn("Connection error,waiting...");
					return;
				default:
					//
				}
				// 任何节点的时机数据变动,都会rebuild,此处为一个"简单的"做法.
				cachedPath.rebuild();
				rebuild();
			}

			protected void rebuild() throws Exception {
				List<ChildData> children = cachedPath.getCurrentData();
				if (children == null || children.isEmpty()) {
					// 有可能所有的thrift server都与zookeeper断开了链接
					// 但是,有可能,thrift client与thrift server之间的网络是良好的
					// 因此此处是否需要清空container,是需要多方面考虑的.
					container.clear();
					logger.error("thrift rpcServer.server-cluster error....");
					return;
				}
				List<InetSocketAddress> current = new ArrayList<InetSocketAddress>();
				String path = null;
				for (ChildData data : children) {
					path = data.getPath();
					logger.debug("get path:"+path);
					byte[] address = data.getData();
					String Serveraddress = new String(address, "utf-8");
					logger.debug("get serviceAddress:"+ Serveraddress);
					current.addAll(transfer(Serveraddress));
					//trace.add(Serveraddress);
				}
				Collections.shuffle(current);
				synchronized (lock) {
					container.clear();
					container.addAll(current);
					inner.clear();
					inner.addAll(current);

				}
			}

		});
	}

	private List<InetSocketAddress> transfer(String address) {
		String[] hostname = address.split(":");
		Integer weight = DEFAULT_WEIGHT;
		if (hostname.length == 3) {
			weight = Integer.valueOf(hostname[2]);
		}
		String ip = null;
		String hostName = hostname[0];
		InetAddress[] ipList = null;
		try {
			ipList = InetAddress.getAllByName(hostName);
		
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Integer port = Integer.valueOf(hostname[1]);
		List<InetSocketAddress> result = new ArrayList<InetSocketAddress>();
		// 根据优先级，将ip：port添加多次到地址集中，然后随机取地址实现负载
		for (int i = 0; i < weight; i++) {
			for(int j=0;j<ipList.length;j++)
			{
				ip = ipList[i].getHostAddress();
				result.add(new InetSocketAddress(ip, port));
			}
		}
		return result;
	}

	@Override
	public List<InetSocketAddress> findServerAddressList() {
		//返回不可变视图
		return Collections.unmodifiableList(container);
	}

	@Override
	public synchronized InetSocketAddress selector() {
		if (inner.isEmpty()) {
			if (!container.isEmpty()) {
				inner.addAll(container);
			}
		}
		return inner.poll();
	}

	@Override
	public void close() {
		try {
            cachedPath.close();
            zkClient.close();
        } catch (Exception e) {
        }
	}

	@Override
	public String getService() {
		return service;
	}
}
