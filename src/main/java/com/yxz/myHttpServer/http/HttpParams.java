package com.yxz.myHttpServer.http;

import java.util.*;

/**
* @author Yu 
* 2016年11月30日 下午4:45:25
* http请求参数包装类
*/
public class HttpParams {
	
	private Map<String, String> params = new HashMap<>();
	
	public HttpParams() {
		
	}

	public void add(String key, String value) {
		if(key != null && value != null) {
			String key2 = key.trim();
			if(key2.length() > 0)
			params.put(key2, value.trim());
		}
	}
	
	public String get(String key) {
		if(key == null) {
			return null;
		}
		key = key.trim();
		if(!this.params.containsKey(key)) {
			return null;
		}
		return this.params.get(key);
	}
	
}
