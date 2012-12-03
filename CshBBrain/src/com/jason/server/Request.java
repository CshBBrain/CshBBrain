/**
 * <li>文件名：RequestWithFile.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-12-12
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jason.util.CoderUtils;

/**
 * <li>类型名称：
 * <li>说明：带发送文件的请求
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-12-12
 * <li>修改人： 
 * <li>修改日期：
 */
public class Request {
	private static Log log = LogFactory.getLog(Request.class);// 日志记录器
	private static Integer BUFFER_LIMIT = 10 * 1000;// 10k大小
	private FileTransfer fileReceiver; // 文件接受
	private HashMap<String, String> requestData;// 请求参数
	private Boolean isReadFile = false;// 是否准备读取文件
	
	private Boolean readData = false;// 请求数据是否读取完毕，主要针对post请求处理
	
	// 针对数据量比较小的请求，使用的int型表示，表示最大的数据量为int能所能表示的最大长度
	private Integer dataSizeLeftInt = 0;// 数据剩余大小
	private ByteBuffer datas;//请求数据
	
	// 针对数据量特别大的请求，比如视频流，做专门的处理
	private Long dataSizeLeftLong = 0l;// 数据剩余大小 
	private ArrayList<byte[]> byteDatas;// 流格式数据
	private boolean directProcess = false;// 是否立即处理收到的数据，具体协议实现根据需要设置
	
	private long dataPosition = 0l;// 接收数据的当前位置，从0开始接收起
	private String message = null;// 临时存储的消息
	
	private AtomicInteger requestIndex = new AtomicInteger(0);// 请求顺序
	private HashMap<String,Object> messageHeaders = new HashMap<String, Object>();
	private String crrentRequestIndex = null;// 当前请求序号
	
	public String getCrrentRequestIndex() {
		return crrentRequestIndex;
	}

	public void setCrrentRequestIndex(String crrentRequestIndex) {
		this.crrentRequestIndex = crrentRequestIndex;
	}
	/**
	 * 
	 * <li>方法名：setMessageHeader
	 * <li>@param messageHeader
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：
	 * <li>创建人：陈嗣洪
	 * <li>创建日期：2012-11-26
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public String setMessageHeader(Object messageHeader){
		String index = String.valueOf(requestIndex.incrementAndGet());
		this.messageHeaders.put(index, messageHeader);
		this.crrentRequestIndex = index;
		return index;
	}
	
	/**
	 * 
	 * <li>方法名：getMessageHeader
	 * <li>@param <T>
	 * <li>@param index
	 * <li>@return
	 * <li>返回类型：T
	 * <li>说明：
	 * <li>创建人：陈嗣洪
	 * <li>创建日期：2012-11-26
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public <T> T getMessageHeader(String index) {
		Object header = this.messageHeaders.get(index);
		if(header != null){
			return (T)header;
		}else{
			return null;
		}
	}
	
	
	public String getRequestMessage(){
		if(byteDatas.size() == 1){// 如果长度为1的话
			this.message = CoderUtils.decode(ByteBuffer.wrap(byteDatas.get(0)));// 直接处理返回
		}else if(byteDatas.size() > 1){		
			ByteBuffer buffer = null;// 缓冲区
			if(this.dataPosition > BUFFER_LIMIT){// 收到的数据超过大小
				buffer = ByteBuffer.allocateDirect(Integer.parseInt(String.valueOf(this.dataPosition)));
			}else{// 收到的数据没有超过大小
				buffer = ByteBuffer.allocate(Integer.parseInt(String.valueOf(this.dataPosition)));
			}
				
			for(int i =0; i < byteDatas.size(); ++i){
				buffer.put(byteDatas.get(i));
			}
			
			buffer.flip();// 读取数据
			this.message = CoderUtils.decode(buffer);// 直接处理返回
		}
		
		return message;// 返回空串
	}
	
	/**
	 * 
	 * <li>方法名：clear
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：清理每次连接的数据
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-19
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void clear(){
		this.fileReceiver = null;
		this.requestData = null;
		this.isReadFile = false;
		this.readData = false;
		this.dataSizeLeftInt = 0;
		this.datas = null;
		this.dataSizeLeftLong = 0l;
		this.byteDatas = null;
		this.directProcess = false;
		this.dataPosition = 0l;
		this.message = null;
		this.crrentRequestIndex = null;
	}
	
	/**
	 * 
	 * <li>方法名：clearMessageHeader
	 * <li>@param index
	 * <li>返回类型：void
	 * <li>说明：清除请求消息头部信息
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-11-27
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void clearMessageHeader(String index){
		this.messageHeaders.remove(index);
	}
	
	/**
	 * 
	 * <li>方法名：readFinish
	 * <li>@return
	 * <li>返回类型：boolean
	 * <li>说明：是否接收完毕所有数据
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-6
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public boolean readFinish(){
		return (this.dataSizeLeftInt <= 0 ? true : false) && (this.dataSizeLeftLong <= 0 ? true : false) && (this.fileReceiver == null || (this.fileReceiver != null && this.fileReceiver.finishWrite()));
	}
	public Request(){
		
	}
	
	public Request(FileTransfer fileReceiver, HashMap<String, String> requestData){
		this.fileReceiver = fileReceiver;
		this.requestData = requestData;
	}
	
	public FileTransfer getFileReceiver() {
		return fileReceiver;
	}
	public void setFileReceiver(FileTransfer fileReceiver) {
		this.fileReceiver = fileReceiver;
	}
	public HashMap<String, String> getRequestData() {
		return requestData;
	}
	public void setRequestData(HashMap<String, String> requestData) {
		this.requestData = requestData;
	}
	
	public boolean isReadFile(){
		return this.fileReceiver == null ? false : true;
	}

	public Boolean getIsReadFile() {
		return isReadFile;
	}

	public void setIsReadFile(Boolean isReadFile) {
		this.isReadFile = isReadFile;
	}

	public ByteBuffer getDatas() {
		return datas;
	}

	public void setDatas(ByteBuffer datas) {
		this.datas = datas;
	}

	public Integer getDataSizeLeftInt() {
		return dataSizeLeftInt;
	}

	public void setDataSizeLeftInt(Integer dataSizeLeft) {
		this.dataSizeLeftInt = dataSizeLeft;
	}

	public Boolean getReadData() {
		return readData;
	}

	public void setReadData(Boolean readData) {
		this.readData = readData;
	}
	public ArrayList<byte[]> getByteDatas() {
		return byteDatas;
	}
	public void setByteDatas(ArrayList<byte[]> byteDatas) {
		this.byteDatas = byteDatas;
	}
	public Long getDataSizeLeftLong() {
		return dataSizeLeftLong;
	}
	public void setDataSizeLeftLong(long dataSizeLeftLong) {
		this.dataSizeLeftLong = dataSizeLeftLong;
	}
	public boolean isDirectProcess() {
		return directProcess;
	}
	public void setDirectProcess(boolean directProcess) {
		this.directProcess = directProcess;
	}
	public long getDataPosition() {
		return dataPosition;
	}
	public void setDataPosition(long dataPosition) {
		this.dataPosition = dataPosition;
	}	
}
