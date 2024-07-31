package sn.payway.payment.services;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logging.Logger;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.aci.Caisse;
import sn.apiapg.entities.aci.LinkedPos;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.utils.Constantes;
import sn.payway.common.utils.PaywayException;
import sn.payway.merchant.dto.PosResponse;
import sn.payway.payment.dto.PaymentDetails;
import sn.payway.payment.dto.PaymentDto;

@Stateless
public class CashierMerchantService {

	private static final Logger LOG = Logger.getLogger(CashierMerchantService.class);
	@Inject
	private OnlinePaymentService onlinePay;
	@Inject
	private WalletPayService walletPay;

	public PaymentDetails payment(Utilisateur user, PaymentDto dto) throws PaywayException {
		PaymentDetails response;
		Session session = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		LinkedPos linkedPos = session.executeNamedQuerySingle(LinkedPos.class, "lpos.findbyUser",
				new String[] {"utilisateur"}, new Utilisateur[] {user});
		
		if("CAISSE".equals(linkedPos.getEntityType())) {
			Caisse caisse =  session.executeNamedQuerySingle(Caisse.class, "Caisse.findByNumeroCaisse",
					new String[] {"numeroCaisse"}, new String[] {linkedPos.getIdEntity()});
			dto.setRequestId(RandomStringUtils.randomAlphanumeric(10));
			dto.setCurrencyName(caisse.getPointDeVente().getCommercant().getCurrencyName());
			dto.setTerminalNumber(caisse.getNumeroCaisse());
			dto.setMerchantNumber(caisse.getPointDeVente().getCommercant().toString());
			dto.setMerchantAddress(caisse.getPointDeVente().getCommercant().getName());
			response = Constantes.WALLET.equals(dto.getMeansType())? walletPay.directPayment(caisse,dto):onlinePay.initiateOnlinePayment(dto);
		    response.setMerchantName(dto.getMerchantAddress());
		}else {
			response = new PaymentDetails();
			response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			response.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		return response;
	}

	public PosResponse details(Utilisateur user) throws PaywayException {
			Session session = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
			LinkedPos linkedPos = session.executeNamedQuerySingle(LinkedPos.class, "lpos.findbyUser",
					new String[] {"utilisateur"}, new Utilisateur[] {user});
			if(linkedPos ==null) {
				throw new PaywayException(ErrorResponse.CAISSE_NOT_FOUND.getCode(), ErrorResponse.CAISSE_NOT_FOUND.getMessage(""));
			}
			if("CAISSE".equals(linkedPos.getEntityType())) {
				Caisse caisse =  session.executeNamedQuerySingle(Caisse.class, "Caisse.findByNumeroCaisse",
						new String[] {"numeroCaisse"}, new String[] {linkedPos.getIdEntity()});
				PosResponse response  = new PosResponse();
				response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
				response.setMerchantName(caisse.getPointDeVente().getCommercant().getName());
				response.setMerchantNumber(caisse.getPointDeVente().getCommercant().getIdPartner().toString());
				response.setPosNumber(caisse.getPointDeVente().getNumeroPointDeVente());
				response.setTerminalNumber(caisse.getNumeroCaisse());
				LOG.info(caisse.getNumeroCaisse());
				return response;
				}else 
					throw new PaywayException(ErrorResponse.CAISSE_NOT_FOUND.getCode(), ErrorResponse.CAISSE_NOT_FOUND.getMessage(""));
	}

	
}
