package sn.adiya.payment.cancel;

import java.math.BigDecimal;
import java.util.Date;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.entities.Transaction;
import sn.fig.entities.aci.IsoAcquisition;
import sn.fig.session.Session;
import sn.adiya.common.utils.Constantes;
import sn.adiya.partner.orangemoney.service.OrangeMoneyService;
import sn.adiya.payment.dto.PaymentDetails;

@Stateless
public class CancelAcquisition {

	private static final String APG_ACQ_CODE = "000000";
	private static final Logger logger = Logger.getLogger(CancelAcquisition.class);
	
	@Inject
	private UpdateAcctBalance updateAcct;

	public PaymentDetails cancel(Session sess,IsoAcquisition acqui, Transaction trx,String reason) {
		PaymentDetails resp;
		try {

			logger.info("cancelPayment");

			if (acqui == null) {
				resp = new PaymentDetails(ErrorResponse.TRANSACTION_NOT_FOUND, "");
			} else if (Constantes.ISO_SUCCESS_STATUS.equals(acqui.getField39())) {
				resp = cancelByAcceptor(acqui);
				if(ErrorResponse.REPONSE_SUCCESS.getCode().equals(resp.getCode())) {
					updateAcct.updateAccountBalance(sess,trx);
					acqui.setField39(Constantes.ISO_CANCELED_STATUS);
					trx.setStatus(BEConstantes.STATUS_TRANSACTION_CANCELED);
					trx.setPaymentDate(new Date());
					trx.setReasonForCancellation(reason);
					sess.updateObject(trx);
					sess.updateObject(acqui);
					resp.setAmount(new BigDecimal(acqui.getField4()));
					resp.setFees(acqui.getFees());
					resp.setTransactionId(trx.getId().toString());
					resp.setMerchantName(acqui.getCaisse().getPointDeVente().getCommercant().getName());
				}
			} else
				resp = new PaymentDetails(ErrorResponse.UNAUTHORIZED_CANCEL, "");

		} catch (Exception e) {
			logger.error("cancelAcquiErr", e);
			return new PaymentDetails(ErrorResponse.UNKNOWN_ERROR, "");
		}
		return resp;
	}

	private PaymentDetails cancelByAcceptor(IsoAcquisition acqui) {

		PaymentDetails resp;
		switch (acqui.getField33()) {
		case OrangeMoneyService.CODE:
		case Constantes.CODE_RESTAU:
		case APG_ACQ_CODE:
			resp =  new PaymentDetails(ErrorResponse.REPONSE_SUCCESS, "");
			break;

		default:
			resp = new PaymentDetails();
			resp.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			resp.setMessage("Annulation acquerreur non prise en charge");
			break;
		}
		return resp;
	}
	
}
