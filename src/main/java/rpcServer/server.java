package rpcServer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class server {
    public static void main(String[] args) {
        try {
            System.out.println("..server is start ");
            ApplicationContext context = new ClassPathXmlApplicationContext("ServerZookeeper.xml");
            Thread.sleep(30000);
            System.out.println("finish is  start");
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
