/**
 * @Author Yomi
 * @Description:
 * @Data 2025/7/15 10:57
 * @Modified By:
 */
package link.rdcn;

import link.rdcn.server.FairdServer;

public class JServerDemo {
    public static void main(String[] args) {
        TestDemoProvider provider = new TestDemoProvider();
        //根据fairdHome自动读取配置文件
        FairdServer server = new FairdServer(provider.dataProvider(),provider.authProvider(),TestBase.getResourcePath("tls"));
        server.start();
    }
}
