package sn.payway.partner.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.APGCommonRequest;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.PartnerUtilisateur;
import sn.apiapg.session.AdministrationSession;
import sn.apiapg.session.AdministrationSessionBean;
import sn.apiapg.session.OperationSession;
import sn.apiapg.session.OperationSessionBean;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * Moussa SENE
 * Cherif DIOUF
 * @version 1.0
 **/

@Stateless
public class ListEntity {
	
	final static String TAG = ListEntity.class.getName();
	static Logger LOG = Logger.getLogger(ListEntity.class);
	
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
			return Response.ok().entity(getListEntityByParenandType(partnerType, parent, isValidated,indice,pas))
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

	private static APGPartnerResponse getListEntityByParenandType(String partnerType, Partner parent, Boolean isValidated, int indice, int pas) throws Exception {
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());
		AdministrationSession administrationSession = (AdministrationSession) BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());

		List<APGPartner> lPtns = new ArrayList<APGPartner>();

//		String[] parameters = {"type", "idPartner", "isValidated"};
//		Object[] datas = {partnerType, parent.getIdPartner(), isValidated};
		List<Partner> lPChilds = administrationSession.findEntityChild(partnerType, parent.getIdPartner(), isValidated, indice, pas);

		if(!lPChilds.isEmpty()){
			BigDecimal balance = BigDecimal.ZERO;
			Double soldeAgenceEP = Double.valueOf(0);
			Double soldeCaisseAgenceEP = Double.valueOf(0);
			Double soldeDistributeurAgenceP = Double.valueOf(0);
			Double soldeSousDistributeurAgenceP = Double.valueOf(0);
			Double soldeCaisseAgenceP = Double.valueOf(0);
			Double soldeCaisseSousDistributeurP = Double.valueOf(0);
			Double soldeCaisseDistributeurP = Double.valueOf(0);

			for(Partner p: lPChilds){
				// Start Managed EP
				if(parent.getPType().equals(BEConstantes.PARTNER_SENDER_PAYER) && p.getPType().equals(BEConstantes.PARTNER_AGENCE)) {
					String[] params = {"type", "idPartner", "isValidated"};
					Object[] dats = {BEConstantes.PARTNER_CAISSE, p.getIdPartner(), true};
					List<Partner> lCaisseAgenceEPs = session.executeNamedQueryList(Partner.class,"findEntityChild", params, dats);
					if(!lCaisseAgenceEPs.isEmpty()){
						BigDecimal bChild = BigDecimal.ZERO;
						for(Partner caisseAgenceEP : lCaisseAgenceEPs){
							bChild = operationSession.balancePartner(caisseAgenceEP);
							if(bChild.compareTo(BigDecimal.ZERO) > 0)
								soldeAgenceEP = soldeAgenceEP + bChild.doubleValue();
						}
					}
				}
				if(parent.getPType().equals(BEConstantes.PARTNER_AGENCE) && parent.getParent().getPType().equals(BEConstantes.PARTNER_SENDER_PAYER) && p.getPType().equals(BEConstantes.PARTNER_CAISSE)) {
					BigDecimal bChild = BigDecimal.ZERO;
					bChild = operationSession.balancePartner(p);
					balance = bChild;
					if(bChild.compareTo(BigDecimal.ZERO) > 0)
						soldeCaisseAgenceEP = soldeCaisseAgenceEP + bChild.doubleValue();
				}
				// End Managed EP
				//### --- --- -
				// Start Managed Provider
				if(parent.getPType().equals(BEConstantes.PARTNER_PROVIDER)) {
					if(p.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR) || p.getPType().equals(BEConstantes.PARTNER_AGENCE)) {
						BigDecimal bChild = BigDecimal.ZERO; 
						bChild = operationSession.balancePartner(p);
						balance = bChild;
						if(bChild.compareTo(BigDecimal.ZERO) > 0)
							soldeDistributeurAgenceP = soldeDistributeurAgenceP + bChild.doubleValue();
					}
				}
				if(parent.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR)) {
					if(p.getPType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)) {
						BigDecimal bChild = BigDecimal.ZERO;
						bChild = operationSession.balancePartner(p);
						balance = bChild;
						if(bChild.compareTo(BigDecimal.ZERO) > 0)
							soldeSousDistributeurAgenceP = soldeSousDistributeurAgenceP + bChild.doubleValue();
					}
				}
				if(parent.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR)) {
					if(p.getPType().equals(BEConstantes.PARTNER_AGENCE)) {
						BigDecimal bChild = BigDecimal.ZERO;
						bChild = operationSession.balancePartner(p);
						balance = bChild;
						if(bChild.compareTo(BigDecimal.ZERO) > 0)
							soldeCaisseAgenceP = soldeCaisseAgenceP + bChild.doubleValue();
					}
				}
				if(parent.getPType().equals(BEConstantes.PARTNER_AGENCE)) {
					if(p.getPType().equals(BEConstantes.PARTNER_CAISSE)) {
						BigDecimal bChild = BigDecimal.ZERO;
						bChild = operationSession.balancePartner(p);
						balance = bChild;
						if(bChild.compareTo(BigDecimal.ZERO) > 0)
							soldeCaisseAgenceP = soldeCaisseAgenceP + bChild.doubleValue();
					}
				}
				if(parent.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR)) {
					if(p.getPType().equals(BEConstantes.PARTNER_CAISSE)) { 
						BigDecimal bChild = BigDecimal.ZERO;
						bChild = operationSession.balancePartner(p);
						balance = bChild;
						if(bChild.compareTo(BigDecimal.ZERO) > 0)
							soldeCaisseDistributeurP = soldeCaisseDistributeurP + bChild.doubleValue();
					}
				}
				if(parent.getPType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)) {
					if(p.getPType().equals(BEConstantes.PARTNER_CAISSE)) {
						BigDecimal bChild = BigDecimal.ZERO;
						bChild = operationSession.balancePartner(p);
						balance = bChild;
						if(bChild.compareTo(BigDecimal.ZERO) > 0)
							soldeCaisseSousDistributeurP = soldeCaisseSousDistributeurP + bChild.doubleValue();
					}
				}
				lPtns.addAll(getPartnerResponse(p, balance, lPChilds.size()));
			}
			lPtns.addAll(getPartnerResponse(parent, operationSession.balancePartner(parent), lPChilds.size()));
		}else{
			lPtns.addAll(getPartnerResponse(parent, operationSession.balancePartner(parent), lPChilds.size()));
		}
		return new APGPartnerResponse("0", "OK",lPtns);
	}

	private static List<APGPartner> getPartnerResponse(Partner p, BigDecimal solde, int nbChild) {
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		APGPartner ptn = new APGPartner();
		ptn.setId(p.getIdPartner());
		ptn.setName(p.getName());
		ptn.setTelephonePartner(p.getTelephonePartner());
		ptn.setTelephoneContact(p.getTelephoneContact());
		ptn.setEmail(p.getEmailContact());
		ptn.setCurrencyName(p.getCurrencyName());
		ptn.setCountryIsoCode(p.getCountryIsoCode());
		ptn.setCountryIndicatif(p.getCountryIndicatif());
		ptn.setNomContact(p.getNomContact());
		ptn.setPrenomContact(p.getPrenomContact());
		ptn.setPartnerType(p.getPType()); p.getIsActive();
		ptn.setIsActive(p.getIsActive());
		ptn.setType(p.getFilsDistributeur());
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
		ptn.setBalance(solde+"");
		ptn.setNbChild(nbChild+"");
		ptn.setRegion(p.getRegionName());
		ptn.setCodeAgence(p.getCodeAgence());

		List<APGPartner> lPtns = new ArrayList<APGPartner>();
		lPtns.add(ptn);

		return lPtns;
	}

}
