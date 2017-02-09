package com.yxz.myHttpServer.http;

import java.util.*;

/**
* @author Yu 
* http方法
*/
public class HttpMethod {

	private String methodName;
	
	/*
	 * 获取资源，请求参数在请求url中
	 */
	public static final HttpMethod GET = new HttpMethod("GET");
	
	/*
	 * 传输请求实体，获取处理结果
	 */
	public static final HttpMethod POST = new HttpMethod("POST");
	
	/*
	 * 客户端传输文件给服务器
	 */
	public static final HttpMethod PUT = new HttpMethod("PUT");
	
	/*
	 * 和get一样，不过只返回响应首部，不返回实体主体，用于确认uri的有效性以及资源更新日期等
	 */
	public static final HttpMethod HEAD = new HttpMethod("HEAD");
	
	/*
	 * 和put相反，删除服务器指定文件
	 */
	public static final HttpMethod DELETE = new HttpMethod("DELETE");
	
	/*
	 * 查询针对请求uri指定资源支持的方法
	 */
	public static final HttpMethod OPTIONS = new HttpMethod("OPTIONS");
	
	/*
	 * 追踪路径，查询发出的请求怎么样被代理服务器修改的，不常用
	 */
	public static final HttpMethod TRACE = new HttpMethod("TRACE");
	
	/*
	 * 要求用隧道协议连接代理，与代理服务器通信时建立隧道，主要使用ssl，tls把通信内容加密后经网络隧道传输
	 */
	public static final HttpMethod CONNECT = new HttpMethod("CONNECT");
	
	private static final Map<String, HttpMethod> map = new HashMap<>();
	
	static {
		map.put(GET.methodName, GET);
		map.put(POST.methodName, POST);
		map.put(HEAD.methodName, HEAD);
		map.put(PUT.methodName, PUT);
		map.put(DELETE.methodName, DELETE);
		map.put(OPTIONS.methodName, OPTIONS);
		map.put(TRACE.methodName, TRACE);
		map.put(CONNECT.methodName, CONNECT);
	}
	
	
	private HttpMethod(String methodName) {
		this.methodName = methodName;
	}
	
	public static HttpMethod getMethod(String methodName) {
		if(methodName == null || methodName.trim().equals(""))
			throw new IllegalArgumentException("empty method name");
		methodName = methodName.trim();
		HttpMethod httpMethod = map.get(methodName);
		if(httpMethod == null)
			throw new IllegalArgumentException("wrong method name");
		return httpMethod;
	}
	
}
