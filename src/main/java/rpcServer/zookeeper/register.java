package rpcServer.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.CreateMode;

public class register {

        private CuratorFramework zookeeper;

        public void setZookeeper(CuratorFramework zookeeper) {
            this.zookeeper = zookeeper;
        }
        public void report(String service, String address) throws Exception {
            if(zookeeper.getState() == CuratorFrameworkState.LATENT){
                zookeeper.start();
                zookeeper.newNamespaceAwareEnsurePath(service);
            }
            zookeeper.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(service +"/i_",address.getBytes("utf-8"));
        }


        public void close(){
            zookeeper.close();
        }

}
