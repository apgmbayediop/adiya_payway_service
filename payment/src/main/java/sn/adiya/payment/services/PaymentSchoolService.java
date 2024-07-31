package sn.adiya.payment.services;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import lombok.extern.jbosslog.JBossLog;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.BulkPaymentFile;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.utils.Constantes;
import sn.adiya.payment.dto.PaymentDetails;
import sn.adiya.payment.dto.Person;

@Stateless
@JBossLog
public class PaymentSchoolService {

	
	public PaymentDetails findPendingTrxByStudent(Long idMerchant,String idStudent) {
		
		PaymentDetails response  = new PaymentDetails();
		try {
		Session sess = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		EntityManager em = sess.getManager();
		String queryStr = "select b from BulkPaymentFile b  JOIN b.transactionId t where b.partner.idPartner = :idMerchant ";
		queryStr = queryStr+"and upper(b.beneficiaryId) = :idStudent and b.status = :status and t.field39=:statusPay order by b.date desc";
		TypedQuery<BulkPaymentFile> query = em.createQuery(queryStr,BulkPaymentFile.class);
		query.setParameter("idMerchant", idMerchant);
		query.setParameter("idStudent", idStudent);
		query.setParameter("status",BEConstantes.STATUS_TRANSACTION_PENDING );
		query.setParameter("statusPay",Constantes.ISO_PENDING_STATUS );
		List<BulkPaymentFile> lines = query.getResultList();
		BulkPaymentFile line = lines.isEmpty()?null:lines.get(0);
		if(line == null) {
			response.setCode(ErrorResponse.CARD_HOLDER_NOT_FOUND.getCode());
			response.setMessage("Il ny a pas de paiement en attente. Veuillez si les informations saisies sont correctes");
		}else {
			response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
			response.setAmount(line.getAmount());
			response.setMerchantName(line.getPartner().getName());
			response.setMerchantNumber(idMerchant.toString());
			response.setMeansType(line.getTransactionId().getPaymentMeansType());
			response.setTransactionType(line.getTransactionId().getChannelType());
			Person ben = new Person();
			ben.setFirstName(line.getBeneficiaryName());
			ben.setDocumentNumber(line.getBeneficiaryId());
			response.setBeneficiary(ben);
			response.setTransactionId(line.getTransactionId().getId().toString());
			response.setRequestId(line.getTransactionId().getField63());
			
		}
		}
		catch (Exception e) {
			log.error("findLineError",e);
			response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			response.setMessage("Service mommentanement indisponible. Veuillez reessayer plus tard");
		}
		return response;
	}
}
