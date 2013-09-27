package com.tpsoft.tuixin.model;

import java.util.Date;

import com.tpsoft.pushnotification.model.MyMessage;

public class MyMessageSupportSave extends MyMessage {

	private Long recordId; // 空值表示未保存到数据库，否则表示数据库中的记录ID

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
		this.setId(message.getId());
	}
	
	public MyMessageSupportSave() {
		
	}

	public Long getRecordId() {
		return recordId;
	}

	public void setRecordId(Long recordId) {
		this.recordId = recordId;
	}

}
