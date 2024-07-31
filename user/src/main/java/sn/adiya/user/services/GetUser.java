package sn.adiya.user.services;

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
import sn.fig.entities.PartnerUtilisateur;
import sn.fig.session.AdministrationSession;
import sn.fig.session.AdministrationSessionBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.user.object.APGUtilisateur;
import sn.adiya.user.object.APGUtilisateurResponse;
/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * Moussa SENE
 * Cherif DIOUF
 * @version 1.0
 **/
@Stateless
public class GetUser {
	final static String TAG = GetUser.class+"";
	final static Logger LOG = Logger.getLogger(GetUser.class);

	public Response Service(String flashcode, APGCommonRequest apgValidateRequest){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			AdministrationSession administrationSession = (AdministrationSession) BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());

			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1802.getCode(), ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("value"), apgValidateRequest.getValue());
			List<Utilisateur> lU = administrationSession.getUserBy(USER, apgValidateRequest.getValue());
			List<APGUtilisateur> lpts = new ArrayList<APGUtilisateur>();
			String[] param = {"idUtilisateur"};
			for (Utilisateur user : lU) {
				APGUtilisateur us = new APGUtilisateur();
				if(USER.getIdUtilisateur().equals(user.getIdUtilisateur())) continue;
				if(BEConstantes.ESPACE_PROVIDER.equals(USER.getPartner().getEspace()) || BEConstantes.ESPACE_ENVOYEUR_PAYEUR.equals(USER.getPartner().getEspace())) {
					Object[] dt = {user.getIdUtilisateur()};
					List<PartnerUtilisateur> lPart = session.executeNamedQueryList(PartnerUtilisateur.class, "findEntiteByUtilisateur", param, dt);
					if(!lPart.isEmpty()) {
						if(lPart.get(0).getPartner().getPartnerType().getType().equals(BEConstantes.PARTNER_CAISSE)) {
							if(lPart.get(0).getPartner().getParent().getPartnerType().getType().equals(BEConstantes.PARTNER_AGENCE)) {
								us.setAgence(lPart.get(0).getPartner().getParent().getName());
								if(lPart.get(0).getPartner().getParent().getParent().getPartnerType().getType().equals(BEConstantes.PARTNER_DISTRIBUTEUR))
									us.setDistributeur(lPart.get(0).getPartner().getParent().getName());
							}else if(lPart.get(0).getPartner().getParent().getPartnerType().getType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)) { 
								us.setSousDistributeur(lPart.get(0).getPartner().getParent().getName());
								if(lPart.get(0).getPartner().getParent().getPartnerType().getType().equals(BEConstantes.PARTNER_DISTRIBUTEUR))
									us.setDistributeur(lPart.get(0).getPartner().getParent().getName());
							}
							us.setCaisse(lPart.get(0).getPartner().getName());
						}else if(lPart.get(0).getPartner().getPartnerType().getType().equals(BEConstantes.PARTNER_AGENCE)) {
							us.setAgence(lPart.get(0).getPartner().getName());
							if(lPart.get(0).getPartner().getParent().getPartnerType().getType().equals(BEConstantes.PARTNER_DISTRIBUTEUR))
								us.setDistributeur(lPart.get(0).getPartner().getParent().getName());
						}
						else if(lPart.get(0).getPartner().getPartnerType().getType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)) {
							us.setSousDistributeur(lPart.get(0).getPartner().getName());
							if(lPart.get(0).getPartner().getParent().getPartnerType().getType().equals(BEConstantes.PARTNER_DISTRIBUTEUR))
								us.setDistributeur(lPart.get(0).getPartner().getParent().getName());
						}
						else if(lPart.get(0).getPartner().getPartnerType().getType().equals(BEConstantes.PARTNER_DISTRIBUTEUR)) {
							us.setDistributeur(lPart.get(0).getPartner().getName());
						}
						us.setIsValidated(lPart.get(0).getIsValidated());
					}
				}
				us.setId(user.getIdUtilisateur());
				us.setAdresse(user.getAdresse());
				us.setPhone(user.getPhone());
				us.setEmail(user.getEmail());
				us.setNom(user.getNom());
				us.setPrenom(user.getPrenom());
				us.setGenre(user.getGenre());
				us.setLogin(user.getLogin());
				us.setProfil(user.getGroupeUtilisateur().getIdGroupeUtilisateur());
				us.setLibelleProfil(user.getGroupeUtilisateur().getLibelle());
				us.setFirst(user.getFirst());
				us.setIsInit(user.getIsInit());
				us.setIsActive(user.getIsActive());
				us.setIsDongle(user.getIsDongle());
				us.setReference(user.getReference());
				us.setMatricule(user.getMatricule());
				us.setPartnerName(user.getPartner().getName());
				us.setPartnerId(user.getPartner().getIdPartner().toString());
				us.setPartnerType(user.getPartner().getPartnerType().getType());
				us.setIsMasterDealer(user.getIsMasterDealer());
				us.setNumeroDocument(user.getDocumentNumber());
				us.setExpirationDocDate(APIUtilVue.getInstance().commonDateToString(user.getExpirationDocDate(), BEConstantes.FORMAT_DATE_DAY_MM_YYYY));
				us.setIsConnected(user.getIsConnected());
				us.setNbPassword(user.getNbPassword());
				lpts.add(us);

			}
			return Response.ok().entity(new APGUtilisateurResponse("0", "OK",lpts))
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
