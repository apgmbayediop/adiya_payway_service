package sn.payway.payment.gim;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;

import sn.apiapg.common.config.entities.ParametresGeneraux;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.MailUtils;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.hsm.HSMHandler;
import sn.payway.common.utils.Constantes;
import sn.payway.common.utils.PaywayCommonTools;

@Singleton
public class CfpGimController {

	private static final Logger LOG = Logger.getLogger(CfpGimController.class);

	@Inject
	private PaywayCommonTools props;
	private String template;
	private Integer port = 0;
	private Integer sessionTime = 0;
	private Integer failedTime = 0;
	private String address;
	private static final String ID_BANQUE = "CFP";
	private String mailNotify;

	@PostConstruct
	public void init() {
		LOG.info("init brm cfp controller");
		String sep = File.separator;
		template = BEConstantes.path + sep + "config" + sep + "isoascii.xml";
		LOG.info("template " + template);
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		ParametresGeneraux param2 = session.findObjectById(ParametresGeneraux.class, null, "PARAM_SWITCH_CFP");
		if (param2 == null) {
			LOG.info("param PARAM_SWITCH_APG is null ");
		} else {
			String[] data = param2.getLibelle().split(";");
			address = data[0];
			port = Integer.parseInt(data[1]);
		}
		ParametresGeneraux param3 = session.findObjectById(ParametresGeneraux.class, null, "PARAM_MAIL_NOTIFY_ERROR");
		if (param3 == null) {
			LOG.info("mail notify not configured is null ");
		} else {
			mailNotify = param3.getLibelle();
		}

	}
	//@Schedule(hour = "*", minute = "*/30", persistent = false)
	//@AccessTimeout(unit = TimeUnit.MINUTES, value = 3)
	@Lock(LockType.WRITE)
	protected void signOn() {
		String hh = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH").withZone(ZoneId.systemDefault()));
		boolean hours="22".equals(hh) || "23".equals(hh) || "00".equals(hh) || "01".equals(hh) || "02".equals(hh)
				|| "03".equals(hh) || "04".equals(hh) || "05".equals(hh) || "06".equals(hh);
		if (!hours&& Constantes.PREPROD_ENV.equals(props.getProperty(Constantes.ENV_PRPOPERTY))) {
				sendSignOn();
		}
	}

	@Lock(LockType.READ)
	private void sendSignOn() {
		try {
			if (address == null) {
				init();
			}
			try(InputStream stream = Files.newInputStream(Paths.get(template))){
			GenericPackager packager = new GenericPackager(stream);
			String dateYYMMddHHmmss = DateTimeFormatter.ofPattern("yyMMddHHmmss", Locale.getDefault())
					.withZone(ZoneId.systemDefault()).format(Instant.now());
			String systemAudit = Long.toString(System.currentTimeMillis());
			systemAudit = systemAudit.substring(systemAudit.length() - 6);
			ISOMsg msg;
			msg = packager.createISOMsg();
			msg.setPackager(packager);
			msg.setMTI("1804");
			msg.set(7, dateYYMMddHHmmss.substring(0, 10));
			msg.set(11, systemAudit);
			msg.set(12, dateYYMMddHHmmss);
			msg.set(24, "803");
			msg.set(25, Constantes.ISO_SUCCESS_STATUS);
			msg.set(37, systemAudit + systemAudit);
			msg.set(128, HSMHandler.hexStringToByteArray("00000000"));
			msg.set(33, "280124");
			byte[] pack = msg.pack();
			byte[] toSend = formIsoData(pack);
			byte[] data = sendData(toSend);
			if (data.length == 0) {
				notifyFailConnexion();

			} else {

				ISOMsg isoResp = new ISOMsg();
				isoResp.setPackager(packager);
				isoResp.unpack(data);
				restablishConnexion();
			}
		}

		} catch (Exception e) {
			LOG.error("Exception " + e.getMessage());
		}
	}

	@Lock(LockType.READ)
	private byte[] formIsoData(byte[] isob) {
		String strHearder = "000" + (isob.length + 11) + "ISO70100000";
		byte[] head = strHearder.substring(strHearder.length() - 15, strHearder.length()).getBytes();
		byte[] toSend = new byte[head.length + isob.length];
		System.arraycopy(head, 0, toSend, 0, head.length);
		System.arraycopy(isob, 0, toSend, head.length, isob.length);
		return toSend;
	}

	@Lock(LockType.WRITE)
	private byte[] sendData(byte[] toSend) {
		try {
			int len;
			byte[] received = new byte[2000];
			try(Socket clientSocketAPG = new Socket(address, port)) {
			clientSocketAPG.setKeepAlive(true);
			clientSocketAPG.setSoTimeout(15_000);
			try(DataInputStream dataInAPG = new DataInputStream(new BufferedInputStream(clientSocketAPG.getInputStream()))){
					try(DataOutputStream dataOutAPG = new DataOutputStream(
					new BufferedOutputStream(clientSocketAPG.getOutputStream()))){

			dataOutAPG.write(toSend);
			dataOutAPG.flush();
			len = dataInAPG.read(received);
			}
			}
			}
			if (len <= 0) {
				sessionTime = 0;
				return new byte[0];
			}
			byte[] data = new byte[len - 15];
			System.arraycopy(received, 15, data, 0, len - 15);
			sessionTime++;
			return data;

		} catch (SocketTimeoutException e) {
         failedTime++;
         sessionTime = 0;
			LOG.error("TImeoutException " + e.getMessage());
			return new byte[0];
		} catch (SocketException e) {
			failedTime++;
			sessionTime = 0;
			LOG.error("ExceptionSocket " + e.getMessage());
			return new byte[0];
		} catch (IOException e) {
			LOG.error("ExceptionIO " + e.getMessage());
			sessionTime = 0;
			return new byte[0];
			
		}
	}

	@Lock(LockType.READ)
	private void notifyFailConnexion() {
		try {
			LOG.info("not read data for signOn");
			String body = "l'interface GIM  de " + ID_BANQUE + " n'est pas disponible depuis " + failedTime
					+ " mn. Veuillez vérifier";
			MailUtils.sendEmails(mailNotify,
					"GIM interface non disponible " + props.getProperty(Constantes.ENV_PRPOPERTY), body, false, null,
					null);
		} catch (Exception e) {
			LOG.error("failConnexion", e);
		}
	}

	@Lock(LockType.READ)
	private void restablishConnexion() {
		try {
			LOG.info("sessionTime " + ID_BANQUE + sessionTime);
			if (failedTime != 0) {
				String object = "GIM retablissement connexion " + props.getProperty(Constantes.ENV_PRPOPERTY);
				String body = "retablissement connexion interface " + ID_BANQUE + " GIM aprés " + failedTime + " mn";
				MailUtils.sendEmails(mailNotify, object, body, false, null, null);
				failedTime = 0;
			}
		} catch (Exception e) {
			LOG.error("restatbConnex", e);
		}
	}
}
