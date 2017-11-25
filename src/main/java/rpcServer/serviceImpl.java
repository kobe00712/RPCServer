package rpcServer;

import com.facebook.fb303.fb_status;
import org.apache.thrift.TException;
import rpc.thrift.idl.rpcRequest;
import rpc.thrift.idl.rpcResponse;

import java.util.Map;

public class serviceImpl extends service {
    @Override
    public rpcResponse query(rpcRequest request) throws TException {
        System.out.println("hello" + request.getRequest());
        rpcResponse rpcResponse = new rpcResponse();
        rpcResponse.setReponse("hello"+ request.getRequest());
        rpcResponse.setReponseIsSet(true);
        return rpcResponse;
    }
    @Override
    public String getName() throws TException {
        return null;
    }
    @Override
    public String getVersion() throws TException {
        return null;
    }

    @Override
    public fb_status getStatus() throws TException {
        return null;
    }

    @Override
    public String getStatusDetails() throws TException {
        return null;
    }

    @Override
    public Map<String, Long> getCounters() throws TException {
        return null;
    }

    @Override
    public long getCounter(String s) throws TException {
        return 0;
    }

    @Override
    public void setOption(String s, String s1) throws TException {

    }

    @Override
    public String getOption(String s) throws TException {
        return null;
    }

    @Override
    public Map<String, String> getOptions() throws TException {
        return null;
    }

    @Override
    public String getCpuProfile(int i) throws TException {
        return null;
    }

    @Override
    public long aliveSince() throws TException {
        return 0;
    }

    @Override
    public void reinitialize() throws TException {

    }

    @Override
    public void shutdown() throws TException {

    }
}
