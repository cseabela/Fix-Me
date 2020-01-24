package com.core.encoders;

import com.core.messages.MessageAcceptConnection;
import com.core.messages.MessageTypes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.Charset;

public class AcceptConnectionEncoder extends MessageToByteEncoder<MessageAcceptConnection> {
	private final Charset charset=Charset.forName("UTF-8");

	@Override
	protected void encode(ChannelHandlerContext ctx, MessageAcceptConnection msg, ByteBuf out) throws Exception {
		out.writeInt(msg.getTypeLength());
		out.writeCharSequence(msg.getMessageType(), charset);
		if (msg.getMessageType().equals(MessageTypes.MESSAGE_ACCEPT_CONNECTION.toString())) {
			out.writeInt(msg.getId());
			out.writeInt(msg.getChecksumLength());
			out.writeCharSequence(msg.getChecksum(), charset);
		}
	}
}