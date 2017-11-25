package rpcClient.rpc;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpcClient.zookeeper.ThriftServer;
import java.net.InetSocketAddress;

/*
 * 获取连接池
 */
public class ThriftClientPool extends ThriftPool<TServiceClient> {
	private static final Logger logger = LoggerFactory.getLogger(ThriftClientPool.class);
	public ThriftClientPool(final GenericObjectPoolConfig poolConfig,
							ThriftServer addressProvider, TServiceClientFactory<TServiceClient> clientFactory, int timeout ) throws Exception {
	        super(poolConfig, new ThriftClientFactory(addressProvider, clientFactory, timeout));
	    }
	private static class ThriftClientFactory implements PooledObjectFactory<TServiceClient> {
		private final ThriftServer serverAddressProvider;
		private final TServiceClientFactory<TServiceClient> clientFactory;
		//超时时间
		private final int timeout;
		protected ThriftClientFactory(ThriftServer addressProvider, TServiceClientFactory<TServiceClient> clientFactory, int timeout) throws Exception {
			this.serverAddressProvider = addressProvider;
			this.clientFactory = clientFactory;
			this.timeout = timeout;
		}
		/*
		 * @see org.apache.commons.pool.BasePoolableObjectFactory#makeObject()
		 * 创建非阻塞IO的thrift
		 */
		public PooledObject<TServiceClient> makeObject() throws TTransportException {
            logger.info("begin to makeObject");
			InetSocketAddress address = serverAddressProvider.selector();
			if(address==null){
				throw new TTransportException(5,"No serice for RPCClient");
			}
			if(logger.isDebugEnabled())
				logger.debug("mk object, host:"+address.getHostName()+" "+ address.getPort());
			//设置超时时间，server超时，会使用保底的策略
			TSocket tsocket = new TSocket(address.getHostName(), address.getPort(),timeout);
			//非阻塞IO 服务端和客户端需要指定 TFramedTransport 数据传输的方式
			TTransport transport = new TFramedTransport(tsocket);
			//二进制协议
			TProtocol protocol = new TCompactProtocol(transport);
			TServiceClient client = this.clientFactory.getClient(protocol);
			transport.open();
			return new DefaultPooledObject<>(client);
		}
		@Override
		/*
		 * 销毁对象
		 */
		public void destroyObject(PooledObject<TServiceClient> p)
				throws Exception {
            logger.info("begin to destroyObject");
			TServiceClient client = p.getObject();
			TTransport pin = client.getInputProtocol().getTransport();
			pin.close();
			TTransport pout = client.getOutputProtocol().getTransport();
			pout.close();
		}
		@Override
		/*
		 * 检验对象是否可以由pool安全返回
		 */
		public boolean validateObject(PooledObject<TServiceClient> p) {
            logger.info("begin to validateObject");
			TServiceClient client = p.getObject();
			TTransport pin = client.getInputProtocol().getTransport();
			TTransport pout = client.getOutputProtocol().getTransport();
			return pin.isOpen() && pout.isOpen();
		}
		@Override
		public void activateObject(PooledObject<TServiceClient> p)
				throws Exception {
			if(logger.isDebugEnabled())
				logger.debug("begin to activateObject");
		}
		@Override
		public void passivateObject(PooledObject<TServiceClient> p)
				throws Exception {
			if(logger.isDebugEnabled())
				logger.debug("begin to passivateObject");
		}

	}
}
