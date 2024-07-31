package sn.payway.payment.cancel;

import javax.ejb.Stateless;
import javax.inject.Inject;

import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.IsoAutorisation;
import sn.apiapg.entities.Transaction;
import sn.apiapg.entities.aci.IsoAcquisition;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.payment.dto.PaymentDetails;

@Stateless
public class CancelTrxService {

	@Inject
	private CancelAcquisition cancelAcqui;
	@Inject
	private CancelEmission cancelEmiss;
	public PaymentDetails cancel(Long trxId,String reason) {
		
		PaymentDetails resp;
		try {
			Session sess =(Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
			Transaction trx = sess.findObjectById(Transaction.class, trxId, null);
			if(BEConstantes.STATUS_TRANSACTION_VALIDATED.equals(trx.getStatus())||
					BEConstantes.STATUS_TRANSACTION_PAYED.equals(trx.getStatus())) {
			String params[]= {"field52"};
			/*if(ChannelResponse.CARD2ATM.equals(trx.getChannelType())) {
				IsoAutorisation aut = sess.executeNamedQuerySingle(IsoAutorisation.class, "IsoAutorisation.findByTransactionId",
						params, new String[] {trxId.toString()});
			}*/
			IsoAcquisition acqui = sess.executeNamedQuerySingle(IsoAcquisition.class, "IsoAcquisition.findByTransactionId",
					params, new String[] {trxId.toString()});
			if(acqui == null) {
			  resp = cancelAcqui.cancel(sess,acqui,trx,reason);
			 }else {
				 IsoAutorisation auto=sess.executeNamedQuerySingle(IsoAutorisation.class, "IsoAutorisation.findByTransactionId",
							params, new String[] {trxId.toString()});
				 resp = auto==null?new PaymentDetails(ErrorResponse.UNAUTHORIZED_CANCEL, ""):cancelEmiss.cancel(sess, auto, trx, reason);
			 }
			}else {
				resp = new PaymentDetails(ErrorResponse.UNAUTHORIZED_CANCEL, "");
			}
		} catch (Exception e) {
		   resp = new PaymentDetails();
		   resp.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
		   resp.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		return resp;
	}
}
