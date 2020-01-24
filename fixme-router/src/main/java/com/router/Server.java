package com.router;

import com.core.decoders.Decoder;
import com.core.encoders.AcceptConnectionEncoder;
import com.core.encoders.SellOrBuyEncoder;
import com.core.exceptions.ChecksumIsNotEqual;
import com.core.exceptions.ClientNotInRoutingTable;
import com.core.messages.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.HashMap;

public class Server implements Runnable {
	private static HashMap<Integer, ChannelHandlerContext>	routingTable = new HashMap<>();
	static final int										MARKET_SERVER = 5001;
	static final int										BROKER_SERVER = 5000;
	private EventLoopGroup									bossGroup;
	private EventLoopGroup									workerGroup;
	private int												serverType;

	Server(int serverType) {
		this.serverType = serverType;
	}

	@Override
	public void run() {
		createServer(serverType);
	}

	private String brokerOrMarketString() {
		return serverType == MARKET_SERVER ? "market" : "broker";
	}

	private boolean brokerOrMarketBool() {
		return serverType != MARKET_SERVER;
	}

	private void createServer(int port) {
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast(
									new Decoder(),
									new AcceptConnectionEncoder(),
									new SellOrBuyEncoder(),
									new ProcessingHandler());
						}
					}).option(ChannelOption.SO_BACKLOG, 128)
					.childOption(ChannelOption.SO_KEEPALIVE, true);
			ChannelFuture f = b.bind(port).sync();
			f.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			shutDown();
		}
	}

	void shutDown() {
		workerGroup.shutdownGracefully();
		bossGroup.shutdownGracefully();
	}

	class ProcessingHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			FIXMessage message = (FIXMessage)msg;
			if (message.getMessageType().equals(MessageTypes.MESSAGE_ACCEPT_CONNECTION.toString()))
				acceptNewConnection(ctx, msg);
			else if (	message.getMessageType().equals(MessageTypes.MESSAGE_BUY.toString()) ||
						message.getMessageType().equals(MessageTypes.MESSAGE_SELL.toString())) {
				MessageSellOrBuy ret = (MessageSellOrBuy)msg;
				try {
					checkForErrors(ret);
					if (checkIfMessageIsRejectedOrExecuted(ret))
						return;
					System.out.println("Sending request to market with ID " + ret.getMarketId());
					getFromTableById(ret.getMarketId()).channel().writeAndFlush(ret);
				} catch (Exception e) {
					System.out.println(e.getMessage());
					ret.setMessageAction(MessageTypes.MESSAGE_REJECT.toString());
					ret.setNewCheckSum();
					ctx.writeAndFlush(ret);
				}
			}
		}
	}

	private void acceptNewConnection(ChannelHandlerContext ctx, Object msg) {
		MessageAcceptConnection ret  = (MessageAcceptConnection)msg;
		String newID = ctx.channel().remoteAddress().toString().substring(11);
		newID = newID.concat(brokerOrMarketBool() ? "2" : "3");
		ret.setId(Integer.valueOf(newID));
		ret.setNewCheckSum();
		ctx.writeAndFlush(ret);
		routingTable.put(ret.getId(), ctx);
		System.out.println("Accepted a connection from " + brokerOrMarketString() + ": " + newID);
	}

	private void checkForErrors(MessageSellOrBuy ret) throws Exception {
		if (!ret.getMsgMD5().equals(ret.getChecksum()))
			throw new ChecksumIsNotEqual();
		if (!checkIfInTable(ret.getMarketId()))
			throw new ClientNotInRoutingTable();
	}

	private boolean checkIfMessageIsRejectedOrExecuted(MessageSellOrBuy ret) throws Exception {
		if (ret.getMessageAction().equals(MessageTypes.MESSAGE_EXECUTE.toString()) ||
			ret.getMessageAction().equals(MessageTypes.MESSAGE_REJECT.toString())) {
			if (!ret.getMsgMD5().equals(ret.getChecksum()))
				throw new ChecksumIsNotEqual();
			getFromTableById(ret.getId()).writeAndFlush(ret);
			return true;
		}
		return false;
	}

	private boolean checkIfInTable(int id) {
		return routingTable.containsKey(id);
	}

	private ChannelHandlerContext getFromTableById(int id) {
		return routingTable.get(id);
	}
}
