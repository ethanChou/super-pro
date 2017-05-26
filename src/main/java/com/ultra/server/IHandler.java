package com.ultra.server;

import java.nio.channels.SelectionKey;
import java.util.List;

import com.ultra.protocol.Message;

public interface IHandler extends Runnable {
	void process(SelectionKey key,List<Message> args);
}
