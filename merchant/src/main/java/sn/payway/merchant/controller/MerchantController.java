package sn.payway.merchant.controller;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.aci.MerchantCategorie;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.dto.ListResponse;
import sn.payway.merchant.dto.PosDto;
import sn.payway.merchant.services.PosService;

@Path("/merchants")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class MerchantController {

	@Inject
	private PosService posService;
	
	@GET
	public ListResponse<PosDto> listMerchant(@QueryParam(value= "mcc") String mcc) {
		
		List<PosDto> poss = posService.listMerchantByMcc(mcc);
		ListResponse<PosDto> response = new ListResponse<>();
		response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
		response.setData(poss);
		return response;
	}
	@GET
	@Path("pos")
	public ListResponse<PosDto> listPos(@QueryParam(value ="idMerchant") Long idMerchant,
			@QueryParam(value= "mcc") String mcc) {
		
		List<PosDto> poss = posService.listPosByMcc(mcc);
		ListResponse<PosDto> response = new ListResponse<>();
		response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
		response.setData(poss);
		return response;
	}
	@GET
	@Path("mcc")
	public ListResponse<MerchantCategorie> listMcc() {
		
		Session sess = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		List<MerchantCategorie> poss = sess.findAllObject(MerchantCategorie.class);
		ListResponse<MerchantCategorie> response = new ListResponse<>();
		response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
		response.setData(poss);
		return response;
	}
}
