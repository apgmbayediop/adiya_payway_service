package sn.adiya.user.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import sn.fig.common.entities.GroupeUtilisateur;
import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.exception.TransactionException;
import sn.fig.common.utils.APGCommonRequest;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.common.utils.MailUtils;
import sn.fig.common.utils.StringOperation;
import sn.fig.entities.Partner;
import sn.fig.entities.PartnerUtilisateur;
import sn.fig.mdb.EmailMessage;
import sn.fig.session.AdministrationSession;
import sn.fig.session.AdministrationSessionBean;
import sn.fig.session.MessageSenderService;
import sn.fig.session.MessageSenderServiceBean;
import sn.fig.session.OperationSession;
import sn.fig.session.OperationSessionBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.utils.Constantes;
import sn.adiya.user.object.APGUtilisateur;
import sn.adiya.user.object.APGUtilisateurResponse;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationAddPartnerUtilisateur {
	final static String TAG = AdministrationAddPartnerUtilisateur.class+"";
	
	public Response Service(String flashcode,APGCommonRequest apgUtilisateurRequest) throws Exception{
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			AdministrationSession administrationSession = (AdministrationSession) BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());
			MessageSenderService messageSenderService = (MessageSenderService) BeanLocator.lookUp(MessageSenderServiceBean.class.getSimpleName());
			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());

			System.out.println("####### Debut Add Partner Utilisateur from APG >>>>>>>");
			APGUtilisateurResponse response; 
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);
			String prenom = apgUtilisateurRequest.getPrenom();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("prenom : "), prenom);
			String nom = apgUtilisateurRequest.getNom();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("nom : "), nom);
			String email = apgUtilisateurRequest.getEmail();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("email : "), email);
			String phone = apgUtilisateurRequest.getTelephonePartner();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("phone : "), phone);
			String login = apgUtilisateurRequest.getLogin();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("login : "), login);
			String profilCode = apgUtilisateurRequest.getProfil();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("profilCode : "), profilCode);
			String type = apgUtilisateurRequest.getType();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("type : "), type);
			String partnerId = apgUtilisateurRequest.getPartnerId();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("partnerId : "), partnerId);
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
			if(!type.equals(BEConstantes.PIECE_1) && !type.equals(BEConstantes.PIECE_2)){
				return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1801.getCode(), ErrorResponse.SYNTAXE_ERRORS_1801.getMessage("type")))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
						.build();			
			}
			GroupeUtilisateur profil = (GroupeUtilisateur) session.findObjectById(GroupeUtilisateur.class, null, profilCode);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("GroupeUtilisateur"), profil);
			if(!partner.getPType().equals(BEConstantes.PARTNER_SENDER_PAYER)) {
				String[] Profil = {BEConstantes.GROUPE_ADMIN_MAKER_EP,BEConstantes.GROUPE_SUPER_ADMIN_EP};
				utilVue.commonVerifyProfil(USER, TAG, Profil);
			}
			if(profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_EP) || profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_EP) || 
					profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPERVISEUR_EP) || profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CONTROLEUR_EP) || 
					profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CAISSIER_EP)){
				if(!partner.getPType().equals(BEConstantes.PARTNER_SENDER_PAYER)) {
					return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("EntityType")))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
							.build();						
				}
			}
			List<Utilisateur> lLoginAgents = administrationSession.findByLoginAgent(login);
			if(!lLoginAgents.isEmpty() || lLoginAgents.size() >0){
				return Response.ok().entity(new AbstractResponse("1", "Utilisateur (login) "+Constantes.F_DUPLICATE))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}
			Utilisateur	utilisateur = new Utilisateur();
			utilisateur.setDate(new Date());
			utilisateur.setEmail(email);
			utilisateur.setEntreprise("APG");
			utilisateur.setFlashCode(StringOperation.generateFlashcode(utilisateur.getIdUtilisateur()));
			utilisateur.setGenre("MR");
			utilisateur.setAdresse(apgUtilisateurRequest.getAdresse());
			utilisateur.setGroupeUtilisateur(profil);

			if(profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPERVISEUR_EP) || 
					profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CONTROLEUR_EP) || 
					profil.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CAISSIER_EP))
				utilisateur.setIsInit(true);
			else
				utilisateur.setIsInit(false);

			utilisateur.setLogin(login); 
			if(!partner.getPType().equals(BEConstantes.PARTNER_SENDER_PAYER)) {
				System.out.println("######## ! PARTNER_SENDER_PAYER");
				utilisateur.setIsActive(true);
			}else if(USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_MAKER_EP)){
				System.out.println("######## ! GROUPE_ADMIN_MAKER_EP");
				utilisateur.setIsActive(false);
			}else if(USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_EP)){
				System.out.println("######## ! GROUPE_SUPER_ADMIN_EP");
				String otp = apgUtilisateurRequest.getOtp();
				System.out.println("### --- --- OTP SUPER ADMIN: "+otp);
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
			utilisateur.setPartner(operationSession.partnerUser(partner));
			String smsPassword = StringOperation.getPasswwd();
			utilisateur.setPassword(APIUtilVue.getInstance().apgSha(smsPassword));
			utilisateur.setPhone(phone);
			utilisateur.setPrenom(prenom);
			utilisateur.setFirst(true);
			utilisateur.setType(type);
			utilisateur.setValeur(apgUtilisateurRequest.getValue());

			utilisateur = (Utilisateur) session.saveObject(utilisateur); 

			session.saveObject(new PartnerUtilisateur(utilisateur, partner, true));

			List<APGUtilisateur> lutilisateurs = new ArrayList<APGUtilisateur>();
			APGUtilisateur apgUtilisateur = new APGUtilisateur();
			apgUtilisateur.setId(utilisateur.getIdUtilisateur());
			apgUtilisateur.setAdresse(utilisateur.getAdresse());
			apgUtilisateur.setEmail(utilisateur.getEmail());
			apgUtilisateur.setNom(utilisateur.getNom());
			apgUtilisateur.setPhone(utilisateur.getPhone());
			apgUtilisateur.setPrenom(utilisateur.getPrenom());
			apgUtilisateur.setFirst(utilisateur.getFirst());
			apgUtilisateur.setProfil(utilisateur.getIdGroupeUtilisateur());
			apgUtilisateur.setTypedocument(utilisateur.getType());
			apgUtilisateur.setNumeroDocument(utilisateur.getValeur());
			apgUtilisateur.setPartnerId(utilisateur.getPartner().getIdPartner()+"");;

			lutilisateurs.add(apgUtilisateur);
			/*
			 * Notify SMS
			 */
			String msg="Bonjour, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Poste "+utilisateur.getGroupeUtilisateur().getLibelle()+" Votre identifiant : "+utilisateur.getLogin()+" votre mot de passe : "+smsPassword;
			APIUtilVue.getInstance().sendSMS("APGSA",utilisateur.getPartner().getCountry().getCountryIndicatif(), utilisateur.getPhone(), msg);
			/*
			 * Notify MAIL
			 */
			String subject = "VOTRE COMPTE APG", header = utilisateur.getEmail();
					
			if("logo-dp".equalsIgnoreCase(utilisateur.getPartner().getLogo()))
				subject  = "VOTRE COMPTE LIMOPAY";
			
			if("logo-coopec".equalsIgnoreCase(utilisateur.getPartner().getLogo()) || "logo-cfp".equalsIgnoreCase(utilisateur.getPartner().getLogo()))
				subject  = "VOTRE COMPTE CFP";
			
			String datas = "";
			if("logo-coopec".equalsIgnoreCase(utilisateur.getPartner().getLogo()) || "logo-cfp".equalsIgnoreCase(utilisateur.getPartner().getLogo()))
				MailUtils.sendEmailCfp(utilisateur.getEmail(), subject, msg, true, null);
			else
				messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null, header, subject, msg, datas));
			
			System.out.println("####### Fin Add Partner Utilisateur from APG >>>>>>>");
			response = new APGUtilisateurResponse("0", "OK",lutilisateurs);
			return Response.ok().entity(response)
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