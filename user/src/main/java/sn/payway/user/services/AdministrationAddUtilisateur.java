package sn.payway.user.services;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import sn.apiapg.common.entities.GroupeUtilisateur;
import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.exception.TransactionException;
import sn.apiapg.common.utils.APGCommonRequest;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.AdministrationJsonResponse1;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.StringOperation;
import sn.apiapg.entities.Partner;
import sn.apiapg.mdb.EmailMessage;
import sn.apiapg.session.AdministrationSession;
import sn.apiapg.session.AdministrationSessionBean;
import sn.apiapg.session.DongleSession;
import sn.apiapg.session.DongleSessionBean;
import sn.apiapg.session.MessageSenderService;
import sn.apiapg.session.MessageSenderServiceBean;
import sn.apiapg.session.OperationSession;
import sn.apiapg.session.OperationSessionBean;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.utils.Constantes;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * Moussa SENE
 * Cherif DIOUF
 * @version 1.0 
 **/

@Stateless
public class AdministrationAddUtilisateur {
	final static String TAG = AdministrationAddUtilisateur.class+"";
	final static Logger LOG = Logger.getLogger(AdministrationAddUtilisateur.class);

	public Response Service(String flashcode,APGCommonRequest apgUtilisateurRequest) throws Exception{
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			AdministrationSession administrationSession = (AdministrationSession) BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());
			MessageSenderService messageSenderService = (MessageSenderService) BeanLocator.lookUp(MessageSenderServiceBean.class.getSimpleName());
			DongleSession dongleSession = (DongleSession) BeanLocator.lookUp(DongleSessionBean.class.getSimpleName());
			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());

			LOG.info("####### Debut Add Utilisateur from APG >>>>>>> |"+flashcode+"|");
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur : "), USER);
			String prenom = apgUtilisateurRequest.getPrenom();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("prenom : "), prenom);
			String nom = apgUtilisateurRequest.getNom();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("nom : "), nom);
			String email = apgUtilisateurRequest.getEmail();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("email : "), email);
			String phone = apgUtilisateurRequest.getPhone();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("phone : "), phone);
			String login = apgUtilisateurRequest.getLogin();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("login : "), login);
			String profilCode = apgUtilisateurRequest.getProfil();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("profilCode : "), profilCode);
			String type = apgUtilisateurRequest.getType();
			String partnerId = apgUtilisateurRequest.getPartnerId();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("partnerId : "), partnerId);
			if(apgUtilisateurRequest.getIsDongle() != null &&  apgUtilisateurRequest.getIsDongle())
				utilVue.CommonObject(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("reference : "), apgUtilisateurRequest.getReference());
			String matricule = null;
			try {
				Long.parseLong(partnerId);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1801.getCode(), ErrorResponse.SYNTAXE_ERRORS_1801.getMessage("partnerId")))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
						.build();
			}
			Partner partner = (Partner) session.findObjectById(Partner.class, Long.parseLong(partnerId), null);
			utilVue.CommonObject(null, TAG, ErrorResponse.AUTHENTICATION_ERRORS_1707.getCode(), ErrorResponse.AUTHENTICATION_ERRORS_1707.getMessage("Partner"), partner);
			LOG.info(">>>>>>>>>>>>>>>> PROFIL : "+profilCode);
			GroupeUtilisateur profil = (GroupeUtilisateur) session.findObjectById(GroupeUtilisateur.class, null, profilCode);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("GroupeUtilisateur"), profil);
			if(partner.getPType().equals(BEConstantes.PARTNER_PROVIDER)) {
				String[] Profil = {BEConstantes.GROUPE_ADMIN_MAKER_P, BEConstantes.GROUPE_SUPER_ADMIN_P,BEConstantes.GROUPE_SUPER_ADMIN_DA, BEConstantes.GROUPE_SUPER_ADMIN_SD,BEConstantes.GROUPE_SUPERVISEUR_SD};
				utilVue.commonVerifyProfil(USER, TAG, Profil);
				matricule = apgUtilisateurRequest.getMatricule();
			}
			if(profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_P) || profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_DA) || profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_SD) ||
					profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_P) || profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPERVISEUR_P) || profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CONTROLEUR_P) || 
					profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CAISSIER_P)){
				if(!partner.getPType().equals(BEConstantes.PARTNER_PROVIDER)) {
					return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("EntityType")))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
							.build();						
				}
			}
			List<Utilisateur> lLoginAgents = administrationSession.findByLoginAgent(login);
			if(!lLoginAgents.isEmpty() || lLoginAgents.size() >0){
				return Response.ok().entity(new AbstractResponse("1", "Utilisateur (login "+login+") "+Constantes.F_DUPLICATE))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}
			Utilisateur	utilisateur = new Utilisateur();
			utilisateur.setDate(new Date());
			utilisateur.setEmail(email);
			utilisateur.setEntreprise("APG");
			utilisateur.setGenre("MR");
			utilisateur.setAdresse(apgUtilisateurRequest.getAdresse());
			utilisateur.setGroupeUtilisateur(profil);
			utilisateur.setIdGroupeUtilisateur(profil.getIdGroupeUtilisateur());
			utilisateur.setLibelleGroupeUtilisateur(profil.getLibelle());
			utilisateur.setPartnerId(partner.getIdPartner()+"");
			utilisateur.setPartnerName(partner.getName());
			utilisateur.setPartnerCode(partner.getCode());

			if(profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPERVISEUR_P) || profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CONTROLEUR_P) || 
					profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CAISSIER_P))
				utilisateur.setIsInit(true);
			else
				utilisateur.setIsInit(false);

			utilisateur.setLogin(login);
			utilisateur.setIsActive(true);
			LOG.info("### --- --- - Partner "+partner.getPType()+" GROUPE "+USER.getIdGroupeUtilisateur());
			if(!partner.getPType().equals(BEConstantes.PARTNER_PROVIDER)) {
				utilisateur.setIsActive(true);
			}else if(USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_MAKER_P)) { 
				utilisateur.setIsActive(false);
			}else if(USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_P) || USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPERVISEUR_SD) ||
					USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_SD) || USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_DA)){
				String otp = apgUtilisateurRequest.getOtp();
				if(USER.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) || USER.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_SENDER_DIASPORA) || USER.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_SENDER)) {
					if(!Boolean.TRUE.equals(utilVue.checkOtp(flashcode, otp))) {
						utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1807.getCode(),ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("otp : "), null);
					}
				}else {
					utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("otp : "), otp);
					String[] parameters = {"flashCode","otp"};
					Object[] data = {flashcode,otp};
					Utilisateur user = session.executeNamedQuerySingle(Utilisateur.class,"findByOTP", parameters, data);
					utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), user);
				}
				utilisateur.setIsActive(true);
			}
			utilisateur.setNom(nom);
			utilisateur.setMatricule(matricule);
			utilisateur.setPartner(operationSession.partnerUser(partner));
			String smsPassword = StringOperation.getPasswwd();
			utilisateur.setPassword(APIUtilVue.getInstance().apgSha(smsPassword));
			utilisateur.setPhone(phone);
			utilisateur.setPrenom(prenom);
			utilisateur.setFirst(true);
			utilisateur.setType(type);
			if(apgUtilisateurRequest.getIsDongle() != null) {
				utilisateur.setIsDongle(apgUtilisateurRequest.getIsDongle());
				utilisateur.setReference(apgUtilisateurRequest.getReference());
				if(!dongleSession.bindUser(apgUtilisateurRequest.getReference(), USER)){
					utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur not bound"), USER);
				}
			}
			utilisateur.setValeur(apgUtilisateurRequest.getValue());
			utilisateur.setDocumentNumber(apgUtilisateurRequest.getDocumentNumber());
			utilisateur.setExpirationDocDate(utilVue.commonStringToDate(apgUtilisateurRequest.getDateExpiration(), BEConstantes.FORMAT_DATE_DAY_MM_YYYY));

			utilisateur = (Utilisateur) session.saveObject(utilisateur); 
			/*
			 * Notify SMS
			 */
			String domaine = "";
			if(utilisateur.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER))
				domaine = "Telechargez l'application Optima Business ici, https://bit.ly/3tSxaZU";
			if(Boolean.TRUE.equals(utilisateur.getIsActive())) {
				String msg="Bonjour, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Poste "+utilisateur.getGroupeUtilisateur().getLibelle()+" Votre identifiant : "+utilisateur.getLogin()+" votre mot de passe : "+smsPassword+". "+domaine;
				if(utilisateur.getPartner().getLogo() != null && utilisateur.getPartner().getLogo().equals("logo-reliance"))
					msg="Hello, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profile: "+utilisateur.getGroupeUtilisateur().getLibelle()+" Your username: "+utilisateur.getLogin()+" your password: "+smsPassword+"";
				utilVue.notifyUtilisateur(utilisateur, msg);
				/*
				 * Notify MAIL
				 */
				String subject = "VOTRE COMPTE APG", header = utilisateur.getEmail();

				if("logo-dp".equalsIgnoreCase(utilisateur.getPartner().getLogo()))
					subject  = "VOTRE COMPTE LIMOPAY";

				String opt = utilisateur.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) ? "\"<tr style=\"color:white;\">\n" + 
						 "<th colspan=\"2\" bgcolor=\"black\">Merci de vous connecter sur https://pms.optima.world</th>\n" + 
						 "</tr>\n" : "";

				String datas = "";
				msg = BEConstantes.MESSAGE_HTML+"<table rules=\"all\" cellpadding=\"5px\" style=\"width:500px; border:solid 1px black; font-family:verdana; font-size:12px;\">\n" + 
						"   <tr>\n" + 
						"      <th bgcolor=\"white\" style=\"font-size:large\" colspan=\"2\" align=\"center\">Vos parametres de connexion</th>\n" + 
						"   </tr>\n" + 
						"   <tr style=\"color:white;\">\n" + 
						"      <th bgcolor=\"black\">Votre identifiant</th>\n" + 
						"      <th bgcolor=\"black\">Votre mot de passe</th>\n" + 
						opt +
						"   </tr>\n" + 
						"   <tr style=\"color:black;\">\n" + 
						"      <th bgcolor=\"white\"> "+utilisateur.getLogin()+" </th>\n" + 
						"      <th bgcolor=\"white\"> "+smsPassword+" </th>\n" + 
						"   </tr>\n" + 
						"</table> ";

				if(utilisateur.getPartner().getLogo() != null && utilisateur.getPartner().getLogo().equals("logo-reliance")) {
					msg = BEConstantes.MESSAGE_HTML+"<table rules=\"all\" cellpadding=\"5px\" style=\"width:500px; border:solid 1px black; font-family:verdana; font-size:12px;\">\n" + 
							"   <tr>\n" + 
							"      <th bgcolor=\"white\" style=\"font-size:large\" colspan=\"2\" align=\"center\">Your access settings</th>\n" + 
							"   </tr>\n" + 
							"   <tr style=\"color:white;\">\n" + 
							"      <th bgcolor=\"black\">Your username</th>\n" + 
							"      <th bgcolor=\"black\">Your password</th>\n" + 
							"   </tr>\n" + 
							"   <tr style=\"color:black;\">\n" + 
							"      <th bgcolor=\"white\"> "+utilisateur.getLogin()+" </th>\n" + 
							"      <th bgcolor=\"white\"> "+smsPassword+" </th>\n" + 
							"   </tr>\n" + 
							"</table> ";

					messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null, header, subject, msg, datas));
				}
				else
					messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null, header, subject, msg, datas));
			}
			LOG.info("####### Fin Add Utilisateur from APG >>>>>>>");
			return Response.ok().entity(AdministrationJsonResponse1.addUserResponse(utilisateur).toString())  
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
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