package com.ultra.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.ultra.protocol.Message;
import com.ultra.protocol.Protocol;

public class Server {

	public static class ListenerThread extends Thread {
		Selector sel = null;
		Protocol protocol = new Protocol();

		public ListenerThread(Selector sel) {
			this.sel = sel;
		}

		public ListenerThread() {

		}

		private void accept(SelectionKey key) {
			System.out.println("Accepting connection!");
			ServerSocketChannel sch = (ServerSocketChannel) key.channel();
			SocketChannel ch;
			try {
				ch = sch.accept();
				ch.configureBlocking(false);
				ch.register(this.sel, SelectionKey.OP_READ);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void read(SelectionKey key) throws IOException {

			System.out.println("Accepting command!");
			SocketChannel ch = (SocketChannel) key.channel();
			ByteBuffer buf = ByteBuffer.allocate(1024 * 1024);
			while (true) {
				int r = ch.read(buf);
				if (r == -1) {
					break;
				}
				buf.flip();
				buf.order(ByteOrder.LITTLE_ENDIAN);
				List<Message> messages = protocol.decode(buf);
				for (Message message : messages) {
					if (message.getId() != lastIndex + 1) {
						System.out.println("=============================");
						System.out.println(String.format("%s,%s", message.getId(), lastIndex));
						System.out.println("=============================");
					}
					if (message.getId() == 1 || message.getId() == 1000000) {
						System.out.println(String.format("Time:%s,%s,%s", System.currentTimeMillis(), message.getId(),
								new String(message.getData(), "utf-8")));
					}
					lastIndex = message.getId();
				}
				buf.compact();
			}
			ch.register(this.sel, SelectionKey.OP_WRITE);
		}

		private void write(SelectionKey key) throws IOException {
			ByteBuffer resp = ByteBuffer.wrap(new String("got it\n").getBytes());
			System.out.println("Sending response!");
			SocketChannel ch = (SocketChannel) key.channel();
			ch.write(resp);
			resp.rewind();
			ch.register(this.sel, SelectionKey.OP_READ);
		}

		public void run() {
			while (true) {
				try {
					while (this.sel.select() > 0) {
						Set keys = this.sel.selectedKeys();
						Iterator i = keys.iterator();
						while (i.hasNext()) {
							SelectionKey key = (SelectionKey) i.next();
							i.remove();
							if (key.isAcceptable()) {
								accept(key);
							}
							if (key.isReadable()) {
								read(key);
							}
							if (key.isWritable()) {
								write(key);
							}
						}
					}
				} catch (IOException e) {
					System.out.println("Error in poll loop");
					System.out.println(e.getMessage());
					System.exit(1);
				}
			}
		}
	}

	static int lastIndex = 0;

	public static void main(String[] args) {
		System.out.println("Hello World!");

		ServerSocketChannel sch = null;
		Selector sel = null;

		try {
			// setup the socket we're listening for connections on.
			int port = 8400;
			InetSocketAddress addr = new InetSocketAddress(port);
			sch = ServerSocketChannel.open();

			sch.configureBlocking(false);
			sch.socket().bind(addr);
			// setup our selector and register the main socket on it
			sel = Selector.open();
			sch.register(sel, SelectionKey.OP_ACCEPT);
			System.out.println("Server socket is running " + port);

		} catch (IOException e) {
			System.out.println("Couldn't setup server socket");
			System.out.println(e.getMessage());
			System.exit(1);
		}

		// fire up the listener thread, pass it our selector
		Server.ListenerThread listener = new ListenerThread(sel);
		listener.run();
	}

	static void test() {
		Message message = new Message(2, 1, (byte) 0, (byte) 0, new byte[] { 1, 1, 1, 1, 2, 2, 2, 2 });
		Protocol protocol = new Protocol();
		byte[] buf = protocol.encode(message);
		List<Message> m = protocol.decode(ByteBuffer.wrap(buf));
		protocol.IsBigEndian = false;
		byte[] buf1 = protocol.encode(message);
		List<Message> m1 = protocol.decode(ByteBuffer.wrap(buf1));
		if (buf == null) {

		}
	}
}
