package zookeeper.impl;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 解析网卡Ip
 *
 */
public class ThriftServerIpLocalNetworkResolve implements ThriftServerIpResolve {

    private Logger logger = LoggerFactory.getLogger(getClass());

    //缓存
    private String serverIp;

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    @Override
    public String getServerIp() {
        System.out.println("get ServerIp begin:");
        if (serverIp != null) {
            return serverIp;
        }
        // 一个主机有多个网络接口
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            System.out.println("get interface:");
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = netInterfaces.nextElement();
                System.out.println("netw interface :"+netInterface.toString());
                // 每个网络接口,都会有多个"网络地址",比如一定会有lookback地址,会有siteLocal地址等.以及IPV4或者IPV6 .
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if(address instanceof Inet6Address){
                        System.out.println("net work is ipv6:"+address.getHostAddress());
                        continue;
                    }
                    if (address.isSiteLocalAddress() && !address.isLoopbackAddress()) {
                        serverIp = address.getHostAddress();
                        logger.info("resolve server ip :"+ serverIp);
                        System.out.println("find ip<>:"+serverIp+"</>");
                        continue;
                    }else{
                        System.out.println("find other :"+address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return serverIp;
    }

    @Override
    public void reset() {
        serverIp = null;
    }
}
