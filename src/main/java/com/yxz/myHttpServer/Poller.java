package com.yxz.myHttpServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author Yu 
* Reactor模式中负责IO的线程
*/
public class Poller implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(Poller.class);
	
	protected CountDownLatch countDownLatch = new CountDownLatch(1);//协调poller全部开始
	protected CountDownLatch countDownLatchClose = new CountDownLatch(1);//协调poller全部关闭
	
	private int keepLiveTimeOut = 20000;
	private int internalTime = 1000;
	private long nextTime = 0;
	private long selectTimeOut = 10000;
	
	//直接内存，创建开销更大，但IO省去一次复制，更快。
	private final ByteBuffer bb = ByteBuffer.allocateDirect(1024);
	
	private final Selector selector;
	private Executor executor;
	private volatile boolean running;
	
	public Poller(Executor executor) throws IOException {
		this.executor = executor;
		this.selector = Selector.open();
		running = true;
	}
	
	//需要进行读写的任务队列
	private ConcurrentLinkedQueue<WrappedChannel> queue = new ConcurrentLinkedQueue<>();
	
	/*
	 * 向poller线程注册新的读写事件
	 */
	public void register(WrappedChannel channel) {
		queue.add(channel);
		channel.setSelector(selector);
		channel.setPoller(this);
		this.wakeup();
	}
	
	private AtomicBoolean wakenUp = new AtomicBoolean(false);
	
	/*
	 * 唤醒selector
	 */
	protected void wakeup() {
		if(wakenUp.compareAndSet(false, true)) { //避免多次调用select.wakeUp，wakeUp是向通道进行写操作，费力。
			selector.wakeup();
		}
	}
	
	@Override
	public void run() {
		countDownLatch.countDown();
		while(true) {	
			this.wakenUp.set(false);//#1
			try {
    			selector.select(selectTimeOut);//#2
    			processQueue();//从队列中取出读写事件，并在selector中注册
    			if(wakenUp.get()) {
    				selector.wakeup();//避免#1和#2之间wakeup被调用，这样容易导致下下次select被阻塞很久
    			}
    			if(running) {
        		    Set<SelectionKey> keys = selector.selectedKeys();
        		    if(keys != null && !keys.isEmpty()) {
            		    Iterator<SelectionKey> iterator = keys.iterator();
            		    while(iterator.hasNext()) {
            		    	SelectionKey key = iterator.next();
            		    	iterator.remove();
            		    	int readyOps = key.readyOps();
            		    	WrappedChannel attach = (WrappedChannel) key.attachment();
            		    	attach.setTime(System.currentTimeMillis());
            		    	if((readyOps & SelectionKey.OP_READ) != 0) {
            		    		int interestOp = readyOps & (~SelectionKey.OP_READ);
            		    		key.interestOps(interestOp);
                		    	attach.setInterest(interestOp);
                		    	if(!read(key)) {
            		    			continue; //connection is closed
            		    		}
            		    	}
            		    	if((readyOps & SelectionKey.OP_WRITE) != 0) {
            		    		int interestOp = readyOps & (~SelectionKey.OP_WRITE);
            		    		key.interestOps(interestOp);
                		    	attach.setInterest(interestOp);
                		    	write(key);
            		    	}
            		    }
        		    }
        		    //timeout();
    			}
    			else {
    				for (SelectionKey k: selector.keys()) {
                        k.cancel();
                    }
                    try {
                        selector.close();
                    } catch (IOException e) {
                        //ignore
                    }
                    countDownLatchClose.countDown();
    				break;
    			}
			} catch (IOException e) {
				logger.error("failed in the poller loop", e);
			}
		}
	}

	public void shutDownPoller() {
		this.running = false;
		if(selector != null) {
			selector.wakeup();
		}
	}
	
	private void processQueue() {
		WrappedChannel channel = null;
		while((channel = queue.poll()) != null) {
			try {
				SocketChannel socketChannel = channel.getSocketChannel();
				socketChannel.configureBlocking(false);
				socketChannel.register(selector, channel.getInterest(), channel);
			} catch (IOException e) {
				//无视这个socketChannel
			}
		}
	}
	
	//keepalive的超时判定
	private void timeout() {
		long now = System.currentTimeMillis();
		if(now < nextTime) //避免频繁进行timeout判定
			return;
		Set<SelectionKey> keys = selector.keys();
		for(SelectionKey key : keys) {
			WrappedChannel attach = (WrappedChannel) key.attachment();
			if(key.interestOps() != 0) {
				if(attach.getTime() + keepLiveTimeOut < System.currentTimeMillis()) {
					key.interestOps(0);
					attach.setInterest(0);
					close(key);
				}
			}
		}
		nextTime += internalTime;
	}

	//write操作
	private void write(SelectionKey key) {
		if(key == null || !key.isValid())
			return;
		SocketChannel channel = (SocketChannel) key.channel();
		WrappedChannel wc = (WrappedChannel) key.attachment();
		Queue<SendBuffer> qs = wc.getSendBuffers();
		SendBuffer sb = qs.peek();
		try {
			while(sb != null) {
        		long length = sb.writeToChannel(channel);
        		if(length == -1) {
        			close(channel.keyFor(selector));
        			break;
        		}
        		else if(length == 0) {
        			break;
        		}
        		if(sb.isFinished()) {
        			qs.poll();
        		}
        		else {
        			break;
        		}
        		sb.close();
        		sb = qs.peek();
			}
    		if(!qs.isEmpty() || !wc.isFinished()) {//write未完成，注册write事件
    			if(setOp(wc, SelectionKey.OP_WRITE));
    				this.wakeup();
    		}
    		else {//write完成，注册read事件
    			wc.setFinished(false);
    			if(setOp(wc, SelectionKey.OP_READ));
    				this.wakeup();
    		}
		} catch(Exception e) {
			logger.error("", e);
			close(key);
		}
	}

	//向通道注册感兴趣的读写操作
	private boolean setOp(WrappedChannel wc, int op) {
		SocketChannel channel = wc.getSocketChannel();
		Selector selector = this.selector;
		SelectionKey key = channel.keyFor(selector);
		if(key == null || !key.isValid())
			return false;
		int interestOp = wc.getInterest();
		if((interestOp & op) == 0) {
			interestOp = interestOp | op;
			key.interestOps(interestOp);
			wc.setInterest(interestOp);
		}
		return true;
	}

	//读操作
	private boolean read(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();
		bb.clear();
		int readCount = 0;
		int readOnce = 0;
		boolean flag = true;
		try {
			while((readOnce = channel.read(bb)) > 0) {
				readCount += readOnce;
				if(!bb.hasRemaining())
					break;
			}
			flag = false;
		} catch (IOException e) {
			//ignore
		}
		if(readCount > 0) {
			bb.flip();
			WrappedChannel wrappedChannel = ((WrappedChannel) key.attachment());
			RequestHandler handler = wrappedChannel.getHandler();
			handler.add(bb);
			this.executor.execute(handler);
		}
		if(readOnce == -1 || flag) {
			close(key);
			return false;
		}
		return true;
	}
	
	//关闭在selector中注册的通道
	private void close(SelectionKey key) {
		key.cancel();
		try {
			key.channel().close();
		} catch (IOException e) {
			
		}
	}
	
}
