package sn.adiya.partner.services;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.exception.TransactionException;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.PartnerType;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationListPartnerType {
	public Response Service(String flashcode){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			System.out.println("####### Debut List Partner Type from APG >>>>>>>");
			APGPartnerTypeResponse response; 
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, "AdministrationListPartnerType", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);
			List<PartnerType> lPartnerTypes = (List<PartnerType>) session.findAllObject(PartnerType.class);
			List<APGPartnerType> lpts = new ArrayList<APGPartnerType>();
			String messages = ""; 
			for(PartnerType partnerType : lPartnerTypes){
				APGPartnerType pt = new APGPartnerType();
				pt.setId(partnerType.getIdPartnerType());
				pt.setPartnerType(partnerType.getType());
				if(!messages.equals("") && messages.length() > 2){
					pt.setMessage( messages.substring(0, messages.length()-2));
				}
				lpts.add(pt);
			}
			System.out.println("####### Fin List all Partner Type from APG >>>>>>>");
			response = new APGPartnerTypeResponse("0",lpts);
			return Response.ok().entity(response)
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
