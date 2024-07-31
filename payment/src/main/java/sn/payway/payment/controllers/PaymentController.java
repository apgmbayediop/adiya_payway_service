package sn.payway.payment.controllers;

import java.util.Locale;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.RandomStringUtils;

import lombok.extern.jbosslog.JBossLog;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.aci.Caisse;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.dto.ListResponse;
import sn.payway.common.utils.Constantes;
import sn.payway.common.utils.PaywayException;
import sn.payway.payment.cancel.CancelTrxService;
import sn.payway.payment.dto.PaymentDetails;
import sn.payway.payment.dto.PaymentDto;
import sn.payway.payment.dto.PaymentMeans;
import sn.payway.payment.services.OnlinePaymentService;
import sn.payway.payment.services.PaymentSchoolService;
import sn.payway.payment.services.WalletPayService;

@Path("/")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@JBossLog
public class PaymentController {

	@Inject
	private OnlinePaymentService  onlinePay;
	@Inject
	private PaymentSchoolService schoolService;
	@Inject
	private WalletPayService wltPay;
	@Inject
	private CancelTrxService cancelTrx;
	
	@POST
	@Path("details")
	public PaymentDetails getPaymentDetails(PaymentDto dto)	{
		
		return onlinePay.autorizeServer(dto)? onlinePay.details(dto): new PaymentDetails(ErrorResponse.UNAUTHORIZED_PAYMENT.getCode(),
				"operation non autorise");
	}
	@DELETE
	public PaymentDetails delete(@QueryParam("transactionId") Long transactionId,
			@QueryParam("reason") String reason)	{
		
		return cancelTrx.cancel(transactionId,reason);
	}
	@GET
	@Path("bank-payer")
	public PaymentDetails getBank(@QueryParam("meansType") String meansType,@QueryParam("subMeansType") String subMeansType,
			@QueryParam("transactionId") String tansactionId)	{
		
		PaymentDto dto = new PaymentDto();
		dto.setMeansType(meansType);
		dto.setSubMeansType(subMeansType);
		dto.setTransactionId(tansactionId);
		return onlinePay.getBank(dto);	
	
	}
	@GET
	@Path("school-bill")
	public PaymentDetails schoolBill(@QueryParam("idStudent") String idStudent,
			@QueryParam("idMerchant") Long idMerchant,
			@QueryParam("transactionId") String transactionId,
			@QueryParam("auth") String auth)	{
		
		PaymentDto dto = new PaymentDto();
		dto.setTimestamp(transactionId);
		dto.setAuth(auth);
		dto.setTransactionId(transactionId);
		return onlinePay.autorizeServer(dto)? schoolService.findPendingTrxByStudent(idMerchant, idStudent.toUpperCase(Locale.FRENCH)): new PaymentDetails(ErrorResponse.UNAUTHORIZED_PAYMENT.getCode(),
				"operation non autorise");
	}
	@GET
	@Path("paymentMeans")
	public ListResponse<PaymentMeans> listPaymentMeans(@QueryParam("meansType") String meansType,
			@HeaderParam(HttpHeaders.AUTHORIZATION)String authorization
			,@QueryParam("requestId")String requestId
			,@QueryParam("terminalNumber")String terminalNumber)	{
		PaymentDto dto = new PaymentDto();
		dto.setMeansType(meansType);
		dto.setTerminalNumber(terminalNumber);
		dto.setRequestId(requestId);
		dto.setAuth(authorization);
			return onlinePay.paymentMeans(dto);
	}
	@POST
	@Path("by-partner")
	public PaymentDetails byPartner(PaymentDto dto)	{
		PaymentDetails resp;
		try {
		Session sess = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		
		Caisse caisse = sess.executeNamedQuerySingle(Caisse.class, "Caisse.findByTerminal", new String[] {"numeroSerie"}, new String[] {dto.getTerminalSN()});
		dto.setRequestId(RandomStringUtils.randomAlphanumeric(15).toLowerCase(Locale.FRENCH));
		dto.setCurrencyName(caisse.getPointDeVente().getCommercant().getCurrencyName());
		dto.setTerminalNumber(caisse.getNumeroCaisse());
		dto.setMerchantAddress(caisse.getPointDeVente().getCommercant().getName());
		
		resp = wltPay.directPayment(caisse, dto);
		}
		catch (PaywayException e) {
			resp = new PaymentDetails(e.getCode(), e.getMessage());
		}
		return resp;
	}
	@GET
	@Path("by-partner")
	public PaymentDetails statusPayment(@HeaderParam(Constantes.FLASHCODE)String flashcode, @QueryParam("transactionId") Long transactionId){
		
		PaymentDetails response;
		try {
			response =  onlinePay.statusPayment(transactionId);
			
		}
		catch (Exception e) {
			log.error("getByPartner",e);
			response = new PaymentDetails(ErrorResponse.UNKNOWN_ERROR.getCode(),
					ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		return response;
	}
}
