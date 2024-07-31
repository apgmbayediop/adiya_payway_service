package sn.adiya.user.services;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
import sn.fig.session.AdministrationSession;
import sn.fig.session.AdministrationSessionBean;
import sn.fig.session.OperationSession;
import sn.fig.session.OperationSessionBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.user.object.APGUtilisateur;
import sn.adiya.user.object.APGUtilisateurResponse;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationListUtilisateur {
	
	public final static Logger LOG = Logger.getLogger(AdministrationListUtilisateur.class);
	final static String TAG = AdministrationListUtilisateur.class+"";
	
	public Response Service(String flashcode, APGCommonRequest apgListUtilisateurRequest) throws Exception{
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			AdministrationSession administrationSession = (AdministrationSession) BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());
			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());

			LOG.info("####### Debut List Utilisateur from APG >>>>>>>");
			String userId = apgListUtilisateurRequest.getUserId();
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);

			List<Utilisateur> lUtilisateur = new ArrayList<Utilisateur>();
			String partnerId = apgListUtilisateurRequest.getPartnerId();
			if(partnerId == null)
				partnerId = USER.getPartner().getIdPartner()+"";
			if(userId != null){
				try {
					Long.parseLong(userId);
				} catch (NumberFormatException e) {
					e.printStackTrace();
					return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("userId")))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
							.build();
				}
				return Response.ok().entity(getUserById(USER, userId))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}

			try {
				Long.parseLong(partnerId);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("partnerId : "+partnerId)))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
						.build();
			}
			Partner partner = session.findObjectById(Partner.class, Long.parseLong(partnerId), null);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Partner"), partner);

			LOG.info("### --- --- - Profil : "+USER.getIdGroupeUtilisateur());
			LOG.info("### --- --- - Prenom : "+USER.getPrenom());
			LOG.info("### --- --- - Nom : "+USER.getNom());
			LOG.info("### --- --- - Linked Partner : "+partner.getName());
			LOG.info("### --- --- - Code Partner : "+partner.getCode());
			LOG.info("### --- --- - Rang "+USER.getGroupeUtilisateur().getRang());

			if(partner.getCode().equals(BEConstantes.CODE_APG)) {
				LOG.info("### --- --- - Bloque APG ALL ");

				String[] parameters = {"rang","idPartner"};
				Object[] data = { USER.getGroupeUtilisateur().getRang(),partner.getIdPartner()};//findUserPartner
			//	lUtilisateur = (List<Utilisateur>) session.executeNamedQueryList(Utilisateur.class,"findUserAPG", parameters, data);
				lUtilisateur = (List<Utilisateur>) session.executeNamedQueryList(Utilisateur.class,"findUserPartner", parameters, data);
			}else if(USER.getPartner().getEspace().equals(BEConstantes.ESPACE_AGREGATOR)) {
				LOG.info("### --- --- - Bloque Agregator child");
				String[] parameters = {"idPartner"};
				Object[] data = {Long.parseLong(partnerId)};
				lUtilisateur = session.executeNamedQueryList(Utilisateur.class, "findUtoRemove", parameters, data);
			}else if(USER.getPartner().getEspace().equals(BEConstantes.ESPACE_PROVIDER) || USER.getPartner().getEspace().equals(BEConstantes.ESPACE_MONETIQUE)) {
				/*
				 * [Dramane BA] : 17/03/2022
				 * Ajout de la condition || USER.getPartner().getEspace().equals(BEConstantes.ESPACE_MONETIQUE)
				 * Pour permettre l'affichage des utilisateur d'un partenaire de type monétique dans le cadre du projet de mise à disposition
				 * d'une interface d'acceptation GAINDE 2000
				 * */
				LOG.info("---------------------- 1 --------------------");
				if(apgListUtilisateurRequest.getEntityId() != null && !apgListUtilisateurRequest.getEntityId().equals("")) {
					LOG.info("---------------------- 2 --------------------");
					try {
						Long.parseLong(apgListUtilisateurRequest.getEntityId());
					} catch (NumberFormatException e) {
						return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("entityId")))
								.header(BEConstantes.AccessControlAllowOrigin,"*")
								.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
								.build();
					}
					Partner p = session.findObjectById(Partner.class, Long.parseLong(apgListUtilisateurRequest.getEntityId()), null);
					utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("entity"), p);
					if(!USER.getPartner().getIdPartner().equals(operationSession.partnerUser(p).getIdPartner())) {
						LOG.info("---------------------- 3 --------------------");
						return Response.ok().entity(new APGUtilisateurResponse("0", "OK",new ArrayList<APGUtilisateur>()))
								.header(BEConstantes.AccessControlAllowOrigin,"*")
								.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
								.build();
					}
					lUtilisateur = administrationSession.findUserByEntity(p,USER.getGroupeUtilisateur().getRang());
					LOG.info("---------------------- 4 --------------------");
				}else {
					LOG.info("### --- --- - 1 Bloque Partner idPartner "+partnerId+" rang "+USER.getGroupeUtilisateur().getRang());
					String[] parameters = {"rang","idPartner"};
					Object[] data = { USER.getGroupeUtilisateur().getRang(),Long.parseLong(partnerId)};
					lUtilisateur = (List<Utilisateur>) session.executeNamedQueryList(Utilisateur.class,"findUserPartner", parameters, data);
				}
			}else{
				LOG.info("### --- --- - 2 Bloque Partner idPartner "+partnerId+" rang "+USER.getGroupeUtilisateur().getRang());
				String[] parameters = {"rang","idPartner"};
				Object[] data = { USER.getGroupeUtilisateur().getRang(),Long.parseLong(partnerId)};
				lUtilisateur = (List<Utilisateur>) session.executeNamedQueryList(Utilisateur.class,"findUserPartner", parameters, data);
			}
			List<APGUtilisateur> lpts = new ArrayList<APGUtilisateur>();
			String[] param = {"idUtilisateur"};
			if(partnerId != null){
				for(Utilisateur user : lUtilisateur){
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
			}
			LOG.info("####### Fin List all Utilisateur from APG >>>>>>>");
			return Response.ok().entity(new APGUtilisateurResponse("0", "OK",lpts))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.build();
		}  catch (TransactionException e) {
			return Response.ok().entity(new AbstractResponse(e.getCode(), e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();
		}	
	}

	private static String getUserById(Utilisateur USER, String userId)  {
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		String prenom="",nom="",profil="";
		String[] parameters = {"idUtilisateur"};
		Object[] data = {Long.parseLong(userId)};
		Utilisateur user = session.executeNamedQuerySingle(Utilisateur.class, "findUserById", parameters, data);
		ArrayNode lists = new ObjectMapper().createArrayNode();
		ObjectNode obj0 = new ObjectMapper().createObjectNode();
		obj0.put("code", "0");obj0.put("message", "OK");
		lists.add(obj0);
		if(user != null) {
			prenom+=user.getPrenom();nom+=user.getNom();profil+=user.getLibelleGroupeUtilisateur();
			ObjectNode obj = new ObjectMapper().createObjectNode();
			obj.put("id", user.getIdUtilisateur()+"");
			obj.put("adresse", user.getAdresse());
			obj.put("phone", user.getPhone());
			obj.put("email", user.getEmail());
			obj.put("nom", user.getNom());
			obj.put("prenom", user.getPrenom());
			obj.put("genre", user.getGenre());
			obj.put("first", user.getFirst());
			obj.put("login", user.getLogin());
			obj.put("profil", user.getGroupeUtilisateur().getIdGroupeUtilisateur());
			obj.put("libelleProfil", user.getLibelleGroupeUtilisateur());
			obj.put("init", user.getIsInit());
			obj.put("isActive", user.getIsActive());
			obj.put("isConnected", user.getIsConnected());
			obj.put("isDongle", user.getIsDongle());
			obj.put("reference", user.getReference());
			obj.put("partnerName", user.getPartner().getName());
			obj.put("isMasterDealer",user.getIsMasterDealer());
			obj.put("numeroDocument",user.getDocumentNumber());
			obj.put("expirationDocDate", APIUtilVue.getInstance().commonDateToString(user.getExpirationDocDate(), BEConstantes.FORMAT_DATE_DAY_MM_YYYY));
			obj.put("nbPassword", user.getNbPassword());

			if(BEConstantes.ESPACE_PROVIDER.equals(USER.getPartner().getEspace()) || BEConstantes.ESPACE_ENVOYEUR_PAYEUR.equals(USER.getPartner().getEspace())) {
				String[] param = {"idUtilisateur"};
				Object[] dt = {user.getIdUtilisateur()};
				List<PartnerUtilisateur> lPart = session.executeNamedQueryList(PartnerUtilisateur.class, "findEntiteByUtilisateur", param, dt);
				if(!lPart.isEmpty()) {
					if(lPart.get(0).getPartner().getPartnerType().getType().equals(BEConstantes.PARTNER_CAISSE)) {
						if(lPart.get(0).getPartner().getParent().getPartnerType().getType().equals(BEConstantes.PARTNER_AGENCE)) {
							obj.put("agence",lPart.get(0).getPartner().getParent().getName());
							if(lPart.get(0).getPartner().getParent().getParent().getPartnerType().getType().equals(BEConstantes.PARTNER_DISTRIBUTEUR))
								obj.put("distributeur",lPart.get(0).getPartner().getParent().getName());
						}else if(lPart.get(0).getPartner().getParent().getPartnerType().getType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)) { 
							obj.put("distributeur",lPart.get(0).getPartner().getParent().getName());
							if(lPart.get(0).getPartner().getParent().getPartnerType().getType().equals(BEConstantes.PARTNER_DISTRIBUTEUR))
								obj.put("sousDistributeur",lPart.get(0).getPartner().getParent().getName());
						}
						obj.put("caisse",lPart.get(0).getPartner().getName());
					}else if(lPart.get(0).getPartner().getPartnerType().getType().equals(BEConstantes.PARTNER_AGENCE)) {
						obj.put("agence",lPart.get(0).getPartner().getName());
						if(lPart.get(0).getPartner().getParent().getPartnerType().getType().equals(BEConstantes.PARTNER_DISTRIBUTEUR))
							obj.put("distributeur",lPart.get(0).getPartner().getParent().getName());
					}
					else if(lPart.get(0).getPartner().getPartnerType().getType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)) {
						obj.put("sousDistributeur",lPart.get(0).getPartner().getName());
						if(lPart.get(0).getPartner().getParent().getPartnerType().getType().equals(BEConstantes.PARTNER_DISTRIBUTEUR))
							obj.put("distributeur",lPart.get(0).getPartner().getParent().getName());
					}
					obj.put("isValidated",lPart.get(0).getIsValidated());
				}
			}

			lists.add(obj);
		}
		LOG.info("####### Fin List Utilisateur "+prenom+" "+nom+" "+profil+" from APG >>>>>>>"); 
		return lists.toString();
	}

}
