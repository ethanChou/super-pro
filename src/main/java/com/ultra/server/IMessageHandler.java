package com.ultra.server;

import com.ultra.protocol.Message;

public interface IMessageHandler {

	Message process(Message msg);
}
