namespace java sgepri.sgcc.demo

struct Request{
  1: string name,
  2: string password,
}

exception InvalidOperation{
  1: i32 code,
  2: string reason
}

service Account{
  string doAction(1: Request request) throws (1: InvalidOperation e);
}