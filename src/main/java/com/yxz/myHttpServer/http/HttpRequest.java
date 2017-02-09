package com.yxz.myHttpServer.http;

/**
* @author Yu 
* http请求包装类
*/
public class HttpRequest {
	
	private HttpMethod httpMethod;
	private String url;
	private HttpVersion httpVersion;
	private HttpHeaders httpHeaders;
	private HttpParams httpParams;
	private String requestBody;
	
	public String getRequestBody() {
		return requestBody;
	}
	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}
	public HttpMethod getHttpMethod() {
		return httpMethod;
	}
	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public HttpVersion getHttpVersion() {
		return httpVersion;
	}
	public void setHttpVersion(HttpVersion httpVersion) {
		this.httpVersion = httpVersion;
	}
	public HttpHeaders getHttpHeaders() {
		return httpHeaders;
	}
	public void setHttpHeaders(HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
	}
	public HttpParams getHttpParams() {
		return httpParams;
	}
	public void setHttpParams(HttpParams httpParams) {
		this.httpParams = httpParams;
	}
	
	public void init() {
		this.httpHeaders = new HttpHeaders();
		this.httpMethod = null;
		this.httpParams = null;
		this.httpVersion = null;
		this.url = null;
		this.requestBody = null;
	}
	
	public int getContentLength() {
		String lenS = null;
		try {
			lenS = this.httpHeaders.getHeaderValue(HttpHeader.CONTENT_LENGTH);
		} catch(Exception e) {
			return -1;
		}
		if(lenS == null || lenS.length() == 0)
			return -1;
		int len = -1;
		try {
			len = Integer.parseInt(lenS);
		} catch(NumberFormatException e) {
			len = -1;
		}
		return len;
	}
	
	/*
	 * 处理收到的报文，封装httpRequest
	 */
	public void process() {
		String tmp = null;
		if(url.contains("?")) {
			tmp = url.substring(url.lastIndexOf("?") + 1, url.length());
			url = url.substring(1, url.lastIndexOf('?'));
		}
		httpParams = new HttpParams();
		setUrlParams(tmp);
		if(httpMethod == HttpMethod.POST && this.requestBody != null) {
			setBodyParams(requestBody);
		}
		//to do cookie
	}
	
	private void setBodyParams(String body) {
		setParams(body);
	}
	
	private void setUrlParams(String urlParam) {
		setParams(urlParam);
	}
	
	private void setParams(String str) {
		if(str != null) {
			String[] params = str.split("&");
			for(String param : params) {
				String[] kv = param.split("=");
    			httpParams.add(kv[0], kv[1]);
			}
		}
	}
	
}
