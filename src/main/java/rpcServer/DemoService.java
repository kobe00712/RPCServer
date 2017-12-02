package rpcServer;


import org.apache.thrift.TException;
import sgepri.sgcc.demo.Account;
import sgepri.sgcc.demo.InvalidOperation;
import sgepri.sgcc.demo.Request;

public class DemoService implements Account.Iface{
    @Override
    public String doAction(Request request) throws InvalidOperation, TException {
        String echo = "your name is:"+request.name + "you password is"  + request.password;
        return echo;
    }
}
