package rpcClient;


import org.apache.thrift.TServiceClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import rpc.thrift.idl.rpcEngine;
import rpc.thrift.idl.rpcRequest;
import rpc.thrift.idl.rpcResponse;
import rpcClient.rpc.ThriftClientPool;
import rpcClient.rpc.ThriftServiceClientConfig;

public class Client {

    public static void main(String[] args) {
        try {
            ApplicationContext context = new ClassPathXmlApplicationContext("ClientZookeeper.xml");
            ThriftServiceClientConfig thriftClientProxy = (ThriftServiceClientConfig) context.getBean(ThriftServiceClientConfig.class);
            rpcEngine.Iface thriftClient = (rpcEngine.Iface)thriftClientProxy.getProxyClient();
            rpcRequest rpcRequest = new rpcRequest();
            rpcRequest.setRequest("Hello world");
            rpcRequest.setRequestIsSet(true);
            rpcResponse algoResponse = null;
            //对象转化
            algoResponse = thriftClient.query(rpcRequest);
            System.out.println(algoResponse.reponse);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
