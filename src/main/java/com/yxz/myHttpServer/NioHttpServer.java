package com.yxz.myHttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yxz.myHttpServer.utils.SimpleThreadPool;

/**
* @author Yu 
* httpServer启动类
*/
public class NioHttpServer {

	private static final Logger logger = LoggerFactory.getLogger(NioHttpServer.class);
	
	//静态文件所在路径
	public static final String pathPrefix = System.getProperty("user.dir") 
											+ File.separator  
											+ "webroot";
	
	//tcp超时时间
	private int timeout = 20000;
	
	//服务器端口号
	private int port = 80;
	
	//服务端接收请求的线程数
	private int nAcceptors = 1;
	
	//进行读写的线程数
	private int nPollers = Runtime.getRuntime().availableProcessors(); 
	
	//进行网络读写的线程数组
	private Poller[] pollers = new Poller[nPollers];
	
	//进行业务数据处理的线程数
	private int nProcessors = 10; 
	
	//进行数据处理的线程池
	//private ExecutorService executor = Executors.newFixedThreadPool(nProcessors); 
	private Executor executor =  new SimpleThreadPool(new LinkedList<Thread>(), new ArrayBlockingQueue<Runnable>(5), 10, 5, 2000, true);

	public static void main(String[] args) {
		NioHttpServer httpServer = new NioHttpServer();
		httpServer.start();
	}
	
	public void start() {
		ServerSocketChannel serverSocketChannel = null;
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.bind(new InetSocketAddress(port));
			serverSocketChannel.configureBlocking(true);
		} catch (IOException e) {
			logger.error("failed to start", e);
			return;
		}
		for(int i = 0; i < nPollers; i++) {
			try {
				pollers[i] = new Poller(executor);
			} catch (IOException e) {
				logger.error("failed to creat Poller " + i, e);
				return;
			}
			new Thread(pollers[i]).start();
		}
		if(!waitForPollers(10)) {//等待所有poller线程都启动，最多等10秒
			logger.error("failed to start");
			return;
		}
		for(int i = 0; i < nAcceptors; i++) {
			new Thread(new Acceptor(serverSocketChannel, nPollers, pollers)).start();
		}
	}

	private boolean waitForPollers(long time) {
		long deadTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(time);
		for(Poller poller : pollers) {
			CountDownLatch countDownLatch = poller.countDownLatch;
			try {
				long leftTime = deadTime - System.currentTimeMillis();
				if(leftTime < 0) {
					return false;
				}
				if(!countDownLatch.await(leftTime, TimeUnit.MILLISECONDS)) {
					return false;
				}
			} catch (InterruptedException e) {
				return false;
			}
		}
		return true;
	}
	
	private boolean shutDownAllPollers(long time) {
		long deadTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(time);
		for(Poller poller : pollers) {
			CountDownLatch countDownLatchClose = poller.countDownLatchClose;
			try {
				long leftTime = deadTime - System.currentTimeMillis();
				if(leftTime < 0) {
					return false;
				}
				if(!countDownLatchClose.await(leftTime, TimeUnit.MILLISECONDS)) {
					return false;
				}
			} catch (InterruptedException e) {
				return false;
			}
		}
		return true;
	}
	
}
