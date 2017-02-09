# mySimpleHttpServer
一个未完善的基于java nio的http服务器

基于Reactor模型：
acceptor线程阻塞地接受http请求连接，并分发给poller线程。
poller线程通过java nio非阻塞地进行读写操作的注册和处理。
最后poller线程将网络数据交给一个简易线程池进行编解码处理。
实现了常用的http方法，首部字段，状态码。

next...
功能测试和性能测试。
完善功能：更多的http方法，首部字段，状态码。
改善性能。
