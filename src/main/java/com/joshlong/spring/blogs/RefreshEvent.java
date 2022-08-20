package com.joshlong.spring.blogs;

import org.springframework.context.ApplicationEvent;

import java.util.Date;

public class RefreshEvent extends ApplicationEvent {

	public RefreshEvent() {
		this(new Date());
	}

	public RefreshEvent(Date source) {
		super(source);
	}

	@Override
	public Date getSource() {
		return (Date) super.getSource();
	}

}
