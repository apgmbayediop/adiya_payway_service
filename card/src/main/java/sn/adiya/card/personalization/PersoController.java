package sn.adiya.card.personalization;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import sn.adiya.card.exception.CardException;
import sn.adiya.common.utils.AdiyaException;
import sn.adiya.common.utils.Constantes;
import sn.adiya.user.services.UserManager;
import sn.fig.common.entities.Utilisateur;
import sn.fig.common.utils.AbstractResponse;

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
			
		} catch (AdiyaException e) {
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
