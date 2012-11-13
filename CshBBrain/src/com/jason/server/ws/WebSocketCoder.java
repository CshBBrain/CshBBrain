/**
 * <li>文件名：HttpCoder.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.ws;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jason.server.Response;
import com.jason.server.Client;
import com.jason.server.hander.CoderHandler;
import com.jason.server.ws.biz.Constants;
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
 * <li>说明：http协议响应编码器
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
public class WebSocketCoder extends CoderHandler {
	private static Log log = LogFactory.getLog(WebSocketCoder.class);// 日志记录器
	public void process(Client sockector) {
		Response msg = sockector.getResponseMsgsNotCode().poll();		
		while(msg != null){			
			try{
				if(sockector.isHandShak()){
					broundMsg(sockector,msg);					
				}else{
					sockector.setHandShak(true);//握手已经完成
				}
				
				msg.bufferedContent();// 缓存内容
				sockector.getResponseMsgs().add(msg);
			}catch(IOException e){
				e.printStackTrace();
			}// 创建响应头部信息
			
			msg = sockector.getResponseMsgsNotCode().poll();
		}		
	}
	
	/**
	 * 
	 * <li>方法名：broundMsg
	 * <li>@param msg
	 * <li>@throws IOException
	 * <li>返回类型：void
	 * <li>说明：将消息的头部和尾部分隔符加上
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-8-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void broundMsg(Client sockector, Response msg) throws IOException{
		if(sockector.getProtocolVersionInt() <= WebSocketConstants.SPLITVERSION0){// 通过0x00,0xff分隔数据
			/*ByteBuffer buffer = ByteBuffer.allocate(msg.getBody().getBytes(Utf8Coder.UTF8).length + 2);
			buffer.put((byte)0x00);
			buffer.put(msg.getBody().getBytes(Utf8Coder.UTF8));
			buffer.put((byte)0xFF);
			msg.setBody(Utf8Coder.decode(buffer));*/
			//log.info("the bg and end : " + Constants.BEGIN_MSG + " : " + Constants.END_MSG);
			msg.setBody(Constants.BEGIN_MSG + msg.getBody() + Constants.END_MSG);
			log.info(msg.getBody());
		}else{
			codeVersion6(sockector, msg);
		}
    }
	
	/**
	 * 
	 * <li>方法名：codeVersion6
	 * <li>@param sockector
	 * <li>@param msg
	 * <li>返回类型：void
	 * <li>说明：对websocket协议进行编码
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-2
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void codeVersion6(Client sockector, Response msg){		
		byte[] msgs = CoderUtils.toByte(msg.getBody());
		WebSocketMessage messageFrame = sockector.getRequestWithFile().<WebSocketMessage>getMessageHeader();
		
		if(messageFrame == null){
			messageFrame = new WebSocketMessage();
		}
		messageFrame.setDateLength(msgs.length);
		
		byte[] headers = new byte[2];
		// todo list
		headers[0] = WebSocketMessage.FIN;// 需要调整
		headers[0] |= messageFrame.getRsv1() | messageFrame.getRsv2() | messageFrame.getRsv3() | WebSocketMessage.TXT;
		headers[1] = 0;
		//headers[1] |=  messageFrame.getMask() | messageFrame.getPayloadLen();		
		headers[1] |=  0x00 | messageFrame.getPayloadLen();
		msg.appendBytes(headers);// 头部控制信息
		
		if (messageFrame.getPayloadLen() == WebSocketMessage.HAS_EXTEND_DATA) {// 处理数据长度为126位的情况
			msg.appendBytes(CoderUtils.shortToByte(messageFrame.getPayloadLenExtended()));
		} else if (messageFrame.getPayloadLen() == WebSocketMessage.HAS_EXTEND_DATA_CONTINUE) {// 处理数据长度为127位的情况
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
		msg.resetCurrentBuffer();
		
		sockector.getRequestWithFile().clear();// 清理每次连接交互的数据
	}
}
