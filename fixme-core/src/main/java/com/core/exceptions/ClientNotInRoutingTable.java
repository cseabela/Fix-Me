package com.core.exceptions;

public class ClientNotInRoutingTable extends Exception {
	public ClientNotInRoutingTable() {
		super("This client is not in routing table!");
	}
}
