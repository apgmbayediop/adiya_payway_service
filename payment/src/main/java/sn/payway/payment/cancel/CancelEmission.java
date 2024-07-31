package sn.payway.payment.cancel;

import java.math.BigDecimal;
import java.util.Date;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.entities.IsoAutorisation;
import sn.apiapg.entities.Transaction;
import sn.apiapg.session.Session;
import sn.payway.common.utils.Constantes;
import sn.payway.partner.uimcec.UIMCECService;
import sn.payway.partner.uimcec.UimcecPayDto;
import sn.payway.partner.uimcec.UimcecRequest;
import sn.payway.payment.dto.PaymentDetails;

@Stateless
public class CancelEmission {

	private static final Logger logger = Logger.getLogger(CancelAcquisition.class);
	@Inject
	private UpdateAcctBalance updateAcct;
	@Inject
	private UIMCECService uimcec;
	public PaymentDetails cancel(Session sess,IsoAutorisation auto, Transaction trx,String reason) {
		PaymentDetails resp;
		try {
			if (Constantes.ISO_SUCCESS_STATUS.equals(auto.getField39())) {
				resp = cancelByBIN(auto,trx);
				if(ErrorResponse.REPONSE_SUCCESS.getCode().equals(resp.getCode())) {
					updateAcct.updateAccountBalance(sess,trx);
					auto.setField39(Constantes.ISO_CANCELED_STATUS);
					trx.setStatus(BEConstantes.STATUS_TRANSACTION_CANCELED);
					trx.setPaymentDate(new Date());
					trx.setReasonForCancellation(reason);
					sess.updateObject(trx);
					sess.updateObject(auto);
					resp.setAmount(new BigDecimal(auto.getField4()));
					resp.setTransactionId(trx.getId().toString());
					resp.setMerchantName(auto.getField43());
				}
			
		} else {
			resp = new PaymentDetails(ErrorResponse.UNAUTHORIZED_CANCEL, "");
		}
		}
		catch (Exception e) {
			logger.error("cancelEmissison", e);
			return new PaymentDetails(ErrorResponse.UNKNOWN_ERROR, "");
		}
		return resp;
	}
	
	private PaymentDetails cancelByBIN(IsoAutorisation auto,Transaction trx) {
		
		PaymentDetails respDetails = new PaymentDetails(ErrorResponse.UNAUTHORIZED_CANCEL, "");
		if(auto.getField2().startsWith(BEConstantes.BIN_UIMCEC)) {
			UimcecRequest uimReq = new UimcecRequest();
			uimReq.setAcquerreurBanque(auto.getField41());
			uimReq.setAcquerreurCode(null);
			uimReq.setAcquerreurName(auto.getField43());
			uimReq.setCardCin(trx.getFromCard().getCin());
			uimReq.setAmount(trx.getPayinAmount());
			uimReq.setFees(trx.getPayinCommission());
			uimReq.setReference(trx.getInTrRefNumber());
			UimcecPayDto uiPay= uimcec.cancel(uimReq);
			if(Constantes.ISO_SUCCESS_STATUS.equals(uiPay.getCodeMessage())) {
				respDetails.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
				respDetails.setMessage("SUCCESS");
			}
			
		}
		return respDetails;
	}
}
