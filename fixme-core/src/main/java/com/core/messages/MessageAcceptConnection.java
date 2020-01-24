package com.core.messages;

import com.core.MD5Creator;

public class MessageAcceptConnection extends FIXMessage {
	private int		id;

	public MessageAcceptConnection(String messageType, int marketId, int id) {
		super(messageType, marketId);
		this.id = id;
		setChecksum(MD5Creator.createMD5FromObject(String.valueOf(id)));
	}

	public MessageAcceptConnection() {}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setNewCheckSum() {
		setChecksum(MD5Creator.createMD5FromObject(String.valueOf(id)));
	}

	@Override
	public String toString() {
		return "MessageAcceptConnection {" +
				"ID = " + id +
				"|MSG_TYPE = '" + getMessageType() + "'" +
				"|CHECKSUM = '" + getChecksum() + "'" +
				'}';
	}
}
