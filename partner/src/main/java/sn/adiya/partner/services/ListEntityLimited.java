package sn.adiya.partner.services;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.APGCommonRequest;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.Partner;
import sn.fig.session.AdministrationSession;
import sn.fig.session.AdministrationSessionBean;
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
public class ListEntityLimited {
	
	final static String TAG = ListEntityLimited.class + "";
	public final static Logger LOG = Logger.getLogger(ListEntityLimited.class);
	
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
			Long id = apgValidateRequest.getId();
			utilVue.CommonObject(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("parent : "), id);
			Boolean isValidated = apgValidateRequest.getIsValidated();
			String partnerType = apgValidateRequest.getPartnerType();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("partnerType : "), partnerType);
			Partner parent = session.findObjectById(Partner.class, id, null);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Parent : "), parent);
			int indice = 0; 
			int pas = 99; 
			if(apgValidateRequest.getIndice() != null && apgValidateRequest.getPas() != null) {
				try {
					indice = Integer.parseInt(apgValidateRequest.getIndice());
				} catch (Exception e) {
					indice = 0;
				}
				try {
					pas = Integer.parseInt(apgValidateRequest.getPas());
				} catch (Exception e) {
					pas = 99;
				}
			}
			return Response.ok().entity(getListEntityByParenandType(partnerType, parent, isValidated,indice, pas))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();			
		} catch (Exception e) {
			return Response.ok().entity(new AbstractResponse("1", e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();	
		}
	}

	private static APGPartnerResponse getListEntityByParenandType(String partnerType, Partner parent, Boolean isValidated,int indice, int pas) throws Exception {
		AdministrationSession administrationSession = (AdministrationSession) BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());

		List<APGPartner> lPtns = new ArrayList<>();

	/*	String[] parameters = {"type", "idPartner", "isValidated"};
		Object[] datas = {partnerType, parent.getIdPartner(), isValidated};
		List<Partner> lPChilds = session.executeNamedQueryList(Partner.class,"findEntityChild", parameters, datas);	*/
		List<Partner> lPChilds = administrationSession.findEntityChild(partnerType, parent.getIdPartner(), isValidated, indice, pas);

		if(!lPChilds.isEmpty()){
			for(Partner p: lPChilds){
				lPtns.add(getPartnerResponse(p));
			}
			lPtns.add(getPartnerResponse(parent));
		}else{
			lPtns.add(getPartnerResponse(parent));
		}
		return new APGPartnerResponse("0", "OK",lPtns);
	}

	private static APGPartner getPartnerResponse(Partner p) {
		APGPartner ptn = new APGPartner();
		ptn.setId(p.getIdPartner());
		ptn.setName(p.getName());
		ptn.setPartnerType(p.getPType());

		return ptn;
	}

}
