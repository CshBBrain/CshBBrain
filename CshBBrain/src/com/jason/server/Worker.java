package com.jason.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jason.Config;

/**
 * 
 * <li>类型名称：
 * <li>说明：客户端请求读取线程
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-5-3
 * <li>修改人： 
 * <li>修改日期：
 */
public class Worker{
	private static Log log = LogFactory.getLog(Worker.class);// 日志记录器
	private static final String READ_BUFFER = "readBuffer";
	private BlockingQueue<Worker> idleWorkers;
	private BlockingQueue<Object> handoffBox;
	private Thread internalThread;//内部工作线程
	private volatile boolean noStopRequested;
	private final AtomicInteger threadIndex = new AtomicInteger();// 线程索引号
	private ByteBuffer charBuffer = null;// 读取缓冲区 
	
	public Worker(int workerPriority,BlockingQueue<Worker> idleWorkers){
		Integer bufferSize = Config.getInt(READ_BUFFER);
		if(bufferSize == null){
			bufferSize = 10;
		}
		
		this.charBuffer = ByteBuffer.allocateDirect(1024 * bufferSize);// 创建读取缓冲区
		this.idleWorkers = idleWorkers;
		handoffBox = new ArrayBlockingQueue<Object>(1);
		
		noStopRequested = true;
		
		Runnable r = new Runnable(){
			public void run(){
				try{
					runWork();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		};
		
		this.internalThread = new Thread(r);
		this.internalThread.setName("请求参数读取线程" + threadIndex.getAndIncrement());
		this.internalThread.setPriority(workerPriority);
		this.internalThread.start();
	}
	
	private void runWork(){		
		while(this.noStopRequested){
			try{
				this.idleWorkers.add(this);
				
				process(handoffBox.take());//接收数据进行处理				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public void processResponse(Object key){
		this.handoffBox.add(key);
	}
	
	public void process(Object selectKey){
		if(selectKey instanceof SelectionKey){
			SelectionKey selectionKey = (SelectionKey)selectKey;
			Client key = Client.getSockector(selectionKey);
			if(key.getMessages(charBuffer)){ // 已完成握手，从客户端读取报刊
				key.process();// 进行业务处理
			}
		}else if(selectKey instanceof Client){// 直接发送消息
			((Client)selectKey).send();
		}
	}
	
	public void stop(){
		this.noStopRequested = false;
		this.internalThread.interrupt();
	}
}
