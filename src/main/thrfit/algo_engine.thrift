include "fb303.thrift"

/*
 * @author: wangfei
 * thrift --gen cpp --gen java --gen python
 */
namespace java rpc.thrift.idl
// (200) + 800 + (5*80) + 100*20 = 2880~3400bytes

struct rpcRequest {
	 1: required string             request,
}

// 160 * 3 = 500bytes
struct rpcResponse {
	 1: required string         reponse,
}

//
service rpcEnging extends fb303.FacebookService {
  rpcResponse query(1: rpcRequest request);
}




