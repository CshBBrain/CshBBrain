/**
 * <li>文件名：HttpCoder.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.clusters;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jason.server.Response;
import com.jason.server.Client;
import com.jason.server.hander.CoderHandler;
import com.jason.util.CoderUtils;

/**
 * 
 * * <pre>
 * version 5-->
 *    0                   1                   2                   3
 *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *   +-+-+-+-+-------+-+-------------+-------------------------------+
 *   |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
 *   |I|S|S|S|  (4)  |A|     (7)     |             (16/63)           |
 *   |N|V|V|V|       |S|             |   (if payload len==126/127)   |
 *   | |1|2|3|       |K|             |                               |
 *   +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
 *   |     Extended payload length continued, if payload len == 127  |
 *   + - - - - - - - - - - - - - - - +-------------------------------+
 *   |                               |Masking-key, if MASK set to 1  |
 *   +-------------------------------+-------------------------------+
 *   | Masking-key (continued)       |          Payload Data         |
 *   +-------------------------------- - - - - - - - - - - - - - - - +
 *   :                     Payload Data continued ...                :
 *   + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
 *   |                     Payload Data continued ...                |
 *   +---------------------------------------------------------------+
 *   version 1--------4
 *    0                   1                   2                   3
 *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *   +-+-+-+-+-------+-+-------------+-------------------------------+
 *   |M|R|R|R| opcode|R| Payload len |    Extended payload length    |
 *   |O|S|S|S|  (4)  |S|     (7)     |             (16/63)           |
 *   |R|V|V|V|       |V|             |   (if payload len==126/127)   |
 *   |E|1|2|3|       |4|             |                               |
 *   +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
 *   |     Extended payload length continued, if payload len == 127  |
 *   + - - - - - - - - - - - - - - - +-------------------------------+
 *   |                               |         Extension data        |
 *   +-------------------------------+ - - - - - - - - - - - - - - - +
 *   :                                                               :
 *   +---------------------------------------------------------------+
 *   :                       Application data                        :
 *   +---------------------------------------------------------------+
 * 
 * </pre>
 * <li>类型名称：
 * <li>说明：cluster协议响应编码器
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
public class ClustersCoder extends CoderHandler {
	private static Log log = LogFactory.getLog(ClustersCoder.class);// 日志记录器
	public void process(Client sockector) {
		Iterator<Response> msgs = sockector.getResponseMsgs().iterator();		
		while(msgs.hasNext()){
			Response msg = msgs.next();
			
			try{
				if(sockector.isHandShak()){
					broundMsg(sockector,msg);
				}else{
					if(!sockector.isClient()){
						sockector.setHandShak(true);//握手已经完成
					}
				}
			}catch(IOException e){
				e.printStackTrace();
			}// 创建响应头部信息
		}
	}
	
	/**
	 * 
	 * <li>方法名：broundMsg
	 * <li>@param msg
	 * <li>@throws IOException
	 * <li>返回类型：void
	 * <li>说明：对clusters协议进行编码
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-8-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void broundMsg(Client sockector, Response msg) throws IOException{		
		byte[] msgs = CoderUtils.toByte(msg.getBody());
		ClustersMessage messageFrame = sockector.getRequestWithFile().<ClustersMessage>getMessageHeader();
		
		if(messageFrame == null){
			messageFrame = new ClustersMessage();
		}
		messageFrame.setDateLength(msgs.length);
		
		byte[] headers = new byte[2];
		// todo list
		headers[0] = ClustersMessage.FIN;// 需要调整
		headers[0] |= messageFrame.getRsv1() | messageFrame.getRsv2() | messageFrame.getRsv3() | ClustersMessage.TXT;
		headers[1] = 0;
		//headers[1] |=  messageFrame.getMask() | messageFrame.getPayloadLen();		
		headers[1] |=  0x00 | messageFrame.getPayloadLen();
		msg.appendBytes(headers);// 头部控制信息
		
		if (messageFrame.getPayloadLen() == ClustersMessage.HAS_EXTEND_DATA) {// 处理数据长度为126位的情况
			msg.appendBytes(CoderUtils.shortToByte(messageFrame.getPayloadLenExtended()));
		} else if (messageFrame.getPayloadLen() == ClustersMessage.HAS_EXTEND_DATA_CONTINUE) {// 处理数据长度为127位的情况
			msg.appendBytes(CoderUtils.longToByte(messageFrame.getPayloadLenExtendedContinued()));
		}

		/*if(messageFrame.isMask()){// 做了掩码处理的，需要传递掩码的key
			byte[] keys = messageFrame.getMaskingKey();
			msg.appendBytes(messageFrame.getMaskingKey());
			
			for(int i = 0; i < msgs.length; ++i){// 进行掩码处理
				msgs[i] ^= keys[i % 4];
			}
		}*/
		
		msg.appendBytes(msgs);
		
		sockector.getRequestWithFile().clear();// 清理每次连接交互的数据
    }
	
	public void handShak(Client sockector) {
		Response response = new Response();		
		StringBuilder sb = new StringBuilder();		
		try{
			ClustersHandShak handShak = new ClustersHandShak(generateKey(),InetAddress.getLocalHost().getHostAddress().toString());
			sb.append(ClustersConstants.CLUSTERS).append("\r\n")			
			.append(ClustersConstants.HOST).append(":").append(handShak.getHost()).append("\r\n")
			.append(ClustersConstants.KEY).append(":").append(handShak.getKey()).append("\r\n")
			.append(ClustersConstants.PROTOCOL).append(":").append(ClustersConstants.PROTOCOL).append("\r\n");
			sockector.setHandShakObject(handShak);// 设置握手对象
		}catch(UnknownHostException e){
			e.printStackTrace();
		}
		
		response.setBody(sb.toString());
		
		sockector.sendMessage(response);
		//sockector.sendDirectMessage(response);// 发送消息
		log.info("the response: " + sb.toString());		
	}
	
	private String generateKey(){
		String key = UUID.randomUUID().toString().replace("-", "");
		return key.substring(0, 24);
	}
}
