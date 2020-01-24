package com.core.encoders;

import com.core.messages.MessageSellOrBuy;
import com.core.messages.MessageTypes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.Charset;

public class SellOrBuyEncoder extends MessageToByteEncoder<MessageSellOrBuy> {
	private final Charset charset=Charset.forName("UTF-8");

	@Override
	protected void encode(ChannelHandlerContext ctx, MessageSellOrBuy msg, ByteBuf out) throws Exception {
		out.writeInt(msg.getTypeLength());
		out.writeCharSequence(msg.getMessageType(), charset);
		if (msg.getMessageType().equals(MessageTypes.MESSAGE_BUY.toString()) ||
			msg.getMessageType().equals(MessageTypes.MESSAGE_SELL.toString())) {
			out.writeInt(msg.getActionLength());
			out.writeCharSequence(msg.getMessageAction(), charset);
			out.writeInt(msg.getId());
			out.writeInt(msg.getInstrumentLength());
			out.writeCharSequence(msg.getInstrument(), charset);
			out.writeInt(msg.getMarketId());
			out.writeInt(msg.getPrice());
			out.writeInt(msg.getQuantity());
			out.writeInt(msg.getChecksumLength());
			out.writeCharSequence(msg.getChecksum(), charset);
		}
	}
}

