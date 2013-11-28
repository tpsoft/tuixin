package com.tpsoft.tuixin.model;

import java.util.Date;

import com.tpsoft.pushnotification.model.MyMessage;

public class MyMessageSupportSave extends MyMessage {

	private Long recordId; // 空值表示未保存到数据库，否则表示数据库中的记录ID
	private int messageId; // 消息ID(用于列表与弹出窗之间通信)
	private boolean favorite; // 是否收藏(默认为false)

	public MyMessageSupportSave(String sender, String receiver, String title,
			String body, String type, String url, Date generateTime,
			Date expiration, Attachment[] attachments) {
		super(sender, receiver, title, body, type, url, generateTime,
				expiration, attachments);
	}

	public MyMessageSupportSave(String title, String body, String type,
			String url, Date generateTime, Date expiration,
			Attachment[] attachments) {
		super(title, body, type, url, generateTime, expiration, attachments);
	}
	
	public MyMessageSupportSave(MyMessage message) {
		super(message.getSender(), message.getReceiver(), message.getTitle(), message.getBody(),
				message.getType(), message.getUrl(), message.getGenerateTime(),
				message.getExpiration(), message.getAttachments());
	}
	
	public MyMessageSupportSave() {
		
	}

	public Long getRecordId() {
		return recordId;
	}

	public void setRecordId(Long recordId) {
		this.recordId = recordId;
	}

	public int getMessageId() {
		return messageId;
	}

	public void setMessageId(int messageId) {
		this.messageId = messageId;
	}

	public boolean isFavorite() {
		return favorite;
	}

	public void setFavorite(boolean favorite) {
		this.favorite = favorite;
	}

}
