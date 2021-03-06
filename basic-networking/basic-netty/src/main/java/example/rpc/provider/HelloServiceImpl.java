package example.rpc.provider;

import example.rpc.client.HelloService;

/**
 * 暴露服务的实现
 */
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String msg) {
        return "hello " + msg;
    }
}
