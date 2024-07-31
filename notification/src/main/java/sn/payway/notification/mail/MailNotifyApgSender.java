package sn.payway.notification.mail;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
@Stateless
public class MailNotifyApgSender {

	
	private static final String ALERT = "\n\n\n"
			+ "AVERTISSEMENT \n Les opinions exprimees dans cet e-mail sont uniquement celles de l'expediteur et ne sauraient constituer un engagement formel de APG. \n L'information contenue dans ce document est exclusivement destinee a l'attention du destinataire et peut etre confidentielle. \n Copier, divulguer ou utiliser le contenu de cet e-mail peut etre illegal. \n Pensez à l'environnement avant d'imprimer cet e-mail.";

	private static final String MAIL_APG_OPT="notifyapg@outlook.fr";
	private static final String PASSWORD_APG_OPT="Apg@Optima2022";
	
	private static final String MAIL_IPRES="cssipresportail@secusociale.sn";
	private static final String PASSWORD_IPRES="RVpho023";
	
	private static final String HOST ="mail.smtp.host";
	private static final String PORT ="mail.smtp.port";
	private static final String AUTH ="mail.smtp.auth";
	private static final String STARTTLS ="mail.smtp.starttls.enable";
	private static final int PORT_587=587;
	private static final String OFFICE_HOST="smtp.office365.com";
	
	
	@Asynchronous
	public void sendSimpleMail(String object,String message,String toAddress,List<File> files) {
		try {
			
			Properties props = new Properties();
			props.put(HOST, "smtp-mail.outlook.com");
			props.put(PORT, PORT_587);
			props.put(AUTH, Boolean.TRUE);
			props.put(STARTTLS, Boolean.TRUE);
			Session session = Session.getInstance(props, new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(MAIL_APG_OPT, PASSWORD_APG_OPT);
				}
			});
			Multipart mPart = new MimeMultipart();
			Message mimeMessage = new MimeMessage(session);
			mimeMessage.setFrom(new InternetAddress(MAIL_APG_OPT));
			mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
			mimeMessage.setSubject(object);
			mimeMessage.setText(message+ALERT);
			MimeBodyPart messageBodyPart = new MimeBodyPart();
	        messageBodyPart.setText(message);
			mPart.addBodyPart(messageBodyPart);
			if (files == null||files.isEmpty()) {
			}else {
				for (File file:files) {
					MimeBodyPart filePart = new MimeBodyPart();
					filePart.attachFile(file);
					mPart.addBodyPart(filePart);
				}
				mimeMessage.setContent(mPart);
			}
			Transport.send(mimeMessage);
		}
		catch (Exception e) {
			log.info("errorEmail "+e.getMessage());	
		}
	}

	@Asynchronous
	public void sendMailFilesIpress(String object,String message,String toAddress, List<File> files) {
		try {
			Properties props = new Properties();
			props.put(HOST, OFFICE_HOST);
			props.put(PORT, 587);
			props.put(AUTH, Boolean.TRUE);
			props.put(STARTTLS, Boolean.TRUE);
			Session session = Session.getInstance(props, new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(MAIL_IPRES, PASSWORD_IPRES);
				}
			});
			Multipart mPart = new MimeMultipart();
			
			Message mimeMessage = new MimeMessage(session);
			mimeMessage.setFrom(new InternetAddress(MAIL_IPRES));
			mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
			mimeMessage.setSubject(object);
			mimeMessage.setSentDate(new Date());
			mimeMessage.setText(message);
			
			MimeBodyPart messageBodyPart = new MimeBodyPart();
	        messageBodyPart.setText(message);
			mPart.addBodyPart(messageBodyPart);
			if (files == null||files.isEmpty()) {
			}else {
				for (File file:files) {
					MimeBodyPart filePart = new MimeBodyPart();
					filePart.attachFile(file);
					mPart.addBodyPart(filePart);
				}
				mimeMessage.setContent(mPart);
			}
			
			Transport.send(mimeMessage);
		}
		catch (Exception e) {
			log.error("errorEmailIpress ",e);	
		}
	}
}
