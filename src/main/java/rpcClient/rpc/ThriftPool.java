package rpcClient.rpc;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TException;

public abstract class ThriftPool<T> {
	private final GenericObjectPool<T> internalPool;

	public ThriftPool(final GenericObjectPoolConfig poolConfig,
    		PooledObjectFactory<T> factory) {
        this.internalPool = new GenericObjectPool<T>(factory, poolConfig);
    }

    public T getResource() throws TException {
        try {
            return (T) internalPool.borrowObject();
        } catch (Exception e) {
            throw new TException(
                    "Could not get a resource from the pool", e);
        }
    }

    public void returnResource(final T resource) throws TException {
        try {
            internalPool.returnObject(resource);
        } catch (Exception e) {
            throw new TException(
                    "Could not return the resource to the pool", e);
        }
    }

    public void returnBrokenResource(final T resource) throws TException {
        try {
            internalPool.invalidateObject(resource);
        } catch (Exception e) {
            throw new TException(
                    "Could not return the resource to the pool", e);
        }
    }

    public void destroy() throws TException {
        try {
            internalPool.close();
        } catch (Exception e) {
            throw new TException("Could not destroy the pool", e);
        }
    }
    public int getActive() {
    	return internalPool.getNumActive();
    }
}
