package sn.adiya.partner.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.exception.TransactionException;
import sn.fig.common.utils.APGCommonRequest;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.Partner;
import sn.fig.entities.PartnerType;
import sn.fig.entities.PartnerUtilisateur;
import sn.fig.session.AdministrationSession;
import sn.fig.session.AdministrationSessionBean;
import sn.fig.session.OperationSession;
import sn.fig.session.OperationSessionBean;
import sn.fig.session.ServiceSession;
import sn.fig.session.ServiceSessionBean;
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
public class GetEntity {
	final static String TAG = GetEntity.class+"";
	final static Logger LOG = Logger.getLogger(GetEntity.class);

	public Response Service(String flashcode, APGCommonRequest apgValidateRequest){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			AdministrationSession administrationSession = (AdministrationSession) BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());
			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());
			ServiceSession serviceSession = (ServiceSession) BeanLocator.lookUp(ServiceSessionBean.class.getSimpleName());
			
			Utilisateur user = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				user = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), user);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1802.getCode(), ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("value"), apgValidateRequest.getValue());
			Partner p = null;
			if((user.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CARD_PROGRAM_MANAGER_P)
					|| user.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_P)
					|| user.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_CHECKER_P)
					|| user.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_MAKER_P)
					|| user.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CONTROLEUR_P)
					|| user.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CONTROLEUR_MAKER_P)
					|| user.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CONTROLEUR_CHECKER_P)
					|| user.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_PARTENAIRE_ECOMMERCE_P)
					|| user.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_COMPLIANCE_OFFICER_P))
					|| !user.getPartner().getEspace().equals(BEConstantes.ESPACE_PROVIDER)) {
				p = user.getPartner();
			}else {
				String[] params = {"idUtilisateur"};
				Object[] dats = {user.getIdUtilisateur()};
				List<PartnerUtilisateur> lPartnerUtilisateurs = (List<PartnerUtilisateur>) session.executeNamedQueryList(PartnerUtilisateur.class,"findValidatedEntiteByUtilisateur", params, dats);
				if(lPartnerUtilisateurs.isEmpty()) 
					throw new TransactionException("1", "User not linked");
				PartnerUtilisateur pu = (PartnerUtilisateur) lPartnerUtilisateurs.get(0); 
				p = pu.getPartner();
			}
			List<Partner> lPs = administrationSession.getPartnerBy(p, apgValidateRequest.getValue());
			List<APGPartner> lpts = new ArrayList<APGPartner>();
			for(Partner partner : lPs){
				APGPartner pt = new APGPartner();
				pt.setType(partner.getFilsDistributeur());
				pt.setId(partner.getIdPartner());
				pt.setConsumerId(partner.getConsumerId());
				pt.setName(partner.getName());
				pt.setTelephonePartner(partner.getTelephonePartner());
				pt.setTelephoneContact(partner.getTelephoneContact());
				pt.setEmail(partner.getEmailContact());
				pt.setNomContact(partner.getNomContact());
				pt.setPrenomContact(partner.getPrenomContact());
				pt.setPartnerCode(partner.getCode());
				pt.setCurrencyName(partner.getCurrencyName());
				pt.setCodeAgence(partner.getCodeAgence());
				pt.setMcc(partner.getMcc());
				pt.setModeReglement(partner.getModeReglement());
				pt.setNumeroCompte(partner.getNumeroCompte());
				pt.setAuth(utilVue.apgDeCrypt(partner.getToken()));BigDecimal mainBalance = BigDecimal.ZERO;
				if(partner.getPType().equals(BEConstantes.PARTNER_AGREGATOR))
					mainBalance = serviceSession.balancePivotA(partner.getCode(), partner.getPrincipalAccountNumber(), partner.getCurrencyName());
				else
					mainBalance = operationSession.balancePartner(partner);
				pt.setBalance(mainBalance+"");
				String param[] = {"idPartner"};
				Object data3[] = {partner.getIdPartner()};
				PartnerUtilisateur pu = session.executeNamedQuerySingle(PartnerUtilisateur.class, "findUtilisateurByEntite", param, data3);
				if(pu != null && Boolean.TRUE.equals(pu.getIsValidated())) {
					pt.setIsAssigned(true);
					pt.setAssignedUserId(pu.getUtilisateur().getIdUtilisateur());
					pt.setAssignedUserFirstName(pu.getUtilisateur().getPrenom());
					pt.setAssignedUserLastName(pu.getUtilisateur().getNom());
					pt.setAssignedUserPhone(pu.getUtilisateur().getPhone());
					pt.setCountryIndicatif(pu.getUtilisateur().getPartner().getCountryIndicatif());
				}
				PartnerType pType = partner.getPartnerType();
				pt.setPartnerType(pType.getType());
				pt.setPartnerTypeId(pType.getIdPartnerType()+"");
				pt.setActive(partner.getIsActive());
				pt.setCountryIsoCode(partner.getCountryIsoCode());
				pt.setIsB2B(partner.getIsB2B());
				pt.setIsDecouvert(partner.getIsDecouvert());
				pt.setBalanceDecouvert(partner.getBalanceDecouvert());

				lpts.add(pt);
			}
			LOG.info("####### End List Partner payer from APG >>>>>>>");
			APGPartnerResponse response = new APGPartnerResponse("0", "OK",lpts);
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
