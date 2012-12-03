/**
 * <li>文件名：HttpDecoder.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.ws;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sun.misc.BASE64Encoder;

import com.jason.Config;
import com.jason.server.Request;
import com.jason.server.Client;
import com.jason.server.hander.DecoderHandler;
import com.jason.server.ws.biz.Constants;
import com.jason.util.MyStringUtil;
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
 * <li>说明：http协议解码器
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
public class WebSocketDecoder extends DecoderHandler {
	private static Log log = LogFactory.getLog(WebSocketDecoder.class);// 日志记录器
	public static final String TMP_ROOT = "tmpRoot";// 临时文件目录
	public static final String tmpRoot = Config.getStr(TMP_ROOT);// 文件保存临时目录
	private static final Pattern PARAM_PATTERN = Pattern.compile("([^=]*)=([^&]*)&*");	
	
	public void process(ByteBuffer buffer, Client sockector){		
		if(sockector.isHandShak()){// 已经握手处理				
			if(sockector.getProtocolVersionInt() < WebSocketConstants.SPLITVERSION6){// 通过0x00,0xff分隔数据		
				String msg = CoderUtils.decode(buffer);
				if(MyStringUtil.isBlank(msg)){
					return;
				}
				
				log.info("the msg received: " + msg);
				
				/*HashMap<String,String> requestData = new HashMap<String,String>();
				sockector.addRequest(requestData);
				requestData.put(Constants.FILED_MSG, msg);*/
				Request requestData= sockector.getRequestWithFile();		    	
		    	String requestIndex = requestData.getCrrentRequestIndex();
				
				HashMap<String,String> data = MyStringUtil.parseKeyValue(msg);
				sockector.addRequest(data);
				data.put(WebSocketConstants.REQUEST_INDEX, requestIndex);
				
			}else{// 通过复杂的数据帧格式来传递数据
				parserVersion6(buffer,sockector);
			}
		}else{// 进行握手处理
			String msg = CoderUtils.decode(buffer);
			if(MyStringUtil.isBlank(msg)){
				return;
			}
			
			log.info("the msg received: \r\n" + msg);
			
			HashMap<String,String> requestData = new HashMap<String,String>();
			sockector.addRequest(requestData);
			
			try{
				WebSocketRequest requestInfo = parserRequest(msg);
				
				requestData.put(Constants.FILED_MSG, generateHandshake(requestInfo));
				requestData.put(Constants.HANDSHAKE, Constants.HANDSHAKE);
				sockector.setProtocolVersion(requestInfo.getSecVersion().toString());//设置协议版本
			}catch(UnsupportedEncodingException e){
				e.printStackTrace();
			}
		}
	}
	
	private void parserVersionBefore6(ByteBuffer buffer, Client sockector){
		/*String msg = CoderUtils.decode(buffer);
		if(MyStringUtil.isBlank(msg)){
			return;
		}
		
		log.info("the msg received: " + msg);
		
		HashMap<String,String> requestData = new HashMap<String,String>();
		sockector.addRequest(requestData);
		requestData.put(Constants.FILED_MSG, msg);*/
		
		
    	do{
	    	Request requestData= sockector.getRequestWithFile();
	    	
	    	String requestIndex = requestData.getCrrentRequestIndex();
	    	WebSocketMessage messageFrame = null;
	    	if(MyStringUtil.isBlank(requestIndex)){// 没有出现半包的情况
	    		messageFrame = new WebSocketMessage();
		    	requestIndex = requestData.setMessageHeader(messageFrame);
	    	}else{// 出现半包的情况
	    		messageFrame = requestData.<WebSocketMessage>getMessageHeader(requestIndex);
	    	}
	    	
	    	if(requestData.readFinish()){
		    	requestData.setByteDatas(new ArrayList<byte[]>(2));		    	
	    	}
	    	
	    	if(!messageFrame.isReadFinish()){
	    		messageFrame.parseMessageHeader(buffer);// 读取解析消息头
	    		requestData.setDataSizeLeftLong(messageFrame.getDateLength());// 设置数据长度
	    	}
	    	
	    	if(!requestData.readFinish()){// 是否读取完数据
	    		int bufferDataLength = buffer.limit() - buffer.position();
	    		int dataLength = bufferDataLength > requestData.getDataSizeLeftLong() ? requestData.getDataSizeLeftLong().intValue() : bufferDataLength;
	    		    		
	    		byte[] datas = new byte[dataLength];
	    		
	    		if(dataLength > 0){
		    		buffer.get(datas);
		    		
		    		if(messageFrame.isMask()){// 做加密处理
						for (int i = 0; i < dataLength; i++) {
							datas[i] ^= messageFrame.getMaskingKey()[(int) (requestData.getDataPosition() % 4)];
							requestData.setDataPosition(requestData.getDataPosition() + 1);
						}
					}else{// 没做加密处理					
						requestData.setDataPosition(requestData.getDataPosition() + dataLength);
					}
		    		
		    		requestData.setDataSizeLeftLong(requestData.getDataSizeLeftLong() - dataLength);// 设置剩余数量的数据
		    		
		    		requestData.getByteDatas().add(datas);
	    		}
	    		
	    		if(requestData.readFinish()){// 消息读取完毕，放入处理队列中
					log.info("jason,the msg is : " + requestData.getRequestMessage());
					
					HashMap<String,String> data = MyStringUtil.parseKeyValue(requestData.getRequestMessage());
					sockector.addRequest(data);
					data.put(WebSocketConstants.REQUEST_INDEX, requestIndex);
					
					requestData.clear();// 清空字节数组
	    		}else{
	    			log.info("jason,the msg is : 78" );
	    		}
	    	}
    	}while(buffer.limit() > buffer.position());// 处理粘包的情况
	}
    
	/**
	 * 
	 * <li>方法名：parser
	 * <li>@param buffer
	 * <li>@param sockector
	 * <li>返回类型：void
	 * <li>说明：解析版本6以后的数据帧格式
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
    private void parserVersion6(ByteBuffer buffer, Client sockector){    	
    	do{
	    	Request requestData= sockector.getRequestWithFile();
	    	
	    	String requestIndex = requestData.getCrrentRequestIndex();
	    	WebSocketMessage messageFrame = null;
	    	if(MyStringUtil.isBlank(requestIndex)){// 没有出现半包的情况
	    		messageFrame = new WebSocketMessage();
		    	requestIndex = requestData.setMessageHeader(messageFrame);
	    	}else{// 出现半包的情况
	    		messageFrame = requestData.<WebSocketMessage>getMessageHeader(requestIndex);
	    	}
	    	
	    	if(requestData.readFinish()){
		    	requestData.setByteDatas(new ArrayList<byte[]>(2));		    	
	    	}
	    	
	    	if(!messageFrame.isReadFinish()){
	    		messageFrame.parseMessageHeader(buffer);// 读取解析消息头
	    		requestData.setDataSizeLeftLong(messageFrame.getDateLength());// 设置数据长度
	    	}
	    	
	    	if(!requestData.readFinish()){// 是否读取完数据
	    		int bufferDataLength = buffer.limit() - buffer.position();
	    		int dataLength = bufferDataLength > requestData.getDataSizeLeftLong() ? requestData.getDataSizeLeftLong().intValue() : bufferDataLength;
	    		    		
	    		byte[] datas = new byte[dataLength];
	    		
	    		if(dataLength > 0){
		    		buffer.get(datas);
		    		
		    		if(messageFrame.isMask()){// 做加密处理
						for (int i = 0; i < dataLength; i++) {
							datas[i] ^= messageFrame.getMaskingKey()[(int) (requestData.getDataPosition() % 4)];
							requestData.setDataPosition(requestData.getDataPosition() + 1);
						}
					}else{// 没做加密处理					
						requestData.setDataPosition(requestData.getDataPosition() + dataLength);
					}
		    		
		    		requestData.setDataSizeLeftLong(requestData.getDataSizeLeftLong() - dataLength);// 设置剩余数量的数据
		    		
		    		requestData.getByteDatas().add(datas);
	    		}
	    		
	    		if(requestData.readFinish()){// 消息读取完毕，放入处理队列中
					log.info("jason,the msg is : " + requestData.getRequestMessage());
					
					HashMap<String,String> data = MyStringUtil.parseKeyValue(requestData.getRequestMessage());
					sockector.addRequest(data);
					data.put(WebSocketConstants.REQUEST_INDEX, requestIndex);
					
					requestData.clear();// 清空字节数组
	    		}else{
	    			log.info("jason,the msg is : 78" );
	    		}
	    	}
    	}while(buffer.limit() > buffer.position());// 处理粘包的情况
	}

	/**
     * 
     * <li>方法名：getKey
     * <li>@param key
     * <li>@return
     * <li>返回类型：String
     * <li>说明：
     * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
     * <li>创建日期：2012-8-21
     * <li>修改人： 
     * <li>修改日期：
     */
    public static String getKey(String key) {  
	       // CHROME WEBSOCKET VERSION 8中定义的GUID，详细文档地址：http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-10  
	       String guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	       key += guid;
	       log.info(key);
	       try {  
	           MessageDigest md = MessageDigest.getInstance("SHA-1");  
	           md.update(key.getBytes("iso-8859-1"), 0, key.length());  
	           byte[] sha1Hash = md.digest();  
	           key = base64Encode(sha1Hash);  
	       } catch (NoSuchAlgorithmException e) {  
	           e.printStackTrace();  
	       } catch (UnsupportedEncodingException e) {  
	            e.printStackTrace();
	       }  
	       return key;  
	   }  

	public static String base64Encode(byte[] input) {  
		BASE64Encoder encoder = new BASE64Encoder();  
		String base64 = encoder.encode(input);  
		return base64;  
	}
	
	/**
	 * 
	 * <li>方法名：byteCollectionToString
	 * <li>@param collection
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：Creates a string from given byte collection.
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-9-28
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	protected String byteCollectionToString(Collection<Byte> collection) {
	    byte[] byteArray = new byte[collection.size()];
	    Integer i = 0;
	    for (Iterator<Byte> iterator = collection.iterator(); iterator
	            .hasNext();) {
	        byteArray[i++] = iterator.next();
	    }
	    return new String(byteArray, Charset.forName("UTF-8"));
	}
	
	/**
	 * 
	 * <li>方法名：parserRequest
	 * <li>@param str
	 * <li>@param requestInfo
	 * <li>返回类型：void
	 * <li>说明：对请求参数进行解析
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-9-21
	 * <li>修改人： 
	 * <li>修改日期：
	 * @throws UnsupportedEncodingException 
	 */
	private WebSocketRequest parserRequest(String requestData) throws UnsupportedEncodingException{		
			// 解析握手信息
			WebSocketRequest requestInfo = new WebSocketRequest();
			
			String[] requestDatas = requestData.split("\r\n");
			
			if(requestDatas.length < 0){
				return null;
			}
			
			String line = requestDatas[0];
	        String[] requestLine = line.split(" ");
	        if (requestLine.length < 2){
	        	log.info("Wrong Request-Line format: " + line);
	            return null;
	        }
	        
	        requestInfo.setRequestUri(requestLine[1]);
	        
	        for(int i = 1; i < requestDatas.length; ++i){
	        	// 解析单条请求信息        	
	        	line = requestDatas[i];
	        	
	        	// 如果获取到空行，则读取后面的内容信息
	        	if(line.equalsIgnoreCase(Constants.BLANK)){// 版本0---3放到消息体中的
	        		if((i + 1) < requestDatas.length){// 有发送内容到服务器端
	        			line = requestDatas[i + 1] + "00000000";
	        			byte[] token = line.getBytes();//.substring(0, 8).getBytes(Utf8Coder.UTF8);
	        			try {
	        				requestInfo.setDigest(this.makeResponseToken(requestInfo, token));// 设置签名
						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
						}
	        			break;
	        		}
	        	}
	        	
	        	String[] parts = line.split(": ", 2);
	            if (parts.length != 2){
	            	log.info("Wrong field format: " + line);
	                return null;
	            }
	            
	            String name = parts[0].toLowerCase();
	            String value = parts[1].toLowerCase();
	
	            if (name.equals("upgrade")) {
	                if (!value.equals("websocket")){
	                	log.info("Wrong value of upgrade field: " + line);
	                	return null;
	                }
	                requestInfo.setUpgrade(true);
	            }else if(name.equals("connection")) {
	                if (!value.equals("upgrade")){
	                	log.info("Wrong value of connection field: " + line);
	                }
	                requestInfo.setConnection(true);
	            }else if(name.equals("host")){
	                requestInfo.setHost(value);
	            }else if (name.equals("origin")){
	                requestInfo.setOrigin(value);
	            }else if((name.equals("sec-websocket-key1")) || (name.equals("sec-websocket-key2"))){
	            	log.info(name + ":" + value);
	            	Integer spaces = new Integer(0);
	                Long number = new Long(0);
	                for (Character c : parts[1].toCharArray()){
	                    if (c.equals(' '))
	                        ++spaces;
	                    if (Character.isDigit(c)){
	                        number *= 10;
	                        number += Character.digit(c, 10);
	                    }
	                }
	                number /= spaces;
	                
	                if (name.endsWith("key1")){
	                    requestInfo.setKey1(number);
	                }else{
	                    requestInfo.setKey2(number);
	                }
	            }else if(name.equals("cookie")){
	                requestInfo.setCookie(value);
	            }else if(name.equals("sec-websocket-key")){// 版本4以及以上放到sec key中
	            	requestInfo.setDigest(getKey(parts[1]));// 设置签名
	            }else if(name.equals("sec-websocket-version")){//获取安全控制版本
	            	requestInfo.setSecVersion(Integer.valueOf(value));// 设置版本
	            }else if(name.equals("sec-websocket-extensions")){//获取安全控制版本	            	
	            	log.info(value);
	            }else{
	            	log.info("Unexpected header field: " + line);
	            }
	        }
	        
	        return requestInfo;
	}
	
	/**
	 * 
	 * <li>方法名：makeResponseToken
	 * <li>@param requestInfo
	 * <li>@param token
	 * <li>@return
	 * <li>@throws NoSuchAlgorithmException
	 * <li>返回类型：byte[]
	 * <li>说明：
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-8-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	protected String makeResponseToken(WebSocketRequest requestInfo, byte[] token)throws NoSuchAlgorithmException {
		MessageDigest md5digest = MessageDigest.getInstance("MD5");
		for(Integer i = 0; i < 2; ++i){
			byte[] asByte = new byte[4];
			long key = (i == 0) ? requestInfo.getKey1().intValue() : requestInfo.getKey2().intValue();
			asByte[0] = (byte) (key >> 24);
			asByte[1] = (byte) ((key << 8) >> 24);
			asByte[2] = (byte) ((key << 16) >> 24);
			asByte[3] = (byte) ((key << 24) >> 24);
			md5digest.update(asByte);
		}
		md5digest.update(token);		
		return new String(md5digest.digest());
	}
	
	/**
	 * 
	 * <li>方法名：generateHandshake
	 * <li>@param requestInfo
	 * <li>@throws UnsupportedEncodingException
	 * <li>返回类型：void
	 * <li>说明：
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-8-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public String generateHandshake(WebSocketRequest requestInfo) throws UnsupportedEncodingException{
		StringBuilder sb = new StringBuilder();
		if(requestInfo.getSecVersion() < 4){// 版本0--3
		sb.append("HTTP/1.1 101 WebSocket Protocol Handshake").append("\r\n")
		.append("Upgrade: WebSocket").append("\r\n")
		.append("Connection: Upgrade").append("\r\n")
		.append("Sec-WebSocket-Origin: ").append(requestInfo.getOrigin()).append("\r\n")
		.append("Sec-WebSocket-Location: ws://").append(requestInfo.getHost()).append(requestInfo.getRequestUri()).append("\r\n");
		
		if(requestInfo.getCookie() != null){
			sb.append("cookie: ").append(requestInfo.getCookie()).append("\r\n");
		}
		
		sb.append("\r\n"); // 写入空行
		
		sb.append(requestInfo.getDigest());
		//ByteBuffer buffer = ByteBuffer.allocate(sb.length() + requestInfo.getDigest().length); 		
		//buffer.put(sb.toString().getBytes(Utf8Coder.UTF8)).put(requestInfo.getDigest());
		}else{// 大于等于版本4
			sb.append("HTTP/1.1 101 Switching Protocols").append("\r\n")
			.append("Upgrade: websocket").append("\r\n")
			.append("Connection: Upgrade").append("\r\n")
			.append("Sec-WebSocket-Accept: ").append(requestInfo.getDigest()).append("\r\n")
			.append("Sec-WebSocket-Origin: ").append(requestInfo.getOrigin()).append("\r\n")
			.append("Sec-WebSocket-Location: ws://").append(requestInfo.getHost()).append(requestInfo.getRequestUri()).append("\r\n");
			//.append("Sec-WebSocket-Protocol: chat").append("\r\n");
			
			sb.append("\r\n"); // 写入空行			
		}
		log.info("the response: " + sb.toString());
		
		return sb.toString();
	}	
}
