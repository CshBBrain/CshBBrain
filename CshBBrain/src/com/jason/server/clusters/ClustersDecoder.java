/**
 * <li>文件名：HttpDecoder.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.clusters;

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
public class ClustersDecoder extends DecoderHandler {
	private static Log log = LogFactory.getLog(ClustersDecoder.class);// 日志记录器
	public static final String TMP_ROOT = "tmpRoot";// 临时文件目录
	public static final String tmpRoot = Config.getStr(TMP_ROOT);// 文件保存临时目录
	private static final Pattern PARAM_PATTERN = Pattern.compile("([^=]*)=([^&]*)&*");	
	
	public void process(ByteBuffer buffer, Client sockector){		
		if(sockector.isHandShak()){// 已经握手处理				
			// 通过复杂的数据帧格式来传递数据
			parser(buffer,sockector);
		}else{// 进行握手处理
			String msg = CoderUtils.decode(buffer);
			if(MyStringUtil.isBlank(msg)){
				return;
			}
			
			log.info("the msg received: \r\n" + msg);
			
			if(sockector.isClient()){// 接收到本方客户端发送的握手响应包
				this.processClientHandShak(sockector, msg);// 处理客户端连接握手
			}else{
				HashMap<String,String> requestData = new HashMap<String,String>();
				sockector.getRequestWithFile().setRequestData(requestData);
				
				try{
					ClustersRequest requestInfo = parserRequest(msg);
					
					requestData.put(Constants.FILED_MSG, generateHandshake(requestInfo));
					requestData.put(Constants.HANDSHAKE, Constants.HANDSHAKE);
				}catch(UnsupportedEncodingException e){
					e.printStackTrace();
				}
			}			
		}
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
    private void parser(ByteBuffer buffer, Client sockector){
    	Request requestData= sockector.getRequestWithFile();
    	ClustersMessage messageFrame = requestData.<ClustersMessage>getMessageHeader();
    	if(messageFrame == null){// 读取消息帧信息    		
        	messageFrame = new ClustersMessage();
        	requestData.setMessageHeader(messageFrame);
        	requestData.setByteDatas(new ArrayList<byte[]>(2));
        	
        	byte[] headers = new byte[8];// 数据帧的头部信息    
        	buffer.get(headers, 0, 2);
    		int bt, b2;
    		bt = headers[0];
    		messageFrame.setFin((byte) (bt & ClustersMessage.FIN));// 后面是否有续帧数据标识
    		messageFrame.setRsv1((byte) (bt & ClustersMessage.RSV1));// 保留标识1
    		messageFrame.setRsv2((byte) (bt & ClustersMessage.RSV2));// 保留标识2
    		messageFrame.setRsv3((byte) (bt & ClustersMessage.RSV3));// 保留标识3
    		messageFrame.setOpcode((byte) (bt & ClustersMessage.OPCODE));//标识数据的格式，以及帧的控制，如：01标识数据内容是 文本，08标识：要求远端去关闭当前连接。 

    		bt = headers[1];
    		messageFrame.setMask((byte) (bt & ClustersMessage.MASK));// 是否mask标识
    		
    		/*如果小于126 表示后面的数据长度是 [Payload len] 的值。（最大125byte） 
              等于 126 表示之后的16 bit位的数据值标识数据的长度。（最大65535byte） 
              等于 127 表示之后的64 bit位的数据值标识数据的长度。（一个有符号长整型的最大值）*/
    		int dataLen = bt & ClustersMessage.PAYLOADLEN;// 数据长度位数

    		if (dataLen == ClustersMessage.HAS_EXTEND_DATA) {// read next 16 bit
    			buffer.get(headers, 0, 2);// 读取2位数字
    			bt = headers[0];
    			b2 = headers[1];
    			messageFrame.setDateLength(CoderUtils.toShort((byte)bt, (byte)b2));
    		} else if (dataLen == ClustersMessage.HAS_EXTEND_DATA_CONTINUE) {// read next 32 bit
    			buffer.get(headers, 0, 8);// 读取8位数字
    			messageFrame.setDateLength(CoderUtils.toLong(headers));
    		} else {
    			messageFrame.setDateLength(dataLen);
    		}
    		
    		requestData.setDataSizeLeftLong(dataLen);
    		
    		if (messageFrame.isMask()){
    			buffer.get(headers, 0, 4);// 读取mask的key
    			messageFrame.setMaskingKey(headers[0],headers[1],headers[2],headers[3]);
    		}
    	}
    	
    	if(!requestData.readFinish()){// 是否读取完数据,读取完毕对数据进行处理
    		int dataLength = buffer.limit() - buffer.position();
    		byte[] datas = new byte[dataLength];
    		buffer.get(datas);
    		
    		if(messageFrame.isMask()){// 做加密处理
				for (int i = 0; i < dataLength; i++) {
					datas[i] ^= messageFrame.getMaskingKey()[(int) (requestData.getDataPosition() % 4)];
					requestData.setDataPosition(requestData.getDataPosition() + 1);
				}
			}else{// 没做加密处理
				requestData.setDataSizeLeftLong(requestData.getDataSizeLeftLong() - dataLength);
				requestData.setDataPosition(requestData.getDataPosition() + dataLength);
			}
    		
    		requestData.getByteDatas().add(datas);
    		
    		try{
				String a = new String(datas,"utf-8");
				log.info("jason,the msg is : " + a);
				/*requestData.getByteDatas();
				
				HashMap<String,String> data = new HashMap<String,String>();
				sockector.getRequestWithFile().setRequestData(data);
				data.put(Constants.FILED_MSG, a);*/
				// 对数据进行处理
				sockector.getRequestWithFile().setRequestData(MyStringUtil.parseKeyValue(requestData.getRequestMessage()));				
			}catch(Exception e){
				log.info(e.getMessage());// 日志消息
			}
    	}    	
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
	private ClustersRequest parserRequest(String requestData) throws UnsupportedEncodingException{		
			// 解析握手信息
			ClustersRequest requestInfo = new ClustersRequest();
			
			String[] requestDatas = requestData.split("\r\n");
			
			if(requestDatas.length < 0){
				return null;
			}
			
			String line = requestDatas[0];
			if(!line.equalsIgnoreCase(ClustersConstants.CLUSTERS)){
				return null;
			}	       
	        
	        for(int i = 1; i < requestDatas.length; ++i){
	        	// 解析单条请求信息        	
	        	line = requestDatas[i];
	        	
	        	String[] parts = line.split(":", 2);
	            if (parts.length != 2){
	            	log.info("Wrong field format: " + line);
	                return null;
	            }
	            
	            String name = parts[0].toLowerCase();
	            String value = parts[1].toLowerCase();
	            
	            if(name.equals("host")){
	                requestInfo.setHost(value);
	            }else if(name.equals("key")){// 获取随机码
	            	requestInfo.setDigest(getKey(parts[1]));// 设置签名
	            }else if(name.equals("protocol")){//获取安全控制版本
	            	requestInfo.setProtocol(value);// 设置协议
	            }else{
	            	log.info("Unexpected header field: " + line);
	            }
	        }
	        
	        return requestInfo;
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
	public String generateHandshake(ClustersRequest requestInfo) throws UnsupportedEncodingException{
		StringBuilder sb = new StringBuilder();		
		sb.append(ClustersConstants.CLUSTERS).append("\r\n")			
		.append(ClustersConstants.HOST).append(":").append(requestInfo.getHost()).append("\r\n")
		.append(ClustersConstants.ACCEPT).append(":").append(requestInfo.getDigest()).append("\r\n")
		.append(ClustersConstants.PROTOCOL).append(":").append(requestInfo.getProtocol()).append("\r\n");
		
		log.info("the response: " + sb.toString());
		
		return sb.toString();
	}
	
	/**
	 * 
	 * <li>方法名：processClientHandShak
	 * <li>@param sockector
	 * <li>@param msg
	 * <li>@return
	 * <li>返回类型：Boolean
	 * <li>说明：
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-22
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void processClientHandShak(Client sockector, String msg){
		
		String[] requestDatas = msg.split("\r\n");		
		if(requestDatas.length < 4){
			return;
		}
		
		ClustersHandShak handShak = sockector.<ClustersHandShak>getHandShakObject();
		String line = requestDatas[0];
		if(!line.equalsIgnoreCase(ClustersConstants.CLUSTERS)){// 检查协议名 是否为CshBBrain
			return;
		}
		
		String[] keyValue = requestDatas[1].split(ClustersConstants.CLUSTERS_SPLIT_DOT);
		
		if(keyValue.length < 2 || !keyValue[1].equalsIgnoreCase(handShak.getHost())){// 检查host是否为本机发出时附带的host
			return;
		}
		
		keyValue = requestDatas[2].split(ClustersConstants.CLUSTERS_SPLIT_DOT);
		if(keyValue.length < 2 || !keyValue[1].equals(getKey(handShak.getKey()))){// 检查验证key是否正确
			return;
		}
		
		keyValue = requestDatas[3].split(ClustersConstants.CLUSTERS_SPLIT_DOT);
		if(keyValue.length < 2 || !keyValue[1].equalsIgnoreCase(ClustersConstants.PROTOCOL)){// 检查协议是否正确
			return;
		}
        
		sockector.setHandShak(true);// 握手验证正确，完成握手处理        
	}
}
