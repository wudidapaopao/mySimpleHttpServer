package com.yxz.myHttpServer.http;

import java.util.ArrayList;
import java.util.List;

/**
* @author Yu 
* 2016年11月29日 下午10:53:20
* http报文头部项包装类
*/
public class HttpHeaders {
	
	private List<HttpHeader> lists = new ArrayList<>();
	
	public void addHeader(HttpHeader httpHeader) {
		lists.add(httpHeader);
	}
	
	public String getHeaderValue(String name) {
		for(int i = 0; i < lists.size(); i++) {
			HttpHeader header = lists.get(i);
			if(header.getName().trim().equalsIgnoreCase(name))
				return header.getValue();
		}
		throw new IllegalArgumentException();
	}

	public List<HttpHeader> getLists() {
		return lists;
	}

	public void setLists(List<HttpHeader> lists) {
		this.lists = lists;
	}
	
}
