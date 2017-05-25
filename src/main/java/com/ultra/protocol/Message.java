package com.ultra.protocol;

/**
 * 
 * @author ethan chou
 *
 */
public class Message {

	/**
	 * LEN 10
	 */
	public static int LEN=10;
		
	/**
	 * 消息id
	 */
	private int id;

	/**
	 * 消息时间戳
	 */
	private int timestamp;

	/**
	 * 消息类型 
	 */
	private byte type;
	
	/**
	 * 标记，一条数据分成多个Message，0 or 1
	 */
	private byte marker;
	
	
	/**
	 * 数据
	 */
	private byte[] data;

	public Message(int id,int tstamp,byte type,byte mk, byte[] data) {
		this.id = id;
		this.timestamp=tstamp;
		this.type=type;
		this.marker=mk;
		this.data = data;
	}
	
	public byte getType() {
		return type;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
	
	public byte getMarker() {
		return marker;
	}

	public void setMarker(byte marker) {
		this.marker = marker;
	}
}
