CshBBrain
=========

CshBBrain 是一个来自中国的简单的轻量级的高性能的WebSocket服务器。基于JAVA实现的，充分运用了java的多线程技术，线程池，NIO，缓冲区池等技术。项目从技术架构上采用了分层思想，分为网络传输层，协议解析层和业务层共3层。适合用于数据推送(股票行情),游戏,聊天/im等服务器程序的构建。 网络传输层封装了网络连接的请求建立，数据读写监听，为协议解析层提供服务；协议解析层专门负责具体的协议解析，如果你有兴趣，你也可以在协议层编写自己的协议编码解码器来构建基于你自己协议的服务器；业务层在协议解析层之上做具体的业务处理，这部分的工作就是你要开发具体服务所要编写的业务代码了。 如果你觉得Mina,Netty太复杂，庞大，难于上手，CshBBrain也许是适合你的选择。
CshBBrain is a simple,lightweight and high performace websocket server from China.CshBBrain implements in java, base on NIO,Pools ,Asynchronous and so.If you fell Mina, Netty is too large and complex,maybe CshBBrain is a suitable for you.
该项目被开源中国收录：http://www.oschina.net/p/cshbbrain
作者的技术博客地址：http://cshbbrain.iteye.com/
使用交流专栏地址：http://www.iteye.com/blogs/subjects/CshBBrain