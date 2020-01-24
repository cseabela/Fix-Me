package com.core.messages;

import com.core.MD5Creator;

public class MessageSellOrBuy extends FIXMessage {
	private int		actionLength;
	private String	messageAction;
	private int		id;
	private int		instrumentLength;
	private String	instrument;
	private int		quantity;
	private int		price;

	public MessageSellOrBuy(String messageType, String messageAction, int marketId, int id, String instrument, int quantity, int price) {
		super(messageType, marketId);
		this.messageAction = messageAction;
		this.actionLength = messageAction.length();
		this.id = id;
		this.instrument = instrument;
		this.instrumentLength = instrument.length();
		this.quantity = quantity;
		this.price = price;
		setChecksum(getMsgMD5());
	}

	public MessageSellOrBuy() {}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getInstrument() {
		return instrument;
	}

	public void setInstrument(String instrument) {
		this.instrument = instrument;
		instrumentLength = instrument.length();
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public int getInstrumentLength() {
		return instrumentLength;
	}

	public String getMsgMD5() {
		return MD5Creator.createMD5FromObject(String.valueOf(id).concat(instrument).concat(String.valueOf(quantity)).concat(messageAction));
	}

	public void setNewCheckSum() {
		setChecksum(getMsgMD5());
	}

	public String getMessageAction() {
		return messageAction;
	}

	public void setMessageAction(String messageAction) {
		this.messageAction = messageAction;
		this.actionLength = messageAction.length();
	}

	public int getActionLength() {
		return actionLength;
	}

	@Override
	public String toString() {
		return "MessageSellOrBuy {" +
				"ID = " + id +
				"|MSG_TYPE = '" + getMessageType() + "'" +
				"|MSG_ACTION = '" + messageAction + "'" +
				"|INSTRUMENT = '" + instrument + "'" +
				"|MARKET_ID = " + getMarketId() +
				"|QUANTITY = " + quantity +
				"|PRICE = " + price +
				"|CHECKSUM = '" + getChecksum() + "'" +
				'}';
	}
}
