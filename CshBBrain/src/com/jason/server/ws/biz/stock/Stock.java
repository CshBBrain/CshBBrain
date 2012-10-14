package com.jason.server.ws.biz.stock;

public class Stock {
	// 处理后的为波动标识(1-上涨，2-下跌，0-不变),股指名称,股指最新价格，波动价格，波动比例
	private String name; //股指名称
	private String stockData;// 股票数据
	private boolean isChina = false;// 是国内的股票
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getStockData() {
		return stockData;
	}
	public void setStockData(String stockData) {
		this.stockData = stockData;
	}
	public boolean isChina() {
		return isChina;
	}
	public void setChina(boolean isChina) {
		this.isChina = isChina;
	}
}
