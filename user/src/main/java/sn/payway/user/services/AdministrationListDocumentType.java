package sn.payway.user.services;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.exception.TransactionException;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.BEConstantes;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationListDocumentType {
	final static Logger LOG = Logger.getLogger(AdministrationListDocumentType.class);
	public Response Service(String flashcode){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			LOG.info("### --- --- - Start List all Document from APG APG >>>>>>>");
			Utilisateur user = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				user = (Utilisateur) globalResponse.getEntity();
			}

			ObjectNode jo = new ObjectMapper().createObjectNode(); 
			if(user.getPartner().getLogo() != null && user.getPartner().getLogo().equals("logo-reliance")) {
				jo.put("code", ErrorResponse.REPONSE_SUCCESS.getCode());
				jo.put("message", ErrorResponse.REPONSE_SUCCESS.getMessage(""));

				ArrayNode ja = new ObjectMapper().createArrayNode(); 
				ObjectNode jo0 = new ObjectMapper().createObjectNode();
				jo0.put("type", "1");
				jo0.put("value", "National ID card");
				ja.add(jo0);

				ObjectNode jo1 = new ObjectMapper().createObjectNode();
				jo1.put("type", "2");
				jo1.put("value", "passport");
				ja.add(jo1);

				ObjectNode jo2 = new ObjectMapper().createObjectNode();
				jo2.put("type", "3");
				jo2.put("value", "Voter’s card");
				ja.add(jo2);

				ObjectNode jo3 = new ObjectMapper().createObjectNode();
				jo3.put("type", "4");
				jo3.put("value", "Drive lisence");
				ja.add(jo3);

				ObjectNode jo4 =  new ObjectMapper().createObjectNode();
				jo4.put("type", "5");
				jo4.put("value", "Resident permit");
				ja.add(jo4);
				
				ObjectNode jo5 = new ObjectMapper().createObjectNode();
				jo5.put("type", "6");
				jo5.put("value", "ECOWAS Id Card");
				ja.add(jo5);
				
				jo.set("relianceDocument", ja);

			}else {
				jo.put("code", ErrorResponse.REPONSE_SUCCESS.getCode());
				jo.put("message", ErrorResponse.REPONSE_SUCCESS.getMessage(""));

				ArrayNode ja = new ObjectMapper().createArrayNode(); 
				ObjectNode jo0 = new ObjectMapper().createObjectNode();
				jo0.put("type", "1");
				jo0.put("value", "cni");
				ja.add(jo0);

				ObjectNode jo1 = new ObjectMapper().createObjectNode();
				jo1.put("type", "2");
				jo1.put("value", "passport");
				ja.add(jo1);

				ObjectNode jo2 = new ObjectMapper().createObjectNode();
				jo2.put("type", "3");
				jo2.put("value", "permis de conduire");
				ja.add(jo2);

				ObjectNode jo3 = new ObjectMapper().createObjectNode();
				jo3.put("type", "4");
				jo3.put("value", "carte consulaire");
				ja.add(jo3);

				ObjectNode jo4 = new ObjectMapper().createObjectNode();
				jo4.put("type", "5");
				jo4.put("value", "carte de vote");
				ja.add(jo4);
				
				jo.set("apgDocument", ja);
			}
			LOG.info("### --- --- - Fin List all Document from APG >>>>>>>");
			return Response.ok().entity(jo.toString())
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.build();

		} catch (TransactionException e) {
			return Response.ok().entity(new AbstractResponse(e.getCode(), e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();
		}	
	}

}
