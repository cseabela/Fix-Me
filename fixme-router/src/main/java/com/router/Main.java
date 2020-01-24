package com.router;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
	public static void main(String[] args) {
		Server brokerServer = new Server(Server.BROKER_SERVER);
		Thread brokerServerThread = new Thread(brokerServer);
		brokerServerThread.start();
		Server marketServer = new Server(Server.MARKET_SERVER);
		Thread marketServerThread = new Thread(marketServer);
		marketServerThread.start();
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
				brokerServer.shutDown();
				marketServer.shutDown();
				break;
			}
		}
	}
}
