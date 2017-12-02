package rpcServer.zookeeper;

public interface ThriftServerAddressRegister {
    public void report(String service,String address) throws Exception;
}
