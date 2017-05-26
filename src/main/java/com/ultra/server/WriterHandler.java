package com.ultra.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ultra.protocol.Message;
import com.ultra.protocol.Protocol;

public class WriterHandler implements IHandler {

	public static class WaitData {
		public SelectionKey key;
		public List<Message> data;

		public WaitData(SelectionKey k, List<Message> dt) {
			this.key = k;
			this.data = dt;
		}
	}

	private Protocol protocol = new Protocol();
	private List<WaitData> pool = new ArrayList();
	private UltraServer server;

	public WriterHandler(UltraServer svr) {
		this.server = svr;
	}

	public void run() {
		while (true) {
			try {
				WaitData data;
				synchronized (pool) {
					while (pool.isEmpty()) {
						// key.interestOps(SelectionKey.OP_READ);
						pool.wait();
					}
					data = (WaitData) pool.remove(0);
				}
				write(data);
			} catch (Exception e) {
				continue;
			}
		}
	}

	private void write(WaitData data) {
		if(data.data==null)
			return;
		
		SocketChannel ch = (SocketChannel) data.key.channel();

		for (int i = 0; i < data.data.size(); i++) {
			byte[] buf = protocol.encode(data.data.get(i));
			try {
				ByteBuffer buffer=ByteBuffer.wrap(buf);
				while (buffer.hasRemaining()) {
					ch.write(buffer);
				}
				
			} catch (IOException e) {
				data.key.cancel();
				System.out.println("write error ");
			}
		}
	}

	public void process(SelectionKey key, List<Message>  args) {
		synchronized (pool) {
			pool.add(new WaitData(key, args));
			pool.notify();
		}
	}
}
