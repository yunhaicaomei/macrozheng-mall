package com.macro.mall.portal.vo.API;

public class APIListTraRespVO {
	private String id;
	private String status;
	private String type;
	private String amount;
	private String currency;
	private String time;
	private String reference;
	private String note;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("APIListTraRespVO [id=");
		builder.append(id);
		builder.append(", status=");
		builder.append(status);
		builder.append(", type=");
		builder.append(type);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", currency=");
		builder.append(currency);
		builder.append(", time=");
		builder.append(time);
		builder.append(", reference=");
		builder.append(reference);
		builder.append(", note=");
		builder.append(note);
		builder.append("]");
		return builder.toString();
	}
}
