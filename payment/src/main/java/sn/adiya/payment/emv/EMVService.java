package sn.adiya.payment.emv;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;

import lombok.extern.jbosslog.JBossLog;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.Currency;
import sn.fig.entities.Partner;
import sn.fig.entities.aci.Caisse;
import sn.fig.entities.aci.CommissionMonetique;
import sn.fig.entities.aci.TMK;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.hsm.HSMHandler;
import sn.adiya.common.utils.Constantes;
import sn.adiya.payment.dto.PaymentDetails;
import sn.adiya.payment.dto.PaymentDto;
import sn.adiya.payment.gim.GimService;
import sn.adiya.payment.utils.GimUtils;
import sn.adiya.transaction.service.CommissionService;

@Stateless
@JBossLog
public class EMVService {
	
	@Inject
	private GimUtils gimUtils;
	@Inject
	private HSMHandler hsm;
	@Inject
	private GimService gimService;
	@Inject
	private CommissionService commService;

	public PaymentDetails payment( PaymentDto dto) {
		PaymentDetails resp = new PaymentDetails();
		try {
			log.info("emvPayment");
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			Caisse caisse = session.executeNamedQuerySingle(Caisse.class, "Caisse.findByTerminal",
					new String[] { "numeroSerie" }, new String[] { dto.getTerminalSN() });
			TMK tmk = gimUtils.findTmk(hsm,session,  caisse.getPointDeVente().getCommercant());
			Currency currency = session.executeNamedQuerySingle(Currency.class, "Currency.findByNameOrNumber", new String[] {"code"}, new String[] {dto.getCurrencyName()});
			if(currency.getCurrencyName().equals(caisse.getPointDeVente().getCommercant().getCurrencyName())) {
			log.info(tmk.getIndexCle());
			String indexCle = tmk.getIndexCle();
			String zpkPinBlock= gimUtils.translatePin(dto.getPinBlock(), tmk, dto.getPan(), hsm);
		     String network=getNetwork(dto.getPan(), indexCle);
		     if("GIM".equals(network)) {
		    	 log.info("gim card");
					if ("8092".equals(zpkPinBlock)) {
						log.info("probleme de calcul pinblock");
						resp.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
						resp.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
						
					}
					Partner commercant = caisse.getPointDeVente().getCommercant();
					dto.setPinBlock(zpkPinBlock);
					CommissionMonetique reqCom = new CommissionMonetique();
					reqCom.setPartner(caisse.getPointDeVente().getCommercant());
					reqCom.setChannelType(dto.getTransactionType());
					reqCom.setMeansType(dto.getMeansType());
					reqCom.setSubMeansType("ALL");
					CommissionMonetique comm=commService.findCommissionMonetique(reqCom);
					dto.setRequestId(RandomStringUtils.randomAlphanumeric(15));
					dto.setMeansType(Constantes.CARTE);
					dto.setMerchantAddress(commercant.getName());
					dto.setMerchantNumber(commercant.getIdPartner().toString());
					dto.setTerminalNumber(caisse.getNumeroCaisse());
					resp = gimService.createTransactionGim(dto, caisse,comm);
					log.info(resp.getCode());
				
		     }else {
		    	 resp.setCode(ErrorResponse.UNAUTHORIZED_OPERATION.getCode());
		    	 resp.setMessage(ErrorResponse.UNAUTHORIZED_OPERATION.getMessage(""));
		     }
			}else {
				resp.setCode(ErrorResponse.CURRENCY_ERROR.getCode());
				resp.setMessage(ErrorResponse.CURRENCY_ERROR.getMessage(""));
			}
			
		}
		catch (Exception e) {
		 log.error("emvPayError", e);
		 resp.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
		 resp.setMeansType(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		return resp;
	}
	
	private String getNetwork(String pan,String indexCle) {
		String network;
		if(GimService.FINAO_INDEX.equals(indexCle)&& pan.startsWith(BEConstantes.BIN_FINAO)) {
			network ="FINAO"; 
		}else if(GimService.BRM_INDEX.equals(indexCle)&& pan.startsWith(BEConstantes.BIN_APG)) {
			network ="BRM"; 
		}else if(GimService.CFP_INDEX.equals(indexCle)&& pan.startsWith(BEConstantes.BIN_CFP)) {
			network="CFP";
		}else  {
			network ="GIM";
		}
		return network;
	}

}
