package com.ultra.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author ethan chou
 *
 */
public class Protocol {

	/**
	 * 默认大端解析
	 */
	public static boolean IsBigEndian = false;
	/**
	 * 包头 assic # 35
	 */
	private static final byte HEAD = 0x23;

	/**
	 * 版本 0x01
	 */
	private static final byte VERSION = 0x01;

	/**
	 * 
	 */
	private static final byte RESERVE0 = 0x00;

	/**
	 * 
	 */
	private static final byte RESERVE1 = 0x00;

	/**
	 * 6
	 */
	private static final int CONSTLENGHT = 8;

	/**
	 * 打包
	 * 
	 * @param msg
	 * @return
	 */
	public byte[] encode(Message msg) {
		int len = msg.getData().length + Message.LEN;
		ByteBuffer buffer = ByteBuffer.allocate(CONSTLENGHT + len);
		if (!IsBigEndian)
			buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(HEAD);
		buffer.put(VERSION);
		buffer.put(RESERVE0);
		buffer.put(RESERVE1);
		buffer.putInt(len);
		buffer.putInt(msg.getId());
		buffer.putInt(msg.getTimestamp());
		buffer.put(msg.getType());
		buffer.put(msg.getMarker());
		buffer.put(msg.getData());
		byte[] result = buffer.array();
		buffer = null;
		return result;
	}

	/**
	 * 解包
	 * 
	 * @param buf
	 * @return
	 */
	public List<Message> decode(ByteBuffer buf) {
		
		if (!IsBigEndian)
			buf.order(ByteOrder.LITTLE_ENDIAN);
		
		List<Message> list = new ArrayList<Message>();
		try {
			while (true) {
				if (buf.limit() - buf.position() < CONSTLENGHT) {
					return list;
				}
				byte header = buf.get();
				byte ver = buf.get();
				// 备用，占位
				byte r1 = buf.get();
				byte r2 = buf.get();
				if (!(header == HEAD && ver == VERSION)) {
					buf.position(buf.position() - 3);
					continue;
				}
				int offset = buf.getInt();
				if (offset <= buf.limit() - buf.position()) {
					// 解析包
					int id = buf.getInt();
					int timestamp = buf.getInt();
					byte type = buf.get();
					byte marker = buf.get();
					byte[] data = new byte[offset - Message.LEN];
					buf.get(data, 0, data.length);
					list.add(new Message(id, timestamp, type, marker, data));
					if (buf.limit() - buf.position() > 0) {
						continue;
					}
				} else {
					buf.position(buf.position() - CONSTLENGHT);
				}
				break;
			}
			return list;
		} finally {
			buf.compact();
		}
	}
}
