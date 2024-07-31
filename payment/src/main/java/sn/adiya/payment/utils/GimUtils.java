package sn.adiya.payment.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import sn.fig.common.config.entities.ParametresGeneraux;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.Card;
import sn.fig.entities.Partner;
import sn.fig.entities.aci.TMK;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.hsm.HSMHandler;
import sn.adiya.common.utils.Constantes;
import sn.adiya.common.utils.AdiyaCommonTools;
import sn.adiya.payment.gim.GimService;

@Stateless
public class GimUtils {
	
	private static final Logger LOG = Logger.getLogger(GimUtils.class);
	
	private String finaoPosBdk;
	private String apgBrmPosBDK;
	private String cfpPosBdk;
	
	@PostConstruct
	public void init() {

		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		ParametresGeneraux param = session.findObjectById(ParametresGeneraux.class, null, "PARAM_HSM_FINAO");
		if (param != null) {
			String[] data = param.getLibelle().split(";");
			finaoPosBdk = data[1];
		}
		ParametresGeneraux param1 = session.findObjectById(ParametresGeneraux.class, null, "PARAM_HSM_APG");

		if (param1 != null) {
			String[] data = param1.getLibelle().split(";");
			apgBrmPosBDK = data[1];
		}
		ParametresGeneraux param2 = session.findObjectById(ParametresGeneraux.class, null, "PARAM_HSM_CFP");

		if (param2 != null) {
			String[] data = param2.getLibelle().split(";");
			cfpPosBdk = data[1];
		}

	}
	@Inject
	private AdiyaCommonTools config;
	
	public AbstractResponse checkadiyaCard(Card card) {
		LOG.info("check card");
		AbstractResponse response;
		if (card == null) {
			LOG.info("card not found ");
			response = new AbstractResponse(ErrorResponse.CARD_UNKKNOWN.getCode(), ErrorResponse.CARD_UNKKNOWN.getMessage(""));
		}else {

		LOG.info("check card status");
		if (BEConstantes.CARD_ACTIVE.equalsIgnoreCase(card.getStatus())) {
		
		String date = new SimpleDateFormat("yyMM",Locale.getDefault()).format(new Date());
        if (date.compareTo(card.getExpiryDate())>=0) {
			LOG.info("verify "+ErrorResponse.CARD_EXPIRED.getMessage("")+" "+card.getExpiryDate());
			response = new AbstractResponse(ErrorResponse.CARD_EXPIRED.getCode(), ErrorResponse.CARD_EXPIRED.getMessage(""));
		}else	{
        response =new AbstractResponse(ErrorResponse.REPONSE_SUCCESS.getCode(), "");
		}
		}else {
			LOG.info("card not active");
			response =new AbstractResponse(ErrorResponse.CARD_INACTIVE.getCode(), ErrorResponse.CARD_INACTIVE.getMessage(""));
		}
	}
		return response;
	}
	
	public TMK findTmk(HSMHandler hsm,Session session, Partner commercant) {
		LOG.info("get TMK");
		String indexCle = GimService.BRM_INDEX;
		if (BEConstantes.COD_BANQUE_FINAO_GIM.equals(commercant.getParent().getCodeBanque())) {
			indexCle = GimService.FINAO_INDEX;
		} else if (BEConstantes.COD_BANQUE_CFP_GIM.equals(commercant.getParent().getCodeBanque())) {
			indexCle = GimService.CFP_INDEX;
		}
		TMK tmk = session.executeNamedQuerySingle(TMK.class, "TMK.findByZPK",
				new String[] { "fournisseur", "indexCle" }, new String[] { "GIM", indexCle });
		
		return tmk;
	}
	public String translatePin(String pinBlockksn,TMK tmk,String pan,HSMHandler hsm) {
		
		LOG.info("Translate PIN");
		String ksn = pinBlockksn.substring(16, pinBlockksn.length());
		String pinBlock = pinBlockksn.substring(0, 16);
		String bdk = apgBrmPosBDK;
		if (GimService.FINAO_INDEX.equals(tmk.getIndexCle())) {
			bdk = finaoPosBdk;
		} else if (GimService.CFP_INDEX.equals(tmk.getIndexCle())) {
			bdk = cfpPosBdk;
		}
		String zpkPinBlock = "";
		if (!Constantes.LOCAL_ENV.equals(config.getProperty(Constantes.ENV_PRPOPERTY))) {
			zpkPinBlock = HSMHandler.hexToString(
					hsm.translatePINfromBDKToZPK(ksn, pinBlock, pan, tmk.getValeurCle(), bdk));
			if ("8092".equals(zpkPinBlock)) {
				LOG.info("hsm failed retry ");
				zpkPinBlock = HSMHandler.hexToString(
						hsm.translatePINfromBDKToZPK(ksn, pinBlock, pan, tmk.getValeurCle(), bdk));
			}
		}
		return zpkPinBlock;
	}
	
	
}
