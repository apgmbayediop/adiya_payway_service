package sn.adiya.payment.controllers;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import sn.fig.common.entities.Utilisateur;
import sn.adiya.common.utils.Constantes;
import sn.adiya.common.utils.AdiyaException;
import sn.adiya.payment.dto.PaymentDetails;
import sn.adiya.payment.dto.PaymentDto;
import sn.adiya.payment.services.CashierMerchantService;
import sn.adiya.payment.services.OnlinePaymentService;
import sn.adiya.user.services.UserManager;

@Path("/cashier")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class CashierController {

	@Inject
	private CashierMerchantService cashier;
	@Inject
	private UserManager userManager;
	@Inject
	private OnlinePaymentService onlinePay;
	
	@POST
	@Path("pay")
	public PaymentDetails cashierPayment(@HeaderParam(Constantes.FLASHCODE)String flashcode, PaymentDto dto){
		
		PaymentDetails response;
		try {
			Utilisateur user = userManager.verifyFlashcode(flashcode);
			response =  cashier.payment(user,dto);
			response.setBank(dto.getBank());
			response.setPhone(dto.getPhone());
		}
		catch (AdiyaException e) {
			response = new PaymentDetails();
			response.setCode(e.getCode());
			response.setMessage(e.getMessage());
		}
		return response;
	}
	@GET
	@Path("pay")
	public PaymentDetails statusPayment(@HeaderParam(Constantes.FLASHCODE)String flashcode, @QueryParam("transactionId") Long transactionId){
		
		PaymentDetails response;
		try {
			userManager.verifyFlashcode(flashcode);
			response =  onlinePay.statusPayment(transactionId);
			
		}
		catch (AdiyaException e) {
			response = new PaymentDetails();
			response.setCode(e.getCode());
			response.setMessage(e.getMessage());
		}
		return response;
	}
}
