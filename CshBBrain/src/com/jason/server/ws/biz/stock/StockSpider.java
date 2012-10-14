package com.jason.server.ws.biz.stock;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import com.jason.server.MasterServer;
import com.jason.server.Response;
import com.jason.util.MyStringUtil;

/**
 * 
 * <li>类型名称：
 * <li>说明：股票指数爬虫
 * <li>创建人： 陈嗣洪
 * <li>创建日期：2011-10-8
 * <li>修改人： 
 * <li>修改日期：
 */
public class StockSpider {
	private static final String EQUAL_FLAG = "=";// 等号
	private static final String REFS_FLAG = "\"";// 引号
	private static final String UP_CHANGE = "1";// 上涨
	private static final String DOWN_CHANGE = "2";// 下跌
	private static final String NO_CHANGE = "0";// 不变
	private static final String FLAG_100 = "%";// 百分号
	private static final DecimalFormat myformat = new  DecimalFormat("#####0.00"); 
	private static final String DOT_FLAG = ",";// 逗号
	private static final String SPECIAL_FLAG = ";";// 逗号
	private static final String MAT_FLAG = ":";// 冒号
	private static final String CONFIG_FILE = "stock.properties";// 逗号
	
	private static final String POP_NAME = "\"name\"";// 股指名
	private static final String POP_RAW = "\"raw\"";// 股指变化方向
	private static final String POP_CHANGE = "\"change\"";// 股指该变量
	private static final String POP_CHANGE_SCALE = "\"changeScale\"";// 股指改变幅度
	private static final String POP_POINTS = "\"points\"";// 股指名
	
	private static StockSpider stockSpider = new StockSpider();// stockspider 单实例
	private Thread stockCollectThread;// 股指数据采集线程
	private ArrayList<Stock> stockUrl = new ArrayList<Stock>();// 采集股指地址
	private HashMap<Stock, String> stockData = new HashMap<Stock, String>();// 采集到的数据
	private boolean noStopRequested = true;
	
	public StockSpider(){
		this.init();
	}
	
	public static StockSpider getInstance(){
		return stockSpider;
	}
	
	public static void main(String[] args){
		new StockSpider();		
	}
	
	public void init(){
		// 读取配置文件，根据配置文件创建配置信息
		Properties props = new Properties();
		try {
			InputStream fileInput = new FileInputStream(this.getClass().getResource("/").toString().replace("file:/", "") + CONFIG_FILE );

			// 加载配置文件
			props.load(fileInput);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("配置文件不存在，请仔细检查");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("配置文件读取错误");
		}
		
		Enumeration<String> names = (Enumeration<String>) props.propertyNames();
		while(names.hasMoreElements()){
			String key = names.nextElement();
			String countStr = props.getProperty(key);
			String values[] = countStr.split(DOT_FLAG);
			
			Stock stockUrl = new Stock();
			stockUrl.setName(key);
			
			if(values.length >= 2){ // 国内股指				
				stockUrl.setChina(true);
				stockUrl.setStockData(values[0]);
			}else{// 国外股指
				stockUrl.setChina(false);
				stockUrl.setStockData(countStr);
			}
			
			this.stockUrl.add(stockUrl);
		}		
		
		createStockCollectThread(Thread.NORM_PRIORITY);// 读取线程
	}
	
	// 创建请求读取调度线程
	private void createStockCollectThread(int serverPriority){
		Runnable readDistributeRunner = new Runnable(){
			public void run(){
				try{
					startStockCollect();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		};
		
		this.stockCollectThread = new Thread(readDistributeRunner);
		this.stockCollectThread.setName("股指数据读取线程");
		this.stockCollectThread.setPriority(serverPriority);
		this.stockCollectThread.start();
	}
	
	/**
	 * 
	 * <li>方法名：startStockCollect
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：
	 * <li>创建人：陈嗣洪
	 * <li>创建日期：2011-10-9
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	private void startStockCollect() {
		while(noStopRequested){
			long begin = System.currentTimeMillis();
			// 获取股指数据
			for(int i = 0; i < this.stockUrl.size(); ++i){
				Stock url = this.stockUrl.get(i);
				String result = this.getResult(url.getStockData());
				this.stockData.put(url, result);			
			}			
			
			Iterator<Stock> keys = this.stockData.keySet().iterator();
			StringBuilder sb = new StringBuilder("[");
			while(keys.hasNext()){
				Stock key = keys.next();
				String data = this.stockData.get(key);
				// 返回的数据格式为：国外格式：var hq_str_int_hangseng="恒生指数,17707.00,534.73,3.11";
				// 国内格式:var hq_str_sh000001="上证指数,2368.398,2365.343,2359.220,2377.541,2348.217,0,0,48818372,46104608498,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2011-09-30,15:03:09";
				if(!MyStringUtil.isBlank(data)){
					String datas[] = data.split(EQUAL_FLAG); 
					if(datas.length >= 2){
						String value = datas[1].replace(REFS_FLAG, "").replace(SPECIAL_FLAG, "");
						//System.out.println(value);
						if(key.isChina()){
							// 处理国内股指数据
							String[] values = value.split(DOT_FLAG);
							if(values.length >= 4){
								String name = values[0];
								String yestoday = values[2];// 昨日收盘股指
								String now = values[3];// 当前股指
								
								Float change = Float.valueOf(now) - Float.valueOf(yestoday);
								Float changeScale = change / Float.valueOf(yestoday) * 100;
								
								sb.append("{");
								
								if(change > 0.00f){
									sb.append(POP_RAW).append(MAT_FLAG).append(REFS_FLAG).append(UP_CHANGE).append(REFS_FLAG).append(DOT_FLAG);
								}else if(change == 0.00f){
									sb.append(POP_RAW).append(MAT_FLAG).append(REFS_FLAG).append(NO_CHANGE).append(REFS_FLAG).append(DOT_FLAG);
								}else{
									sb.append(POP_RAW).append(MAT_FLAG).append(REFS_FLAG).append(DOWN_CHANGE).append(REFS_FLAG).append(DOT_FLAG);
								}
								
								sb.append(POP_NAME).append(MAT_FLAG).append(REFS_FLAG).append(name).append(REFS_FLAG).append(DOT_FLAG)
								.append(POP_POINTS).append(MAT_FLAG).append(REFS_FLAG).append(now).append(REFS_FLAG).append(DOT_FLAG)
								.append(POP_CHANGE).append(MAT_FLAG).append(REFS_FLAG).append(myformat.format(change)).append(REFS_FLAG).append(DOT_FLAG)
								.append(POP_CHANGE_SCALE).append(MAT_FLAG).append(REFS_FLAG).append(myformat.format(changeScale)).append(FLAG_100).append(REFS_FLAG);
								
								sb.append("}");								
							}
						}else{
							// 处理国外股指数据
							String[] values = value.split(DOT_FLAG);
							if(values.length >= 4){
								sb.append("{");
								Float change = Float.valueOf(values[values.length -2]);								
								if(change > 0.00f){
									sb.append(POP_RAW).append(MAT_FLAG).append(REFS_FLAG).append(UP_CHANGE).append(REFS_FLAG);
								}else if(change == 0.00f){
									sb.append(POP_RAW).append(MAT_FLAG).append(REFS_FLAG).append(NO_CHANGE).append(REFS_FLAG);
								}else{
									sb.append(POP_RAW).append(MAT_FLAG).append(REFS_FLAG).append(DOWN_CHANGE).append(REFS_FLAG);
								}
								sb.append(DOT_FLAG)
								.append(POP_NAME).append(MAT_FLAG).append(REFS_FLAG).append(values[0]).append(REFS_FLAG).append(DOT_FLAG)
								.append(POP_POINTS).append(MAT_FLAG).append(REFS_FLAG).append(values[1]).append(REFS_FLAG).append(DOT_FLAG)
								.append(POP_CHANGE).append(MAT_FLAG).append(REFS_FLAG).append(values[2]).append(REFS_FLAG).append(DOT_FLAG)
								.append(POP_CHANGE_SCALE).append(MAT_FLAG).append(REFS_FLAG).append(values[3]).append(FLAG_100).append(REFS_FLAG);
								
								sb.append("}");
							}
						}
					}
					
					if(keys.hasNext()){
						sb.append(DOT_FLAG);
					}
				}
			}
			sb.append("]");
			long end = System.currentTimeMillis();
			
			
			Response rs = new Response();			
			rs.setBody(sb.toString());
			System.out.println(sb.toString());
			MasterServer.addBroadMessage(rs);// 将广播消息添加到websocketer服务器的广播消息队列中
						
			if(end - begin < 1000 * 3){
				try {
					//System.out.println("in sleep" + (end - begin));
					Thread.currentThread().sleep(1000 * 3 - (end - begin));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * 
	 * <li>方法名：getResult
	 * <li>@param urlStr
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：通过get方法获取股指数据
	 * <li>创建人：陈嗣洪
	 * <li>创建日期：2011-10-8
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static String getResult(String urlStr){
		URL url = null;
		HttpURLConnection connection = null;
		try {
			url = new URL(urlStr);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("GET");
			connection.setUseCaches(false);
			connection.connect();
			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
			out.flush();
			out.close();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection
			.getInputStream(), "gb2312"));
			StringBuffer buffer = new StringBuffer();
			String line = "";

			while ((line = reader.readLine()) != null){
				buffer.append(line);
			}
			
			reader.close();
			return buffer.toString();
		}catch(IOException e){
			e.printStackTrace();
			System.out.println("Error Url:" + urlStr);
		}finally{
			if(connection != null){
				connection.disconnect();
			}
		}
		return null;
	}
	
	/**
	 * 
	 * <li>方法名：stopStockSpider
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：停止股指采集器
	 * <li>创建人：陈嗣洪
	 * <li>创建日期：2011-10-9
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void stopStockSpider(){
		this.noStopRequested = false;
	}
}
