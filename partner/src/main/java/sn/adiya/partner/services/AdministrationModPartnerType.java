package sn.adiya.partner.services;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.exception.TransactionException;
import sn.fig.common.utils.APGCommonRequest;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.PartnerType;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.utils.Constantes;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationModPartnerType {
	public Response Service(String flashcode,APGCommonRequest apgPartnerTypeRequest){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			System.out.println("####### Start Edit PartnerType from APG >>>>>>>");
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, "AdministrationModPartnerType", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);

			Long idPartnerType = apgPartnerTypeRequest.getId();
			String pType = apgPartnerTypeRequest.getPartnerType();
			utilVue.CommonLabel(null,"AdministrationModPartnerType",ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("pType : "), pType);
			PartnerType partnerType = (PartnerType) session.findObjectById(PartnerType.class, idPartnerType, null);
			if(partnerType == null){
				return Response.ok().entity(new AbstractResponse("1", "PartnerType "+Constantes.NF_TRYAGAIN))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}else{
				partnerType.setType(pType);
				session.updateObject(partnerType);

				List<APGPartnerType> lpts = new ArrayList<APGPartnerType>();
				APGPartnerType pt = new APGPartnerType();
				pt.setId(partnerType.getIdPartnerType());
				pt.setPartnerType(partnerType.getType());

				lpts.add(pt);

				System.out.println("####### End Edit PartnerType from APG >>>>>>>");
				return Response.ok().entity(new APGPartnerTypeResponse("0, OK",lpts))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();	
			}
		} catch (TransactionException e) {
			return Response.ok().entity(new AbstractResponse(e.getCode(), e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();	
		}
	}

}
