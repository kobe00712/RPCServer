package rpcServer.thrift;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import rpc.thrift.idl.rpcEngine;
import rpcServer.zookeeper.ThriftServerAddressRegister;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Constructor;
public class ThriftServiceServerFactory{
        private Integer port;
        private Integer priority = 1;// default
        private Object service;// serice实现类
        private ThriftServerIPLocation ipLocation;
        private ThriftServerAddressRegister addressReporter;
        private ServerThread serverThread;
        private String servicePath;

        public void init() throws Exception {
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
            rpcEngine.Processor processor = null;
            for (Class clazz : interfaces) {
                String cname = clazz.getSimpleName();
                if (!cname.equals("Iface")) {
                    continue;
                }
                String pname = clazz.getEnclosingClass().getName() + "$Processor";
                try {
                    Class pclass = classLoader.loadClass(pname);
                    if (!pclass.isAssignableFrom(rpcEngine.Processor.class)) {
                        continue;
                    }
                    Constructor constructor = pclass.getConstructor(clazz);
                    processor = (rpcEngine.Processor) constructor.newInstance(service);
                    break;
                } catch (Exception e) {
                    //
                }
            }
            if (processor == null) {
                throw new IllegalClassFormatException("service-class should implements Iface");
            }
            serverThread = new ServerThread(processor, port);
            serverThread.start();
            // report
            if (addressReporter != null) {
                addressReporter.report(servicePath, hostname);
            }
        }
        class ServerThread extends Thread {
            private TServer server;
            ServerThread(rpcEngine.Processor processor, int port) throws Exception {
                TServerSocket serverTransport = new TServerSocket(port);
                TBinaryProtocol.Factory portFactory = new TBinaryProtocol.Factory(true, true);
                server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport)
                        .processor(processor)
                        .protocolFactory(portFactory));
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
        public void setAddressReporter(ThriftServerAddressRegister addressReporter) {
        this.addressReporter = addressReporter;
    }
        public void setConfigPath(String servicePath) {
        this.servicePath = servicePath;
    }
}
