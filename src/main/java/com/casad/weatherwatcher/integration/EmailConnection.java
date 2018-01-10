package com.casad.weatherwatcher.integration;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailConnection {

	private final Properties connectionProperties;
	private final String username;
	private final String password;
	private final String notifyAddress;
	
	public EmailConnection(Properties aConnectionProperties, String aUsername, String aPassword, String aNotificationAddress) {
		connectionProperties = aConnectionProperties;
		username = aUsername;
		password = aPassword;
		notifyAddress = aNotificationAddress;
	}

	
	public void sendMessage(String aSubject, String aMessage) {

		Session session = Session.getDefaultInstance(connectionProperties,
			new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			});

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(notifyAddress));
			message.setSubject(aSubject);
			message.setText(aMessage);

			Transport.send(message);

			System.out.println("Done");

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}
