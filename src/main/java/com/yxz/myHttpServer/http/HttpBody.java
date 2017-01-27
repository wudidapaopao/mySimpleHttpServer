package com.yxz.myHttpServer.http;

import java.io.File;

/**
* @author Yu 
* 2016年12月13日 下午4:21:49
* 响应实体内容,暂时只支持文本文件
*/
public class HttpBody {
	
	private File file;

	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	
}
