package com.yxz.myHttpServer.http;
/**
* @author Yu 
* 2016年11月29日 下午9:41:02
* http版本：1.0和1.1
*/
public class HttpVersion {
	
	public static final String HTTP10S = "HTTP/1.0";
	public static final String HTTP11S = "HTTP/1.1";
	
	public static final HttpVersion HTTP10 = new HttpVersion("HTTP/1.0");
	public static final HttpVersion HTTP11 = new HttpVersion("HTTP/1.1");
	
	private final String protocolName;
	
	private HttpVersion(String protocolName) {
		this.protocolName = protocolName;
	}
	
	public static HttpVersion getHttpVersion(String protocolName) {
		if(protocolName == null || protocolName.trim().equals(""))
			throw new IllegalArgumentException("empty protocol name");
		protocolName = protocolName.trim();
		HttpVersion httpVersion = null;
		if(protocolName.equals(HTTP10S))
			return HTTP10;
		else if(protocolName.equals(HTTP11S))
			return HTTP11;
		else
			throw new IllegalArgumentException("wrong protocol name");
	}
	
	public String getProtocolName() {
		return protocolName;
	}
}
