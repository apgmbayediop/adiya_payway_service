package sn.payway.payment.gim;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;

import org.jboss.logging.Logger;

import sn.apiapg.common.config.entities.ParametresGeneraux;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;

@Stateless
public class CfpGimControllerOperation {

	private static final Logger LOG = Logger.getLogger(CfpGimControllerOperation.class);

	private Integer port = 0;
	private String address;
	private static final String ID_BANQUE = "CFP";
	
	@PostConstruct
	public void init() {
		LOG.info("init brm gim controller operation");
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		ParametresGeneraux param2 = session.findObjectById(ParametresGeneraux.class, null, "PARAM_SWITCH_CFP");
		if (param2 == null) {
			LOG.info("param PARAM_SWITCH_APG is null ");
		} else {
			String[] data = param2.getLibelle().split(";");
			address = data[0];
			port = Integer.parseInt(data[1]);
		}
	}
	public byte[] sendTransactionToGim(byte[] toSend) {
		try {
			LOG.info("sendTransactionToGim " + ID_BANQUE);
			int len;
			byte[] received = new byte[2000];
			try (Socket clientSocketAPG = new Socket(address, port)) {
				clientSocketAPG.setKeepAlive(true);
				clientSocketAPG.setSoTimeout(15_000);
				try (DataInputStream dataInAPG = new DataInputStream(
						new BufferedInputStream(clientSocketAPG.getInputStream()))) {
					try (DataOutputStream dataOutAPG = new DataOutputStream(
							new BufferedOutputStream(clientSocketAPG.getOutputStream()))) {
						dataOutAPG.write(toSend);
						dataOutAPG.flush();
						LOG.info("waiting return");
						len = dataInAPG.read(received);
					}
				}
			}
			if (len < 0) {
				return new byte[0];
			}
			byte[] data = new byte[len - 15];
			System.arraycopy(received, 15, data, 0, len - 15);
			return data;

		} catch (SocketTimeoutException e) {
			LOG.error("TImeoutException " + e.getMessage());
			return new byte[1];
		} catch (SocketException e) {
			LOG.error("ExceptionSocket " + e.getMessage());
			return new byte[0];
		} catch (IOException e) {
			LOG.error("ExceptionIO " + e.getMessage());
			return new byte[0];
		}
	}
}
