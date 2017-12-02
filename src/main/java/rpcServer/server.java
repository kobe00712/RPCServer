package rpcServer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class server {
    public static void main(String[] args) {
        try {

            ApplicationContext context = new ClassPathXmlApplicationContext("ServerZookeeper.xml");
            Thread.sleep(3000000);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
