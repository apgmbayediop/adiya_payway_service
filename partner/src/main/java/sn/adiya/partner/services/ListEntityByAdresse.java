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
import sn.fig.entities.Partner;
import sn.fig.entities.PartnerUtilisateur;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * Moussa SENE
 * Cherif DIOUF
 * @version 1.0
 **/

@Stateless
public class ListEntityByAdresse {
	static final String TAG =  ListEntityByAdresse.class+"";
	public Response Service(String flashcode, APGCommonRequest apgValidateRequest){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);
			String adresse = apgValidateRequest.getAdresse();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("adresse : "), adresse);
			String[] parameters = {"adresse","pType"};
			Object[] data  = {"%"+adresse.toUpperCase()+"%",BEConstantes.PARTNER_CAISSE};
			List<APGPartner> lPtns = new ArrayList<APGPartner>();
			List<Partner> lPartner = session.executeNamedQueryList(Partner.class, "findEntityByAdresse", parameters, data);
			for (Partner partner : lPartner) {
				APGPartner ptn = getPartnerResponse(partner);
				if(Boolean.TRUE.equals(ptn.getIsAssigned()))
					lPtns.add(getPartnerResponse(partner));
			}
			return Response.ok().entity(new APGPartnerResponse("0", "OK",lPtns ))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();			
		} catch (TransactionException e) { 
			return Response.ok().entity(new AbstractResponse("1", e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();	
		}
	}


	private static APGPartner getPartnerResponse(Partner p) {
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		APGPartner ptn = new APGPartner();
		ptn.setId(p.getIdPartner());
		ptn.setName(p.getName());
		ptn.setTelephonePartner(p.getTelephonePartner());
		ptn.setTelephoneContact(p.getTelephoneContact());
		ptn.setEmail(p.getEmailContact());
		ptn.setCurrencyName(p.getCurrencyName());
		ptn.setCountryIsoCode(p.getCountryIsoCode());
		ptn.setNomContact(p.getNomContact());
		ptn.setPrenomContact(p.getPrenomContact());
		ptn.setPartnerType(p.getPType());
		ptn.setType(p.getFilsDistributeur());
		ptn.setAdresse(p.getAdresse());
		String param[] = {"idPartner"};
		Object data3[] = {p.getIdPartner()};
		PartnerUtilisateur pu = session.executeNamedQuerySingle(PartnerUtilisateur.class, "findUtilisateurByEntite", param, data3);
		if(pu != null && pu.getIsValidated() != null && pu.getIsValidated()) {
			ptn.setIsAssigned(true);
			ptn.setAssignedUserId(pu.getUtilisateur().getIdUtilisateur());
			ptn.setAssignedUserFirstName(pu.getUtilisateur().getPrenom());
			ptn.setAssignedUserLastName(pu.getUtilisateur().getNom());
			ptn.setAssignedUserPhone(pu.getUtilisateur().getPhone());
		}else
			ptn.setIsAssigned(false);
		ptn.setPartnerCode(p.getCode());
		ptn.setIsValidated(p.getIsValidated());
		ptn.setIsOpened(p.getIsOpened());
		ptn.setPlafondTrx(p.getPlafondTrx()+"");
		ptn.setVolumeTrx(p.getVolumeTrx()+"");
		ptn.setRegion(p.getRegionName());
		ptn.setCodeAgence(p.getCodeAgence());

		return ptn;
	}

}
