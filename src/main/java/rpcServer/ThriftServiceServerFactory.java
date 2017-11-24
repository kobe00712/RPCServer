package rpcServer;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.springframework.beans.factory.InitializingBean;
import rpcServer.zookeeper.register;

import javax.annotation.processing.Processor;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Constructor;

public class ThriftServiceServerFactory implements InitializingBean {
        private Integer port;
        private Integer priority = 1;// default
        private Object service;// serice实现类
        private ThriftServerIPLocation ipLocation;
        private register addressReporter;
        private ServerThread serverThread;
        private String configPath;
        public void setService(Object service) {
            this.service = service;
        }
        public void setPriority(Integer priority) {
            this.priority = priority;
        }
        public void setPort(Integer port) {
            this.port = port;
        }
        public void setIpLocation(ThriftServerIPLocation ipLocation) {
            this.ipLocation = ipLocation;
        }
        public void setAddressReporter(register addressReporter) {
            this.addressReporter = addressReporter;
        }
        public void setConfigPath(String configPath) {
            this.configPath = configPath;
        }
        @Override
        public void afterPropertiesSet() throws Exception {
            if (ipLocation == null) {
                ipLocation = new ThriftServerIPLocation();
            }
            String ip = ipLocation.getLocalIP();
            if (ip == null) {
                throw new NullPointerException("cant find rpcServer.server ip...");
            }
            String hostname = ip + ":" + port + ":" + priority;
            Class serviceClass = service.getClass();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?>[] interfaces = serviceClass.getInterfaces();
            if (interfaces.length == 0) {
                throw new IllegalClassFormatException("service-class should implements Iface");
            }
            // reflect,load "Processor";
            Processor processor = null;
            for (Class clazz : interfaces) {
                String cname = clazz.getSimpleName();
                if (!cname.equals("Iface")) {
                    continue;
                }
                String pname = clazz.getEnclosingClass().getName() + "$Processor";
                try {
                    Class pclass = classLoader.loadClass(pname);
                    if (!pclass.isAssignableFrom(Processor.class)) {
                        continue;
                    }
                    Constructor constructor = pclass.getConstructor(clazz);
                    processor = (Processor) constructor.newInstance(service);
                    break;
                } catch (Exception e) {
                    //
                }
            }

            if (processor == null) {
                throw new IllegalClassFormatException("service-class should implements Iface");
            }
            //需要单独的线程,因为serve方法是阻塞的.
            serverThread = new ServerThread(processor, port);
            serverThread.start();
            // report
            if (addressReporter != null) {
                addressReporter.report(configPath, hostname);
            }

        }
        class ServerThread extends Thread {
            private TServer server;
            ServerThread(Processor processor, int port) throws Exception {
                TServerSocket serverTransport = new TServerSocket(port);
                TBinaryProtocol.Factory portFactory = new TBinaryProtocol.Factory(true, true);
                TServer.Args args = new TServer.Args(serverTransport);
                args.processor(processor);
                args.protocolFactory(portFactory);
                server = new TThreadPoolServer(args);
            }
            @Override
            public void run(){
                try{
                    server.serve();
                }catch(Exception e){
                    //
                }
            }
            public void stopServer(){
                server.stop();
            }
        }
        public void close() {
            serverThread.stopServer();
        }

    }
}
