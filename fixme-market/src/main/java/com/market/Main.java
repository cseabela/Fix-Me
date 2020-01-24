package com.market;

import com.core.Client;

public class Main {
    public static void main(String[] args) {
        Client client = new Client("Market");
        Thread clientThread = new Thread(client);
        clientThread.start();
        try {
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Client.handleInput(client);
    }
}
