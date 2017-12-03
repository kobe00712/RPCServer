package rpcClient;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import sgepri.sgcc.demo.Account;
import sgepri.sgcc.demo.Request;



//客户端调用
@SuppressWarnings("resource")
public class Client {
    public static void main(String[] args) {
        //simple();
        spring();
    }

    public static void spring() {
        try {
            final ApplicationContext context = new ClassPathXmlApplicationContext("spring-context-thrift-client.xml");
            Account.Iface clientSerivce = (Account.Iface) context.getBean("clientSerivce");
            Request request = new Request();
            request.setName("wang xiao 2");
            request.setPassword("123456");
            System.out.println(clientSerivce.doAction(request));
            //关闭连接的钩子

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}