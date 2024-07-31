package sn.payway.payment.emv;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import sn.apiapg.common.utils.AbstractResponse;
import sn.payway.payment.dto.PaymentDetails;
import sn.payway.payment.dto.PaymentDto;

@Path("/emv")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class EMVCtl {


	@Inject
	private EMVService emvService;
	@Inject
	private AciConfig terminalConfig;
	
	@POST
	public PaymentDetails payment(@Valid PaymentDto dto) {
		
		return emvService.payment(dto);
	}
	@GET
	@Path("config")
	public AbstractResponse terminal(@QueryParam("terminalSN")String terminalSN) {
		
		return terminalConfig.configTerminal(terminalSN);
	}
}
