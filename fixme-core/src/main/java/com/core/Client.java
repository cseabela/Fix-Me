package com.core;

import com.core.decoders.Decoder;
import com.core.encoders.AcceptConnectionEncoder;
import com.core.encoders.SellOrBuyEncoder;
import com.core.exceptions.ChecksumIsNotEqual;
import com.core.exceptions.EmptyInput;
import com.core.exceptions.ErrorInput;
import com.core.messages.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

public class Client implements Runnable {
	private String			clientName;
	private EventLoopGroup	workerGroup;
	private int				uniqueID;

	public Client(String clientName) {
		this.clientName = clientName;
	}

	public static void handleInput(Client client) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String command;
		while (true) {
			command = null;
			try {
				command = br.readLine();
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
			if (command != null && command.toLowerCase().equals("exit")) {
				client.shutDown();
				break;
			}
		}
	}

	@Override
	public void run() {
		String host = "localhost";
		int port = 5000;
		if (clientName.equals("Market"))
			port = 5001;
		workerGroup = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(workerGroup)
			.channel(NioSocketChannel.class)
			.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(
							new Decoder(),
							new AcceptConnectionEncoder(),
							new SellOrBuyEncoder(),
							new ClientHandler());
				}
			}).option(ChannelOption.SO_KEEPALIVE, true);
			ChannelFuture f = b.connect(host, port).sync();
			f.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			shutDown();
		}
	}

	private void shutDown() {
		workerGroup.shutdownGracefully();
	}

	class ClientHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			System.out.println(clientName + " is connecting to router..");
			MessageAcceptConnection msg = new MessageAcceptConnection(MessageTypes.MESSAGE_ACCEPT_CONNECTION.toString(), 0, 0);
			ctx.writeAndFlush(msg);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			FIXMessage message = (FIXMessage)msg;
			if (message.getMessageType().equals(MessageTypes.MESSAGE_ACCEPT_CONNECTION.toString())) {
				MessageAcceptConnection ret = (MessageAcceptConnection)msg;
				uniqueID = ret.getId();
				System.out.println("Connection with router established. ID: " + uniqueID);
			} else if (	message.getMessageType().equals(MessageTypes.MESSAGE_BUY.toString()) ||
						message.getMessageType().equals(MessageTypes.MESSAGE_SELL.toString())) {
				MessageSellOrBuy ret = (MessageSellOrBuy)msg;
				try {
					if (!ret.getMsgMD5().equals(ret.getChecksum()))
						throw new ChecksumIsNotEqual();
				} catch (Exception e) {
					System.out.println(e.getMessage());
					return;
				}
				if (checkForBrokerAnswerFromMarket(ret))
					return;
				if (message.getMessageType().equals(MessageTypes.MESSAGE_SELL.toString()))
					marketForSellRequestLogic(ctx, ret);
				else
					marketForBuyRequestLogic(ctx, ret);
			}
		}

		private boolean checkForBrokerAnswerFromMarket(MessageSellOrBuy ret) {
			if (ret.getMessageAction().equals(MessageTypes.MESSAGE_EXECUTE.toString()) ||
					ret.getMessageAction().equals(MessageTypes.MESSAGE_REJECT.toString())) {
				System.out.println("Answer for your request: " + ret.getMessageAction());
				return true;
			}
			return false;
		}

		private void marketForSellRequestLogic(ChannelHandlerContext ctx, MessageSellOrBuy ret) {
			Random random = new Random();
			if (random.nextBoolean()) {
				System.out.println("EXECUTE. Thank you for this instrument!");
				ret.setMessageAction(MessageTypes.MESSAGE_EXECUTE.toString());
			} else {
				System.out.println("REJECT. Cause: we don't want this instrument.");
				ret.setMessageAction(MessageTypes.MESSAGE_REJECT.toString());
			}
			ret.setNewCheckSum();
			ctx.writeAndFlush(ret);
		}

		private void marketForBuyRequestLogic(ChannelHandlerContext ctx, MessageSellOrBuy ret) {
			Random random = new Random();
			int randomInt = random.nextInt(100);
			if (randomInt >= 0 && randomInt < 20) {
				System.out.println("REJECT. Cause: no such instrument on market!");
				ret.setMessageAction(MessageTypes.MESSAGE_REJECT.toString());
			} else if (randomInt >= 20 && randomInt < 40) {
				System.out.println("REJECT. Cause: not enough amount of such instrument on market!");
				ret.setMessageAction(MessageTypes.MESSAGE_REJECT.toString());
			} else {
				System.out.println("EXECUTE. Thank you for buying!");
				ret.setMessageAction(MessageTypes.MESSAGE_EXECUTE.toString());
			}
			ret.setNewCheckSum();
			ctx.writeAndFlush(ret);
		}

		private void channelWrite(ChannelHandlerContext ctx) {
			try {
				String input = getTextFromUser();
				if (input.length() == 0)
					throw new EmptyInput();
				else if (clientName.equals("Broker"))
					handleBrokerWrite(ctx, input);
			} catch (Exception e) {
				System.out.println(e.getMessage());
				channelWrite(ctx);
			}
		}

		private void handleBrokerWrite(ChannelHandlerContext ctx, String s) throws Exception {
			String[] split = s.split("\\s+");
			if (split.length != 5)
				throw new ErrorInput();
			MessageSellOrBuy out;
			int marketID = checkID(split[1]);
			String instrument = split[2];
			int quantity = Integer.valueOf(split[3]);
			int price = Integer.valueOf(split[4]);
			if (split[0].toLowerCase().equals("sell")) {
				out = new MessageSellOrBuy(MessageTypes.MESSAGE_SELL.toString(), "-",marketID, uniqueID, instrument, quantity, price);
			} else if (split[0].toLowerCase().equals("buy")) {
				out = new MessageSellOrBuy(MessageTypes.MESSAGE_BUY.toString(), "-",marketID, uniqueID, instrument, quantity, price);
			} else
				throw new ErrorInput();
			out.setNewCheckSum();
			ctx.writeAndFlush(out);
			System.out.println("Sending request to router..");
		}

		private int checkID(String id) throws Exception {
			int iID = Integer.valueOf(id);
			if (id.length() != 6)
				throw new ErrorInput();
			return iID;
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) {
			if (clientName.equals("Broker"))
				channelWrite(ctx);
		}

		private String getTextFromUser() throws Exception {
			System.out.println("Enter request message of type: [sell || buy] [market id] [instrument] [quantity] [price]");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			return br.readLine();
		}
	}
}

