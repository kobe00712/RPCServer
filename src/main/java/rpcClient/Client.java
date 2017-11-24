package rpcClient;


import rpcClient.rpc.ThriftClientPool;
import rpcClient.rpc.ThriftServiceClientConfig;

public class Client {

    public static void main(String[] args) {
        try {
            // 输入service name
            ThriftServiceClientConfig thriftServiceClientConfig = new ThriftServiceClientConfig();
            ThriftClientPool thriftClientPool = thriftServiceClientConfig.getClientPool();
            //对象转化

        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
