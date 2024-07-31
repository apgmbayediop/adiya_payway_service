package sn.payway.payment.keys;

import javax.ejb.Stateless;
import javax.inject.Inject;

import lombok.extern.jbosslog.JBossLog;
import sn.apiapg.common.config.entities.ParametresGeneraux;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.aci.Caisse;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.hsm.HSMHandler;

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
