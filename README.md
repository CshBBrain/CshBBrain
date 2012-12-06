CshBBrain
=========

宝贝鱼(CshBBrain) 是一个来自中国的简单的轻量级的高性能的WebSocket服务器。支持服务器集群，能满足大并发量高容量的分布式系统开发。如果你需要开发带有集群功能的WebSocket服务器，宝贝鱼(CshBBrain) 也许是非常适合你的选择。在宝贝鱼(CshBBrain)中你可以将某个服务器设置为纯粹的集群管理服务器，或纯粹的业务节点服务器和集群管理业务节点服务器3种类型。适合用于数据推送(股票行情),游戏,聊天/im等服务器程序的构建。​

宝贝鱼(CshBBrain)有NIO版本和AIO版本2个版本，宝贝鱼(CshBBrain) 4.0.0之前的版本基于NIO，从4.0.0版起基于AIO。基于JAVA实现的，充分运用了java的多线程技术，线程池，NIO或AIO，缓冲区池等技术。项目从技术架构上采用了分层思想，分为网络传输层，协议解析层和业务层共3层。

网络传输层封装了网络连接的请求建立，数据读写监听，为协议解析层提供服务；协议解析层专门负责具体的协议解析，如果你有兴趣，你也可以在协议层编写自己的协议编码解码器来构建基于你自己协议的服务器；业务层在协议解析层之上做具体的业务处理，这部分的工作就是你要开发具体服务所要编写的业务代码了。​

2012年11月5日国内首款基于AIO的开源WebSocket服务器 宝贝鱼 (CshBBrainAIO)正式发布。基于AIO的开源WebSocket服务器 宝贝鱼(CshBBrain) 依然采用分层的体系结构，协议层和业务层 与 基于NIO技术的 开源WebSocket服务器 宝贝鱼 (CshBBrain) 完全一样，采用基于AIO的的 宝贝鱼 进行服务器开发的方式 与 基于NIO 的宝贝鱼 开发方式完全一样。正是得益于彻底的分层架构，所有宝贝鱼在很短的时间内开发出基于AIO技术的新版本。

如果你的分层 也向 宝贝鱼 一样的话，甚至 你在基于NIO 的宝贝鱼上开发的服务器 完全不用修改任何代码 只需要替换成 基于 AIO的宝贝鱼的网络传输层 的代码 一切就OK了！对，你的服务器也就变成了基于AIO的服务器了。基于AIO的服务器拥有所有基于 NIO 的宝贝鱼服务器所拥有的全部功能，但你必须将JDK换成JDK7.简单吧，简单就是 宝贝鱼 服务器所追求的理念。

如果你觉得Mina,Netty太复杂，庞大，难于上手，CshBBrain也许是适合你的选择。
CshBBrain is a simple,lightweight and high performace websocket server from China.CshBBrain implements in java, base on NIO,Pools ,Asynchronous and so.If you fell Mina, Netty is too large and complex,maybe CshBBrain is a suitable for you.

该项目被开源中国收录：http://www.oschina.net/p/cshbbrain
作者的技术博客地址：http://cshbbrain.iteye.com/
使用交流专栏地址：http://www.iteye.com/blogs/subjects/CshBBrain

与宝贝鱼服务器搭配使用的前台JS框架CshBBrainJS于2012.12.03发布。
项目地址：
http://www.oschina.net/p/cshbbrainjs
http://code.google.com/p/cshbbrainjs/
https://github.com/CshBBrain/CshBBrainJS

数码快看（http://211.100.41.186:8989/mcms/ws/index_ws_tom.html），如果你喜欢数码快看产品请到www.qook.com.cn下载iOS或Android的安装程序。由于知识产权的关系，提供的实例屏蔽了部分功能，混淆了前台代码，只支持高版本的Chrome浏览器。下面上几张图片吧：

<img src="http://dl.iteye.com/upload/attachment/0077/3127/349cffa7-c0f6-3524-a3a3-6497a062ae6c.png"/>

<img src="http://dl.iteye.com/upload/attachment/0077/3125/1384757b-1a6c-3eb2-a660-ffa302dfd813.png"/>

<img src="http://dl.iteye.com/upload/attachment/0077/3132/952b8d62-d3b2-3b0d-9379-93036204ee9c.png"/>

<img src="http://dl.iteye.com/upload/attachment/0077/3133/1c736e75-7ac3-3ab4-975f-56c58c5aaeee.png"/>

<img src="http://dl.iteye.com/upload/attachment/0077/3130/683d821d-6e90-3334-bf6d-4455e7867691.png"/>

<img src="http://dl.iteye.com/upload/attachment/0077/3123/3a344087-3906-37ab-8253-d5056ef2f1a4.png"/>