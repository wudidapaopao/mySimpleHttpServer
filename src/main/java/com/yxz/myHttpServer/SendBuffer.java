package com.yxz.myHttpServer;

import java.nio.channels.SocketChannel;

/**
* @author Yu 
* 2016年12月15日 下午4:22:08
* 发送缓冲类的公共接口
*/
public interface SendBuffer {
	
	boolean isFinished();
	
	long writeToChannel(SocketChannel socketChannel) throws Exception;
	
	void close();
	
}
