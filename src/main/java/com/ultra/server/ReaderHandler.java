package com.ultra.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import com.ultra.protocol.Message;
import com.ultra.protocol.Protocol;

public class ReaderHandler implements IHandler {

	private Protocol protocol = new Protocol();
	private List pool = new LinkedList();
	private UltraServer server;
	private IMessageHandler msgHandler;
	private static int BUFFER_SIZE = 1024*10;

	public ReaderHandler(UltraServer svr,IMessageHandler msghd) {
		this.msgHandler=msghd;
		this.server = svr;
	}

	public void run() {
		while (true) {
			try {
				SelectionKey key;
				synchronized (pool) {
					while (pool.isEmpty()) {
						pool.wait();
					}
					key = (SelectionKey) pool.remove(0);
				}
				read(key);
			} catch (Exception e) {
				continue;
			}
		}
	}

	public static byte[] grow(byte[] src, int size) {
		byte[] tmp = new byte[src.length + size];
		System.arraycopy(src, 0, tmp, 0, src.length);
		return tmp;
	}

	private void read(SelectionKey key) {
		//System.out.println("read");
		SocketChannel ch = (SocketChannel) key.channel();
		
		ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
		while (true) {
			int r=-1;
			try {
				r = ch.read(buf);
			} catch (IOException e) {
				key.cancel();
				try {
					ch.close();
					ch.finishConnect();
					ch.close();
					ch.socket().close();
				} catch (Exception e2) {
					
				}
				
				//System.out.println("Disconnect "+e.getMessage());
				return;
			}
			if (r== -1) {
				System.out.println("Read Empty");
				key.cancel();
				break;
			}
			buf.flip();
			if (buf.limit()==0) {
				//System.out.println("Read Empty  000");
				break;
			}
			List<Message> messages = protocol.decode(buf);
			for (Message message : messages) {
				if(msgHandler!=null)
				{
					System.out.println("Msgid:"+message.getId());
					
					Message result= msgHandler.process(message);
					
					server.send(key,result);
				}
			}
		}
	}

	public void process(SelectionKey key,List<Message> args) {
		synchronized (pool) {
			pool.add(pool.size(), key);
			pool.notifyAll();
		}
	}
}
