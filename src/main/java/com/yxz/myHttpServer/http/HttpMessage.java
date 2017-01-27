package com.yxz.myHttpServer.http;
/**
* @author Yu 
* 2016年11月29日 下午9:28:15
* HTTP消息接口，作为请求和响应的公共接口
*/
public interface HttpMessage {
	
	public HttpVersion getHttpVersion();
	
	public HttpVersion setHttpVersion();
	
	public HttpHeaders getHttpHeaders();
	
	public void addHeader(HttpHeader Httpheader);
	
}
