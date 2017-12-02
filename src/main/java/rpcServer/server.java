package rpcServer;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class server {
    public static void main(String[] args) {
        try {
            new ClassPathXmlApplicationContext("classpath:server.xml");

        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
