package sn.payway.partner.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.exception.TransactionException;
import sn.apiapg.common.utils.APGCommonRequest;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.Partner;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationModEntity {
	public Response Service(String flashcode,APGCommonRequest apgPartnerRequest) {
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			System.out.println("####### Debut Mod Entity from APG >>>>>>>");
			APGPartnerResponse response; 

			Utilisateur user = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				user = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, "AdministrationModEntity", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur : "), user);

			String partnerId = apgPartnerRequest.getPartnerId();
			utilVue.CommonLabel(null,"AdministrationModEntity",ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("partnerId : "), partnerId);
			Partner PARTNER = (Partner) session.findObjectById(Partner.class, Long.parseLong(partnerId), null);
			utilVue.CommonObject(null, "AdministrationModEntity", ErrorResponse.AUTHENTICATION_ERRORS_1707.getCode(), ErrorResponse.AUTHENTICATION_ERRORS_1707.getMessage("Partner"), PARTNER);
			if(apgPartnerRequest.getPlafondTrx() != null && !apgPartnerRequest.getPlafondTrx().equals("")){
				PARTNER.setPlafondTrx(new BigDecimal(apgPartnerRequest.getPlafondTrx()));
			}
			if(apgPartnerRequest.getVolumeTrx() != null && !apgPartnerRequest.getVolumeTrx().equals("")){
				PARTNER.setVolumeTrx(new BigDecimal(apgPartnerRequest.getVolumeTrx()));
			}
			if(apgPartnerRequest.getIsValidated() != null){
				PARTNER.setIsValidated(apgPartnerRequest.getIsValidated());
			}

			session.updateObject(PARTNER);

			List<APGPartner> lPtns = new ArrayList<APGPartner>();
			APGPartner ptn = new APGPartner();
			ptn.setId(PARTNER.getIdPartner());
			ptn.setName(PARTNER.getName());
			ptn.setTelephonePartner(PARTNER.getTelephonePartner());
			ptn.setTelephoneContact(PARTNER.getTelephoneContact());
			ptn.setEmail(PARTNER.getEmailContact());
			ptn.setCurrencyName(PARTNER.getCurrencyName());
			ptn.setCountryIsoCode(PARTNER.getCountryIsoCode());
			ptn.setNomContact(PARTNER.getNomContact());
			ptn.setPrenomContact(PARTNER.getPrenomContact());
			ptn.setIsValidated(PARTNER.getIsValidated());
			ptn.setPartnerType(PARTNER.getPType());

			lPtns.add(ptn);

			System.out.println("####### Fin Mod Entity from APG >>>>>>>");
			response = new APGPartnerResponse("0", "OK",lPtns);
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
