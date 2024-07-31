package sn.adiya.payment.keys;

import javax.ejb.Stateless;
import javax.inject.Inject;

import lombok.extern.jbosslog.JBossLog;
import sn.fig.common.config.entities.ParametresGeneraux;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.aci.Caisse;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.hsm.HSMHandler;

@Stateless
@JBossLog
public class KeyHandlerService {

	
	@Inject
	private HSMHandler hsm;
	
	public AbstractResponse getTerminalKey(String terminalSN) {
		AbstractResponse resp= new AbstractResponse();
		try {
			log.info("terminalKey");
			log.info(terminalSN);
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			Caisse caisse = sess.executeNamedQuerySingle(Caisse.class, "Caisse.findByTerminal",
					new String[] { "numeroSerie" }, new String[] {terminalSN});
			if (caisse == null) {
				resp.setCode(ErrorResponse.CAISSE_NOT_FOUND.getCode());
				resp.setMessage(ErrorResponse.CAISSE_NOT_FOUND.getMessage(""));
				log.info(resp.getMessage());
				return resp;
			}
			String ksnStr = HSMHandler.generateKsn(terminalSN);
			log.info(ksnStr);
			String param = "PARAM_HSM_APG";
			if (BEConstantes.COD_BANQUE_FINAO_GIM
					.equals(caisse.getPointDeVente().getCommercant().getParent().getCodeBanque())) {
				param = "PARAM_HSM_FINAO";
			} else if (BEConstantes.COD_BANQUE_CFP_GIM
					.equals(caisse.getPointDeVente().getCommercant().getParent().getCodeBanque())) {
				param = "PARAM_HSM_CFP";
			}
			log.info(param);
			log.info(caisse.getPointDeVente().getCommercant().getParent().getCodeBanque());
			String baseKey = getBaseKey(param);
			if (baseKey == null || baseKey.isEmpty()) {
				resp.setCode(ErrorResponse.CONFIG_TERMINAL_ERROR.getCode());
				resp.setMessage("Terminal base key not added");
				log.info(resp.getMessage());
				return resp;
			}
			byte[] ksn = HSMHandler.hexStringToByteArray(ksnStr);
			hsm.connect();
			byte[] ipek = hsm.generateIPEK(ksn, baseKey);
			resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
			resp.setMessage(HSMHandler.hexToString(ksn) + "!" + HSMHandler.hexToString(ipek));
			log.info("IPEK generated ");
			return resp;
		} catch (Exception e) {
			log.error("errorKey", e);
			return new AbstractResponse(ErrorResponse.UNKNOWN_ERROR.getCode(),
					ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
	}
	private String getBaseKey(String key) {
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		ParametresGeneraux param = session.findObjectById(ParametresGeneraux.class, null, key);
		if (param != null) {
			String[] data = param.getLibelle().split(";");
			return data[0];
		}
		return null;
	}
}
