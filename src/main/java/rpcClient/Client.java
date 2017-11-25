package rpcClient;


import org.apache.thrift.TServiceClient;
import rpc.thrift.idl.rpcEngine;
import rpc.thrift.idl.rpcRequest;
import rpc.thrift.idl.rpcResponse;
import rpcClient.rpc.ThriftClientPool;
import rpcClient.rpc.ThriftServiceClientConfig;

public class Client {

    public static void main(String[] args) {
        try {
            ThriftServiceClientConfig thriftServiceClientConfig = new ThriftServiceClientConfig();
            ThriftClientPool thriftClientPool = thriftServiceClientConfig.getClientPool();
            rpcEngine.Iface algoService = null;
            rpcResponse algoResponse = null;
            TServiceClient thriftClient = null;
            rpcRequest rpcRequest = new rpcRequest();
            rpcRequest.setRequest("Hello world");
            rpcRequest.setRequestIsSet(true);
            //对象转化
            thriftClient = thriftClientPool.getResource();
            algoService = (rpcEngine.Iface) thriftClient;
            algoResponse = algoService.query(rpcRequest);
            System.out.println(algoResponse.reponse);
            thriftClientPool.returnResource(thriftClient);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
