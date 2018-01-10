package com.casad.weatherwatcher;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestEmailConnectivity {
	
	private final Configuration config = Configuration.getConfig();
	private static final String HOST = "smtp.gmail.com";
	private static final Properties CONNECTION_PROPERTIES = new Properties();
	
	@BeforeClass
	public static void setUp() {
		CONNECTION_PROPERTIES.put("mail.smtp.host", HOST);
		CONNECTION_PROPERTIES.put("mail.smtp.socketFactory.port", "465");
		CONNECTION_PROPERTIES.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		CONNECTION_PROPERTIES.put("mail.smtp.auth", "true");
		CONNECTION_PROPERTIES.put("mail.smtp.port", "465");
		
		CONNECTION_PROPERTIES.put("mail.pop3.host", HOST);
		CONNECTION_PROPERTIES.put("mail.pop3.port", "995");
		CONNECTION_PROPERTIES.put("mail.pop3.starttls.enable", "true");
	}
	
	@Test
	public void sendEmail() throws IOException {
		Session session = Session.getDefaultInstance(CONNECTION_PROPERTIES, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(config.getEmailUsername(), config.getEmailPassword());
			}
		});

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(config.getEmailUsername()));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.getEmailUsername()));
			message.setSubject("Testing Subject");
			message.setText("Dear Mail Crawler," + "\n\n No spam to my email, please!");

			Transport.send(message);

			System.out.println("Done");

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void getEmail() throws IOException {
		try {
			Session emailSession = Session.getDefaultInstance(CONNECTION_PROPERTIES, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(config.getEmailUsername(), config.getEmailPassword());
				}
			});

			// create the POP3 store object and connect with the pop server
			Store store = emailSession.getStore("pop3s");

			store.connect(HOST, config.getEmailUsername(), config.getEmailPassword());

			// create the folder object and open it
			Folder emailFolder = store.getFolder("INBOX");
			emailFolder.open(Folder.READ_ONLY);

			// retrieve the messages from the folder in an array and print it
			Message[] messages = emailFolder.getMessages();
			System.out.println("messages.length---" + messages.length);

			for (int i = 0, n = messages.length; i < n; i++) {
				Message message = messages[i];
				System.out.println("---------------------------------");
				System.out.println("Email Number " + (i + 1));
				System.out.println("Subject: " + message.getSubject());
				System.out.println("From: " + message.getFrom()[0]);
				System.out.println("Text: " + message.getContent().toString());

			}

			// close the store and folder objects
			emailFolder.close(false);
			store.close();

		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
