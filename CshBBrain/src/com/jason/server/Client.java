package com.jason.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jason.Config;
import com.jason.server.MasterServer.ReadWriteMonitor;
import com.jason.server.hander.CoderHandler;
import com.jason.server.hander.DecoderHandler;
import com.jason.server.hander.ProcessHandler;
import com.jason.util.MyStringUtil;

public class Client{
	private static Log log = LogFactory.getLog(Client.class);// 日志记录器
	public static final String TMP_ROOT = "tmpRoot";// 临时文件目录
	public static final String ZORE_FETCH_COUNT = "zoreFetchCount";// 空读最大次数	
	public static final String tmpRoot = Config.getStr(TMP_ROOT);// 文件保存临时目录
	public static Integer zoreFetchCount = Config.getInt(ZORE_FETCH_COUNT);
		
	private MasterServer sockectServer;// 服务器端socket
	private SelectionKey key;// 客户端通道key
	private boolean handShak = false;// 是否已经握手
	private ConcurrentLinkedQueue<Response> responseMsgs;// 发送给客户端的消息
	private ConcurrentLinkedQueue<Response> responseMsgsNotCode;// 发送给客户端的消息	
	private ConcurrentLinkedQueue<String> requestMsgs;// 客户端发送的请求消息
	private ConcurrentLinkedQueue<HashMap<String, String>> bizObjects;// 解码客户端请求所得到的业务对象
	private AtomicBoolean inRead = new AtomicBoolean(false);// 读取通信信号量
	private AtomicBoolean inWrite = new AtomicBoolean(false);// 回写通信信号量
	private AtomicBoolean isInterestWrite = new AtomicBoolean(false);// 回写事件注册通信信号量
	private ReadWriteMonitor inputMonitorWorker;
	private Integer readCount = 0;// 读取次数
	private boolean preBlank = false;//上次读取为空读
	private boolean readDataFlag = true;//数据读取标识
	private Request requestWithFile = null;// 文件接收器
	
	// 定义编码处理器，业务处理器，解码处理器
	private CoderHandler coderHandler;// 编码处理器
	private DecoderHandler decoderHandler;// 解码处理器
	private ProcessHandler processHandler;// 业务处理器
	private String protocolVersion = "0";//协议版本
	private Object session;//连接会话对象，由开发者自己定义使用
	private Object handShakObject;// 握手处理对象
	private Object index;// 客户端在索引
	private String routeAddress = null;// 远程地址
	private String localAddress = null;// 本地地址
	
	public Object getIndex() {
		return index;
	}

	public void setIndex(Object index) {
		this.index = index;
	}

	public  <T> T  getHandShakObject() {
		return (T)handShakObject;
	}

	public ConcurrentLinkedQueue<Response> getResponseMsgsNotCode() {
		return responseMsgsNotCode;
	}

	public void setResponseMsgsNotCode(
			ConcurrentLinkedQueue<Response> responseMsgsNotCode) {
		this.responseMsgsNotCode = responseMsgsNotCode;
	}
	
	public void setHandShakObject(Object handShakObject) {
		this.handShakObject = handShakObject;
	}

	private boolean isClient = false;// 是否为连接的客户端，默认为不是
	
	public boolean isClient() {
		return isClient;
	}

	public void setClient(boolean isClient) {
		this.isClient = isClient;
	}

	/**
	 * 
	 * <li>方法名：getSession
	 * <li>@param <T>
	 * <li>@return
	 * <li>返回类型：T
	 * <li>说明：获取连接会话对象
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-3
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public <T> T getSession() {//获取会话对象
		return (T)session;
	}

	public void setSession(Object session) {
		this.session = session;
	}

	private static int callMax = 10000;// 最大呼叫10万次
	private int callCount = 0;
	
	static{
		if(zoreFetchCount == null || zoreFetchCount <= 0){
			zoreFetchCount = 1000;
		}
	}
	
	/**
	 * <li>说明：Creates new WebSocket instance using given socket for communication and
     * limiting handshake time to given milliseconds.
     * @param socket :socket that should be used for communication
     * @param timeout ：maximum time in milliseconds for the handshake
     * @param args :arguments that will be passed to handshake call
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-9-28
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public Client(SelectionKey key, MasterServer sockectServer,ReadWriteMonitor inputMonitorWorker)throws IOException{
	    this.sockectServer = sockectServer; 
		this.key = key;
		this.responseMsgs = new ConcurrentLinkedQueue<Response>();// 发送给客户端的消息
		this.responseMsgsNotCode = new ConcurrentLinkedQueue<Response>();// 发送给客户端的消息
		this.requestMsgs =  new ConcurrentLinkedQueue<String>();// 客户端发送的消息
		this.bizObjects =  new ConcurrentLinkedQueue<HashMap<String, String>>();// 客户请求业务处理对象
		this.inputMonitorWorker = inputMonitorWorker;
		this.requestWithFile = new Request();
	}	

	/**
	 * 
	 * <li>方法名：addRequestMsg
	 * <li>@param msg
	 * <li>返回类型：void
	 * <li>说明：添加信息到请求信息中
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-9-29
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void addRequestMsg(String msg){
		this.requestMsgs.add(msg);
	}

	/**
	 * 
	 * <li>方法名：addResponseMsg
	 * <li>@param msg
	 * <li>返回类型：void
	 * <li>说明：将响应信息添加到响应信息集合中
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-9-29
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void addResponseMsg(Response msg){
		this.responseMsgsNotCode.add(msg);
	}
	
	public void addBroadMsg(Response msg){
		this.responseMsgs.add(msg);
	}
	
	/**
	 * 
	 * <li>方法名：sendMsgs
	 * <li>@throws IOException
	 * <li>返回类型：void
	 * <li>说明：批量发送消息到客户端
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-9-29
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	/*public void sendMsgs(){
		if(MasterServer.keepConnect){
			try{
				Response msg = this.responseMsgs.peek();// 获取第一个信息				
				if(msg != null){
					if(msg.isException()){// 存在需要处理的回写，但回写次数超过10万次，关闭链接
						log.info("call count max 100000 per msg");
						this.close();
					}else{
						if(!msg.write(this)){// 回写响应
							this.responseMsgs.clear();
						}else{
							if(msg.isWriteFinished()){
								//this.getChannel().socket().getOutputStream().flush();
								this.responseMsgs.poll();// 移除第一个信息
								
								if(this.responseMsgs.peek() == null){// 没有可以输出的内容，注销输出事件监听
									this.unregisteWrite();
								}
							}
						}
					}
				}
			}catch(Exception e){// 处理异常
				e.printStackTrace();
				this.close();
			}
		}else{// 处理不保存连接的发送方式
			try{
				Response msg = this.responseMsgs.peek();// 获取第一个信息
				if(msg != null){
					if(msg.isException()){// 存在需要处理的回写，但回写次数超过10万次，关闭链接
						log.info("call count max 100000 per msg");
						this.close();
					}else{
						if(!msg.write(this)){// 回写响应
							this.responseMsgs.clear();
						}else{
							if(msg.isWriteFinished()){
								this.responseMsgs.poll();// 移除第一个信息
								
								if(this.responseMsgs.peek() == null){// 没有可以输出的内容，注销输出事件监听
									this.close();
								}
							}
						}
					}
				}else{
					this.close();
				}
			}catch(Exception e){// 处理异常
				e.printStackTrace();
				this.close();
			}
		}
	}*/
	
	public void sendMsgs(){
		if(this.inWrite.compareAndSet(false, true)){
			if(MasterServer.keepConnect){
				try{
					Response msg = this.responseMsgs.peek();// 获取第一个信息
					boolean isStopWrite = false;// 是否停止发送数据
					while(msg != null){
						if(msg.isException()){// 存在需要处理的回写，但回写次数超过10万次，关闭链接
							log.info("call count max 100000 per msg");
							this.close();
							isStopWrite = true;// 回写超过次数限制，退出循环
						}else{
							Integer resulst = msg.write(this);// 写入数据
							switch(resulst){
							case -1:// 出现异常关闭连接
								log.info("写入数据时出现异常，连接关闭");
								this.responseMsgs.clear();
								isStopWrite = true;// 连接关闭，退出循环
								break;
							case 1:// 完成所有数据写入
								this.responseMsgs.poll();// 移除第一个信息
								msg = this.responseMsgs.peek();// 获取第一个信息
								
								if(msg == null){// 没有可以输出的内容，注销输出事件监听
									this.unregisteWrite();
									this.inWrite.set(false);
								}
								break;
							case 2:// 数据没有写入完，等待下次写入
								this.registeWrite();
								isStopWrite = true;// 缓冲区已满，退出循环
								break;
							}						
						}
						if(isStopWrite){// 要么连接关闭，要么连接缓冲区已满，退出循环
							break;
						}
					}
				}catch(Exception e){// 处理异常
					e.printStackTrace();
					this.close();
				}
			}else{// 处理不保存连接的发送方式
				try{
					Response msg = this.responseMsgs.peek();// 获取第一个信息
					boolean isStopWrite = false;// 是否停止发送数据
					while(msg != null){
						if(msg.isException()){// 存在需要处理的回写，但回写次数超过10万次，关闭链接
							log.info("call count max 100000 per msg");
							this.close();
							isStopWrite = true;// 回写超过限制的次数，退出循环
						}else{
							Integer resulst = msg.write(this);// 写入数据
							switch(resulst){
							case -1:// 出现异常关闭连接
								log.info("写入数据时出现异常，连接关闭");
								this.responseMsgs.clear();
								this.close();
								isStopWrite = true;// 连接已关闭，退出循环
								break;
							case 1:// 完成所有数据写入
								this.responseMsgs.poll();// 移除第一个信息
								msg = this.responseMsgs.peek();// 获取第一个信息
								
								if(msg == null){// 没有可以输出的内容，注销输出事件监听
									this.close();
								}
								break;
							case 2:// 数据没有写入完，等待下次写入
								this.registeWrite();
								isStopWrite = true;// 连接缓冲区已满，退出循环
								break;
							}
						}
						
						if(isStopWrite){// 要么连接关闭，要么连接缓冲区已满，退出循环
							break;
						}
					}
				}catch(Exception e){// 处理异常
					e.printStackTrace();
					this.close();
				}
			}
		}
	}
	
	/**
	 * 
	 * <li>方法名：close
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：关闭链接
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-12-11
	 * <li>修改人： 
	 * <li>修改日期：
	 * @throws IOException 
	 */
	public void close(){		
		if(this.requestWithFile != null && this.requestWithFile.isReadFile()){
			FileTransfer fr = requestWithFile.getFileReceiver();
			if(fr != null  && !fr.finishWrite()){
				fr.forceClose();// 强制关闭删除损害的文件
			}
		}
				
		// 关闭连接
		SocketChannel socketChannel = (SocketChannel) this.key.channel();
		try{
			this.sockectServer.clearSocket(index);// 清除
			unregiste();			
			this.key.attach(null);
			this.key.cancel();
		} catch (ClosedChannelException e) {
			e.printStackTrace();
		}finally{			
			try{
				//socketChannel.socket().getOutputStream().flush();
				socketChannel.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * 
	 * <li>方法名：readRequest
	 * <li>@param byteBuffer
	 * <li>@return
	 * <li>返回类型：boolean
	 * <li>说明：读取请求数据
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-2-2
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	private boolean readRequest(ByteBuffer byteBuffer){
		SocketChannel socketChannel = (SocketChannel) this.key.channel();
		boolean readSuccess = false;// 是否读取到数据标识
		boolean returnValue = false;// 返回值	
		
		try{			
			int dataLength = 0;
			do{// 读取客户端请求的数据并解码					
				dataLength = socketChannel.read(byteBuffer);				
				if(dataLength == -1){// 客户端已经关闭连接的情况
					log.info("客户端已经关闭");
					this.close();					
				}else if(dataLength > 0){// 处理读取到数据的情况
					byteBuffer.flip();
					this.decoderHandler.process(byteBuffer,this);// 解码处理				
					byteBuffer.clear();					
					readSuccess = true;
					this.readDataFlag = true;// 将读取数据标识设置为真
					this.preBlank = false;// 上次不为空读
				}else{// 处理空读的情况
					if(this.preBlank){
						++this.readCount;
					}
					
					this.preBlank = true;
					break;
				}
			}while(dataLength > 0);			
			
			if(this.readCount >= zoreFetchCount){// 空读超过指定次数，关闭链接，返回
				log.info("the max count read: " + this.readCount);
				this.close();
				returnValue = false;
				return returnValue;
			}
		}catch(IOException e){	
			e.printStackTrace();
			this.close();
			returnValue = false;
			return returnValue;
		}
		
		if(readSuccess){// 如果读取到数据						
			if(requestWithFile.isReadFile()){// 是否读取文件
				if(requestWithFile.readFinish()){// 是否读取完毕文件
				//if(requestWithFile.getFileReceiver().finishWrite()){// 是否读取完毕文件
					returnValue = true;
					if(MasterServer.keepConnect){//长连接
						this.registeRead();
					}
				}else{// 没有读取完毕文件，注册通道，继续读取
					if(MasterServer.keepConnect){//长连接
						this.registeRead();
					}else{
						try{
							this.inputMonitorWorker.registeRead(key);
						}catch(Exception e){
							e.printStackTrace();
							this.close();
							returnValue = false;
							this.requestWithFile.getFileReceiver().close();// 关闭文件
							return returnValue;
						}
						
						this.inRead.compareAndSet(true, false);
					}
					
					returnValue = false;// 将文件内容读取完后再进行处理
				}
			}else{// 普通请求，没有上传文件
				returnValue = true;
				if(MasterServer.keepConnect){//长连接
					this.registeRead();
				}
			}
		}else{
			returnValue = false;
			if(MasterServer.keepConnect){//长连接
				this.registeRead();
			}
		}
		
		if(returnValue){// 读取完毕放入处理队列
			HashMap<String, String> requestData = requestWithFile.getRequestData();
			if(requestData != null){
				this.getBizObjects().add(requestData);
			}
		}
		
		return returnValue;
	}
	
	/**
	 * 
	 * <li>方法名：registeRead
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：注册读取通道
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-8-22
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	private void registeRead(){// 注册读取
		this.inRead.compareAndSet(true, false);
		
		try{
			this.inputMonitorWorker.registeRead(key);
		}catch(Exception e){
			e.printStackTrace();
			this.close();
		}
	}
		
	/**
	 * 
	 * <li>方法名：getMessages
	 * <li>@param byteBuffer
	 * <li>@return
	 * <li>返回类型：boolean
	 * <li>说明：线程池调用此方法读取数据
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-12
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public boolean getMessages(ByteBuffer byteBuffer){		
		try{
			boolean returnValue = false;
			returnValue = this.readRequest(byteBuffer);
			
			return returnValue;
		}catch(Exception e){// 处理异常
			e.printStackTrace();
			this.close();
			return false;
		}
	}	
	
	/**
	 * 
	 * <li>方法名：process
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：对链接的一次请求进行处理
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-2-9
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void process(){
		try{
			this.processHandler.process(this);// 业务处理
			if(!this.responseMsgsNotCode.isEmpty()){// 不为空进行写出信息
				this.coderHandler.process(this);// 协议编码处理
				//this.inputMonitorWorker.registeWrite(key);
				this.sendMsgs();
			}
		}catch(Exception e){
			e.printStackTrace();
			this.close();
		}
	}
	
	/**
	 * 
	 * <li>方法名：registeWrite
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：注册回写事件监听器
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-8-24
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void registeWrite(){
		if(isInterestWrite.compareAndSet(false, true)){
			this.inputMonitorWorker.registeWrite(key);
		}
	}

	public boolean isHandShak() {
		return handShak;
	}

	public void setHandShak(boolean handShak) {
		this.handShak = handShak;
	}

	public MasterServer getSockectServer() {
		return sockectServer;
	}

	public void setSockectServer(MasterServer sockectServer) {
		this.sockectServer = sockectServer;
	}
	
	/**
	 * 
	 * <li>方法名：getSockector
	 * <li>@param key
	 * <li>@return
	 * <li>返回类型：Sockector
	 * <li>说明：根据selection key 获取业务处理相关对象
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-9
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static Client getSockector(SelectionKey key){
		Object attachment = key.attachment();
		if(attachment instanceof Client){
			return (Client)attachment;
		}else{
			return null;
		}
	}
	
	/**
	 * 
	 * <li>方法名：registeInput
	 * <li>@throws ClosedChannelException
	 * <li>返回类型：void
	 * <li>说明：注册客户端通道的读取监听器
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void registeInput() throws ClosedChannelException{
		this.key.interestOps(this.key.interestOps() | SelectionKey.OP_READ);
	}
	
	/**
	 * 
	 * <li>方法名：unregiste
	 * <li>@throws ClosedChannelException
	 * <li>返回类型：void
	 * <li>说明：取消监听事件
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-2-15
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void unregiste() throws ClosedChannelException{
		this.key.interestOps(0);
	}
	
	/**
	 * 
	 * <li>方法名：unregisteInput
	 * <li>@throws ClosedChannelException
	 * <li>返回类型：void
	 * <li>说明：取消客户端通道的输入监听事件
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void unregisteRead() throws ClosedChannelException{
		this.key.interestOps(this.key.interestOps() ^ SelectionKey.OP_READ);
	}
	
	/**
	 * 
	 * <li>方法名：unregisteWrite
	 * <li>@throws ClosedChannelException
	 * <li>返回类型：void
	 * <li>说明：取消客户端输出监听事件
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-2-6
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void unregisteWrite() throws ClosedChannelException{
		if(this.isInterestWrite.compareAndSet(true, false)){
			this.key.interestOps(this.key.interestOps() ^ SelectionKey.OP_WRITE);
		}
	}
	
	/**
	 * 
	 * <li>方法名：getChannel
	 * <li>@return
	 * <li>返回类型：SocketChannel
	 * <li>说明：获取通道
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-2-15
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public SocketChannel getChannel(){
		return (SocketChannel) this.key.channel();
	}
	
	/**
	 * 
	 * <li>方法名：registeHandler
	 * <li>@param coderHandler
	 * <li>@param decoderHandler
	 * <li>@param processHandler
	 * <li>返回类型：void
	 * <li>说明：给通信通道注册编码器，解码器，业务处理器
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void registeHandler(CoderHandler coderHandler, DecoderHandler decoderHandler, ProcessHandler processHandler){
		this.coderHandler = coderHandler;
		this.decoderHandler = decoderHandler;
		this.processHandler = processHandler;
	}

	public ConcurrentLinkedQueue<HashMap<String, String>> getBizObjects() {
		return bizObjects;
	}

	public void setBizObjects(ConcurrentLinkedQueue<HashMap<String, String>> bizObjects) {
		this.bizObjects = bizObjects;
	}

	public ConcurrentLinkedQueue<String> getRequestMsgs() {
		return requestMsgs;
	}

	public void setRequestMsgs(ConcurrentLinkedQueue<String> requestMsgs) {
		this.requestMsgs = requestMsgs;
	}

	public ConcurrentLinkedQueue<Response> getResponseMsgs() {
		return responseMsgs;
	}

	public void setResponseMsgs(ConcurrentLinkedQueue<Response> responseMsgs) {
		this.responseMsgs = responseMsgs;
	}
	
	/**
	 * 
	 * <li>方法名：getIp
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：获取客户端的ip地址
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-30
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public String getIp(){
		return ((SocketChannel)this.key.channel()).socket().getInetAddress().getHostAddress();
	}
	
	/**
	 * 
	 * <li>方法名：getAddress
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：获取连接另外一端的地址
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-28
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public String getRouteAddress(){
		if(MyStringUtil.isBlank(this.routeAddress)){
			int port = ((SocketChannel)this.key.channel()).socket().getPort();
			this.routeAddress = (((SocketChannel)this.key.channel()).socket().getInetAddress().getHostAddress() + ":" +port);
		}
		
		return this.routeAddress;
	}

	/**
	 * 
	 * <li>方法名：getLocalAddress
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：获取连接的本端地址
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-28
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public String getLocalAddress(){		
		if(MyStringUtil.isBlank(this.localAddress)){
			int port = ((SocketChannel)this.key.channel()).socket().getLocalPort();
			this.localAddress = (((SocketChannel)this.key.channel()).socket().getLocalAddress().getHostAddress() + ":" +port);
		}
		
		return this.localAddress;
	}
	
	public AtomicBoolean getInRead() {
		return inRead;
	}

	public Request getRequestWithFile() {
		return requestWithFile;
	}

	public void setRequestWithFile(Request requestWithFile) {
		this.requestWithFile = requestWithFile;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}
	
	/**
	 * 
	 * <li>方法名：getProtocolVersionInt
	 * <li>@return
	 * <li>返回类型：int
	 * <li>说明：返回整数标识的版本号码
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-12
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public int getProtocolVersionInt(){
		return Integer.valueOf(this.protocolVersion);
	}
	
	/**
	 * 
	 * <li>方法名：broadCastMessage
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：往连接的对端发送消息
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-2
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void sendMessage(Response msg){
		try{
			this.responseMsgsNotCode.add(msg);
			this.coderHandler.process(this);// 编码消息
			log.info("向客户端" + this.index + "发送数据：" + msg.getBody());
			//this.inputMonitorWorker.registeWrite(key);
			this.sendMsgs();
		}catch(Exception e){
			e.printStackTrace();
			this.close();
		}
	}
	
	
	public void send(){
		try{
			//this.coderHandler.process(this);// 编码消息
			this.sendMsgs();
		}catch(Exception e){
			e.printStackTrace();
			this.close();
		}
	}
	
	public void sendDirectMessage(Response msg){
		try{
			this.responseMsgsNotCode.add(msg);
			this.coderHandler.process(this);// 编码消息
			this.sendMsgs();
		}catch(Exception e){
			e.printStackTrace();
			this.close();
		}
	}
	
	public boolean isReadDataFlag() {
		return readDataFlag;
	}

	public void setReadDataFlag(boolean readDataFlag) {
		this.readDataFlag = readDataFlag;
	}
	
	/**
	 * 
	 * <li>方法名：handShak
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：执行握手请求处理
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-22
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void requestHandShak(){
		this.isClient = true;// 连接的客户端
		this.coderHandler.handShak(this);
	}
	
	public CoderHandler getCoderHandler() {
		return coderHandler;
	}

	public void setCoderHandler(CoderHandler coderHandler) {
		this.coderHandler = coderHandler;
	}

	public DecoderHandler getDecoderHandler() {
		return decoderHandler;
	}

	public void setDecoderHandler(DecoderHandler decoderHandler) {
		this.decoderHandler = decoderHandler;
	}

	public ProcessHandler getProcessHandler() {
		return processHandler;
	}

	public void setProcessHandler(ProcessHandler processHandler) {
		this.processHandler = processHandler;
	}
}
