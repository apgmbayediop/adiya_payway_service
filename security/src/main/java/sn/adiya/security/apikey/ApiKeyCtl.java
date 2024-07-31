package sn.adiya.security.apikey;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import lombok.extern.jbosslog.JBossLog;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.AbstractResponse;

@Path("apikey")
@JBossLog
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class ApiKeyCtl {

	public static final String APIKEY="api_key";
	@Inject
	ApiKeyService keyService;
	
	@POST
	public AbstractResponse generate(@QueryParam("idPartner") Long idPartner) {
		log.info("apiKeyCtl: generate");
		AbstractResponse response = new AbstractResponse();
		try {
			String key = keyService.generate(idPartner);
			response.setMessage(key);
			response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
		} catch (Exception e) {
			response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			response.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		return response;
		
	}
}
