package sn.payway.partner.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.PartnerType;
import sn.apiapg.session.OperationSession;
import sn.apiapg.session.OperationSessionBean;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/
@Stateless
public class AdministrationListPartnerInOut {
	public Response Service(String flashcode){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			System.out.println("####### Debut List Partner IN OUT from APG >>>>>>>");
			APGPartnerResponse response; 
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, "AdministrationListPartnerInOut", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);
			
			String[] parameters = {"type"};
			Object[] data = {BEConstantes.PARTNER_SENDER_PAYER};
			List<Partner> lPartner = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", parameters, data);
			List<APGPartner> lpts = new ArrayList<APGPartner>();
			for(Partner partner : lPartner){
				APGPartner pt = new APGPartner();
				pt.setId(partner.getIdPartner());
				pt.setConsumerId(partner.getConsumerId());
				pt.setName(partner.getName());
				pt.setTelephonePartner(partner.getTelephonePartner());
				pt.setTelephoneContact(partner.getTelephoneContact());
				pt.setEmail(partner.getEmailContact());
				pt.setNomContact(partner.getNomContact());
				pt.setPrenomContact(partner.getPrenomContact());
				pt.setCurrencyName(partner.getCurrencyName());
				
				System.out.println("partner.getCode() "+partner.getCode()+" partner.getPrincipalAccountNumber() "+partner.getPrincipalAccountNumber());
				
				BigDecimal mainBalance = operationSession.balancePartner(partner);
				pt.setBalance(mainBalance+"");

				PartnerType pType = partner.getPartnerType();
				pt.setPartnerType(pType.getIdPartnerType()+"");
				pt.setPartnerTypeId(pType.getType());
		//		pt.setPrincipalAccountNumber(partner.getPrincipalAccountNumber());

				lpts.add(pt);
			}
			System.out.println("####### Fin List all Partner IN & OUT from APG >>>>>>>");
			response = new APGPartnerResponse("0", "OK",lpts);

			return Response.ok().entity(response)
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.build();
		} catch (Exception e) {
			return Response.ok().entity(new AbstractResponse("1", e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();
		}	
	}

}
