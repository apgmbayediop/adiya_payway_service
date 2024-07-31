package sn.adiya.payment.services;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logging.Logger;

import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.aci.Caisse;
import sn.fig.entities.aci.LinkedPos;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.utils.Constantes;
import sn.adiya.common.utils.AdiyaException;
import sn.adiya.merchant.dto.PosResponse;
import sn.adiya.payment.dto.PaymentDetails;
import sn.adiya.payment.dto.PaymentDto;

@Stateless
public class CashierMerchantService {

	private static final Logger LOG = Logger.getLogger(CashierMerchantService.class);
	@Inject
	private OnlinePaymentService onlinePay;
	@Inject
	private WalletPayService walletPay;

	public PaymentDetails payment(Utilisateur user, PaymentDto dto) throws AdiyaException {
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

	public PosResponse details(Utilisateur user) throws AdiyaException {
			Session session = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
			LinkedPos linkedPos = session.executeNamedQuerySingle(LinkedPos.class, "lpos.findbyUser",
					new String[] {"utilisateur"}, new Utilisateur[] {user});
			if(linkedPos ==null) {
				throw new AdiyaException(ErrorResponse.CAISSE_NOT_FOUND.getCode(), ErrorResponse.CAISSE_NOT_FOUND.getMessage(""));
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
					throw new AdiyaException(ErrorResponse.CAISSE_NOT_FOUND.getCode(), ErrorResponse.CAISSE_NOT_FOUND.getMessage(""));
	}

	
}
