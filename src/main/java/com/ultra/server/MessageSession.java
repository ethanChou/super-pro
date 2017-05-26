package com.ultra.server;

import java.util.Date;

import com.ultra.protocol.Message;

public class MessageSession implements IMessageHandler {

	int id=1;
	public Message process(Message msg) {
		
		String say="success";
		Message message=new Message(id++,(int) new Date().getTime(), (byte)100,(byte) 1, say.getBytes());
		return message;
	}

}
