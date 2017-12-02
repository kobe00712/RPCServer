package rpcServer.zookeeper.impl;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.CreateMode;

public class ThriftServerAddressRegisterImpl implements ThriftServerAddressRegister {
    private CuratorFramework zookeeper;

    public ThriftServerAddressRegisterImpl(){}

    public void setZookeeper(CuratorFramework zookeeper) {
        this.zookeeper = zookeeper;
    }

    @Override
    public void report(String service, String address) throws Exception {
        if(zookeeper.getState() == CuratorFrameworkState.LATENT){
            zookeeper.start();
            zookeeper.newNamespaceAwareEnsurePath(service);
        }
        //注册
        zookeeper.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(service +"/i_",address.getBytes("utf-8"));
    }


    public void close(){
        zookeeper.close();
    }
    public ThriftServerAddressRegisterImpl(CuratorFramework zookeeper){
        this.zookeeper = zookeeper;
    }
}
