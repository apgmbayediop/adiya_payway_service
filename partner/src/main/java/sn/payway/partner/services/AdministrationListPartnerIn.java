package sn.payway.partner.services;

import java.io.UnsupportedEncodingException;
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
public class AdministrationListPartnerIn {
	public Response Service(String flashcode) throws UnsupportedEncodingException{
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			System.out.println("####### Debut List Partner IN from APG >>>>>>> flashcode : *** * *** ");
			APGPartnerResponse response; 
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, "AdministrationListPartnerIn", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);

			String[] parameters = {"type"};
			Object[] data = {BEConstantes.PARTNER_SENDER};
			List<Partner> lPartnerIN = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", parameters, data);
			Object[] datas = {BEConstantes.PARTNER_SENDER_PAYER};
			List<Partner> lPartnerIN_OUT = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", parameters, datas);
			List<APGPartner> lpts = new ArrayList<APGPartner>();
			List<Partner> lPartner = new ArrayList<Partner>();
			if(!lPartnerIN.isEmpty() && lPartnerIN.size() >0) lPartner.addAll(lPartnerIN);
			if(!lPartnerIN_OUT.isEmpty() && lPartnerIN_OUT.size() >0) lPartner.addAll(lPartnerIN_OUT);
			Partner payway = session.findObjectById(Partner.class, 17181L, null);
			if(payway != null)
				lPartner.add(payway);
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

				if(partner.getPType().equals(BEConstantes.PARTNER_SENDER)) {
					BigDecimal mainBalance = operationSession.balancePartner(partner);
					pt.setBalance(mainBalance+"");
				}else if(partner.getPType().equals(BEConstantes.PARTNER_SENDER_PAYER)) {
					BigDecimal mainBalance = operationSession.balancePartner(partner);
					pt.setBalance(mainBalance+"");
				}
				PartnerType pType = partner.getPartnerType();
				pt.setPartnerType(pType.getIdPartnerType()+"");
				pt.setPartnerTypeId(pType.getType());
		//		pt.setPrincipalAccountNumber(partner.getPrincipalAccountNumber());

				lpts.add(pt);
			}
			System.out.println("####### Fin List all Partner IN from APG >>>>>>>");
			response = new APGPartnerResponse("0", "OK",lpts);

			return Response.ok().entity(response)
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.build();
		} 
		catch (Exception e) {
			return Response.ok().entity(new AbstractResponse("1", e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();
		}	
	}

}
