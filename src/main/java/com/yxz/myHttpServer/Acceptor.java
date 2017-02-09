package com.yxz.myHttpServer;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
* @author Yu 
* Reactor模式负责前端接受连接的线程
*/
public class Acceptor implements Runnable {

	private volatile boolean running;
	
	private ServerSocketChannel serverSocketChannel;
	private int nPollers;
	private Poller[] pollers;
	
	public Acceptor(ServerSocketChannel serverSocketChannel, int nPollers, Poller[] pollers) {
		this.serverSocketChannel = serverSocketChannel;
		this.nPollers = nPollers;
		this.pollers = pollers;
		running = true;
	}
	
	public void run() { //阻塞地接受连接请求
		while(true) {
			try {
				SocketChannel socketChannel = serverSocketChannel.accept();
				if(socketChannel != null) {
					WrappedChannel channel = new WrappedChannel(socketChannel);
					channel.setInterest(SelectionKey.OP_READ);
					RequestHandler handler = new RequestHandler();
					channel.setHandler(handler);
					handler.setWrappedChannel(channel);
					getRandomPoller().register(channel);
				}
			} catch (IOException e) {
				try {
					Thread.sleep(1000);	//避免因为异常而导致cpu过度占用
				} catch (InterruptedException e1) {

				}
			}
			if(!running) {
				try {
					serverSocketChannel.close();
				} catch (IOException e) {
					//failed to close
				}
			}
		}
	}
	
	//关闭acceptor线程
	public void shutDownAcceptor() {
		this.running = false;
	}
	
	private AtomicInteger count = new AtomicInteger(0);
	
	//平均地把接受的连接分发给poller线程
	private Poller getRandomPoller() {
		if((nPollers & (nPollers - 1)) == 0) //如果是poller数量是2的幂，用&的方法代替求余，效率更高
			return pollers[(nPollers - 1) & count.getAndIncrement()];
		return pollers[Math.abs(count.getAndIncrement()) % nPollers];
	}

}
