package sn.payway.card.personalization;

import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.utils.AbstractResponse;
import sn.payway.card.exception.CardException;
import sn.payway.common.utils.Constantes;
import sn.payway.common.utils.PaywayException;
import sn.payway.user.services.UserManager;

@Path("/personalization")
public class PersoController {

	private static final Logger LOG = Logger.getLogger(PersoController.class);
	@Inject
	private UserManager userManager;
	@Inject
	private PersoRestaurant persoResto;
	@POST
	@Path("/restaurant")
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Produces({MediaType.APPLICATION_JSON})
	public AbstractResponse personalize(@HeaderParam(Constantes.FLASHCODE) String flashcode,MultipartFormDataInput form) 
	{
		AbstractResponse response;
		try {
			LOG.info(flashcode);
			Utilisateur user = userManager.verifyFlashcode(flashcode);
			 response=persoResto.personalize(user,form);
			
		} catch (PaywayException e) {
			LOG.error("error",e);
			response =new AbstractResponse();
			response.setCode(e.getCode());
			response.setMessage(e.getMessage());
		} catch (CardException e) {
			response =new AbstractResponse();
			response.setCode(e.getCode());
			response.setMessage(e.getMessage());
		}
		return response;
	}
}
