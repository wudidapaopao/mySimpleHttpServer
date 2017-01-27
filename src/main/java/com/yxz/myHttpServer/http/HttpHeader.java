package com.yxz.myHttpServer.http;
/**
* @author Yu 
* 2016年11月29日 下午10:53:26
* http报文单个头部项包装类
*/
public class HttpHeader {
	
	public static final String CACHE_CONTROL = "Cache-Control";
	public static final String CONNECTION = "Connection";
	public static final String CONTENT_DISPOSITION = "Content-Disposition";
	public static final String CONTENT_LENGTH = "Content-Length";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String DATE = "Date";
	public static final String EXPECT = "Expect";
	public static final String SERVER = "Server";
	public static final String ORIGIN = "Origin";
	public static final String REFERER = "Referer";
	public static final String TRANSFER_ENCODING = "Transfer-Encoding";
	public static final String USER_AGENT = "User-Agent";
	public static final String AUTHORIZATION = "Authorization";
	public static final String COOKIE = "Cookie";
	public static final String SET_COOKIE = "Set-Cookie";
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	public static final String CONTENT_ENCODING = "Content-Encoding";
	
	private String name;
	private String value;
	
	public HttpHeader(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
}
