package com.ultra.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ultra.protocol.Message;

public class UltraServer extends Thread {

	public static void main(String[] args) {
		ServerSocketChannel sch = null;
		Selector sel = null;
		try {
			int port = 8400;
			InetSocketAddress addr = new InetSocketAddress(port);
			sch = ServerSocketChannel.open();

			sch.configureBlocking(false);
			sch.socket().bind(addr);

			sel = Selector.open();
			sch.register(sel, SelectionKey.OP_ACCEPT);
			System.out.println("Server socket is running " + port);

		} catch (IOException e) {
			System.out.println("Couldn't setup server socket");
			System.out.println(e.getMessage());
			System.exit(1);
		}

		UltraServer listener = new UltraServer(sel);
		listener.run();
	}

	private Selector sel = null;
	private List pool = new LinkedList();
	private List<IHandler> readHandlers = new ArrayList<IHandler>();
	private List<IHandler> writeHandlers = new ArrayList<IHandler>();

	public UltraServer(Selector sel) {
		this.sel = sel;
		int cpu = 4;// Runtime.getRuntime().availableProcessors();
		for (int i = 0; i < cpu; ++i) {
			IHandler rh = new ReaderHandler(this, new MessageSession());
			IHandler wh = new WriterHandler(this);
			this.readHandlers.add(rh);
			this.writeHandlers.add(wh);
			new Thread(rh, "readWorker" + i).start();
			new Thread(wh, "writeWorker" + i).start();
		}
	}

	private void accept(SelectionKey key) throws IOException {
		System.out.println("Accepting connection!");
		ServerSocketChannel sch = (ServerSocketChannel) key.channel();
		SocketChannel ch;

		ch = sch.accept();
		ch.configureBlocking(false);
		ch.register(this.sel, SelectionKey.OP_READ);
	}

	private void read(SelectionKey key) throws IOException {
		System.out.println("Read !");
		int worker = key.hashCode() % readHandlers.size();
		readHandlers.get(worker).process(key, null);
	}

	private void write(SelectionKey key) throws IOException {
		System.out.println("Write !");
		int worker = key.hashCode() % writeHandlers.size();
		synchronized (pendingSent) {
			if (pendingSent.containsKey(key.channel())) {
				List<Message> queue = pendingSent.get(key.channel());
				if (queue != null) {
					writeHandlers.get(worker).process(key, queue);
					pendingSent.remove(key.channel());
				}
			}
		}
		key.interestOps(SelectionKey.OP_READ);
	}

	private Map<SocketChannel, List<Message>> pendingSent = new HashMap<SocketChannel, List<Message>>();

	public void send(SelectionKey key, Message msg) {
		synchronized (pool) {
			pool.add(pool.size(), key);
			pool.notifyAll();
			synchronized (pendingSent) {
				SocketChannel socket = (SocketChannel) key.channel();
				List<Message> queue = pendingSent.get(socket);
				if (queue == null) {
					queue = new ArrayList<Message>();
					pendingSent.put(socket, queue);
				}
				queue.add(msg);
			}
		}
		this.sel.wakeup();
	}

	public void run() {
		while (true) {
			try {
				int cout = this.sel.select();

				synchronized (pool) {
					while (!pool.isEmpty()) {
						SelectionKey key = (SelectionKey) pool.remove(0);
						SocketChannel schannel = (SocketChannel) key.channel();
						try {
							schannel.register(this.sel, SelectionKey.OP_WRITE);
						} catch (Exception e) {
							try {
								schannel.finishConnect();
								schannel.close();
								schannel.socket().close();
							} catch (Exception e1) {
							}
						}
					}
					pool.clear();
				}

				if (cout > 0) {
					Iterator<?> i = this.sel.selectedKeys().iterator();
					while (i.hasNext()) {
						SelectionKey key = (SelectionKey) i.next();
						i.remove();
						try {
							if (!key.isValid()) {
								continue;
							}
							if (key.isAcceptable()) {
								accept(key);
							}
							if (key.isReadable()) {
								read(key);
							}

							if (key.isWritable()) {
								write(key);
							}
						} catch (Exception e) {
							System.out.println("Error in SelectionKey");
							key.cancel();
						}

					}
				}
			} catch (Exception e) {
				System.out.println("Error in poll loop");
				// System.out.println(e.getMessage());
			}
		}
	}

}
