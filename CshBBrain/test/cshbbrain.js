var taskCount = 1;
function socket(){
	this.managerServer = "";//管理服务器地址
	this.isClusters = 1;// 是否为集群websocket服务器，1:表示不是，2：表示是集群
	this.bizServer = "ws://192.168.1.220:9090/";// 业务websocket服务器地址
	this.bizSocket = null;// 业务websocket服务器连接
	this.taskQueue = {};// 调用任务队列
	this.waitQueue = new Array();// 等待调用的队列
	this.taskKey = "taskKey";//任务调用key
	this.tryCount = 0;// 尝试连接的次数
	this.status = -1;// 业务websocket连接状态，-1：表示还没有创建连接，0：正在连接，1：表示没有连上服务器，2：正在关闭连接，3：表示已经关闭连接
	var self = this;	
	this.init = function(){// 初始化连接		
		if(self.isClusters == 2){// 服务器采用集群的方式
			var clusterSocket = new WebSocket(self.managerServer);
			clusterSocket.onopen = function(event){
				clusterSocket.send();// 获取biz地址请求
			}
			
			clusterSocket.onmessage = function(event){// 设置biz服务器地址，关闭连接
				self.bizServer = event.data;
				self.createBizSocket();
				clusterSocket.close();
			}
		}else{// 服务器没有采用集群的方式
			self.createBizSocket();
		}		
	}
	
	this.createBizSocket = function(){
		self.bizSocket = new WebSocket(self.bizServer);
		self.status = 0;// 正在连接中
		self.bizSocket.onopen = function(event){// 连接成功
			self.status = 1;// 成功连接上服务器
			self.tryCount = 0;// 尝试连接的次数置空
			var paramter = self.waitQueue.shift();
			while(paramter){
				self.taskQueue[paramter.timestamp] = paramter;
				self.bizSocket.send(paramter.url);
				paramter = self.waitQueue.shift();
			}			
		}
			
		this.bizSocket.onerror = function(event){
			try{
				self.bizSocket.close();
			}catch(e){}
			self.status = -1;// 正在关闭连接
			++self.tryCount;// 尝试连接的次数
			if(self.tryCount <= 10000){
				// 将执行队列中的任务添加到数组中
				for (prop in self.taskQueue){
					self.waitQueue.unshift(self.taskQueue[prop]);
				}
				
				self.init();// 继续连接
			}
			//alert("连接出错");
		}
		
		/*
		this.bizSocket.onclose = function(event){
			this.status = -1;// 已经完成关闭操作
			++this.tryCount;// 尝试连接的次数
		}*/
		
		this.bizSocket.onmessage = function(event){// 对业务服务器返回的业务代码进行处理，并调用回调函数
			eval(event.data);// 执行服务器端返回的js代码
			if(taskKey){// 有回调函数，则调用回调函数				
				try{
					if(self.taskQueue[taskKey].callBack){// 存在回调函数就调用
						self.taskQueue[taskKey].callBack(resultData);
					}
				}catch(e){
					self.taskQueue[taskKey].callBack(null);
					//alert("业务处理函数出现异常");
				}
				
				self.taskQueue[taskKey] = undefined;
			}
			//var stock = window.JSON.parse(event.data);		
	    };
	}
	
	this.currTime = function(formatStr){// 获取系统当前时间戳
		
		return ++taskCount;
		/*
		if(formatStr){
			return (new Date()).format(formatStr);
		}else{
			return (new Date()).valueOf();
		}*/
	}
	
	this.processURL = function(url,timestamp){// 对于请求地址进行处理，并添加任务调度key
		if(!url){
			return;
		}
		var single = "&";
		if(url.indexOf("?") == -1){
			single = "?";
		}
		
		return encodeURIComponent(url) + single + self.taskKey + "=" + timestamp;
	}
	
	this.getData = function(url,callBack){// 调用websocket获取数据
		var callKey = self.currTime();
		var requestUrl = self.processURL(url,callKey);
		console.log(url);

		//if(callBack){// 有回调函数，将函数放入到调度队列中
		var paramters = {timestamp:callKey,url:requestUrl,callBack:callBack};
		self.taskQueue[callKey] = {timestamp:callKey,url:requestUrl,callBack:callBack};
		//}
		switch(self.status){
			case -1:// 没有建立连接
				self.init();
				self.waitQueue.push(paramters);
				break;
			case 0:// 正在建立连接
				self.waitQueue.push(paramters);
				break;
			case 1:// 连接已经建立
				self.taskQueue[callKey] = paramters;
				self.bizSocket.send(requestUrl);
				break;
			case 2:// 正在关闭连接
				self.waitQueue.push(paramters);
				break;
			case 3:// 已经关闭连接
				self.waitQueue.push(paramters);
				break;
		}				
	}
	
	this.init();// 初始化websocket
}

var _ = new socket();// 创建websocket连接对象