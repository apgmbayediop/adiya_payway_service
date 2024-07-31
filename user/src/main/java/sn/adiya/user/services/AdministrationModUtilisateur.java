package sn.adiya.user.services;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import sn.adiya.user.object.APGUtilisateur;
import sn.adiya.user.object.APGUtilisateurResponse;
import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.exception.TransactionException;
import sn.fig.common.utils.APGCommonRequest;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.common.utils.StringOperation;
import sn.fig.mdb.EmailMessage;
import sn.fig.session.MessageSenderService;
import sn.fig.session.MessageSenderServiceBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationModUtilisateur {
	final static String TAG = AdministrationModUtilisateur.class+"";
	final static Logger LOG = Logger.getLogger(AdministrationModUtilisateur.class);
	public Response Service(String flashcode,APGCommonRequest apgUtilisateurRequest) throws Exception{
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			MessageSenderService messageSenderService = (MessageSenderService) BeanLocator.lookUp(MessageSenderServiceBean.class.getSimpleName());

			LOG.info("####### Start Edit Utilisateur from APG >>>>>>> "+apgUtilisateurRequest.getProfil());
			Utilisateur user = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				user = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur : "), user);

			Long idUtilisateur = apgUtilisateurRequest.getId();
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("idUtilisateur : "), idUtilisateur+"");

			String profile[] = {BEConstantes.GROUPE_SUPER_ADMIN_EP,BEConstantes.GROUPE_ADMIN_CHECKER_EP,BEConstantes.GROUPE_ADMIN_CHECKER_P,BEConstantes.GROUPE_ADMIN_MAKER_P,BEConstantes.GROUPE_SUPER_ADMIN,BEConstantes.GROUPE_SUPER_ADMIN_P};
			Utilisateur utilisateur = null;
			String[] parameters = {"idUtilisateur"};
			Object[] data = {idUtilisateur};
			List<Utilisateur> lUsers = session.executeNamedQueryList(Utilisateur.class, "findUserById", parameters, data);
			if(!lUsers.isEmpty()) {
				utilisateur = lUsers.get(0);
			}
			utilVue.CommonObject(null, "", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), utilisateur);

			if(utilisateur.getPartner().getPType().equals(BEConstantes.PARTNER_SENDER_PAYER) || utilisateur.getPartner().getPType().equals(BEConstantes.PARTNER_PROVIDER))
				utilVue.commonVerifyProfil(user, TAG, profile);

			if(apgUtilisateurRequest.getAdresse() != null && !apgUtilisateurRequest.getAdresse().isBlank())
				utilisateur.setAdresse(apgUtilisateurRequest.getAdresse());
			if(apgUtilisateurRequest.getEmail() != null && !apgUtilisateurRequest.getEmail().isBlank())
				utilisateur.setEmail(apgUtilisateurRequest.getEmail());
			if(apgUtilisateurRequest.getNom() != null && !apgUtilisateurRequest.getNom().isBlank())
				utilisateur.setNom(apgUtilisateurRequest.getNom());
			if(apgUtilisateurRequest.getPrenom() != null && !apgUtilisateurRequest.getPrenom().isBlank())
				utilisateur.setPrenom(apgUtilisateurRequest.getPrenom());
			if(apgUtilisateurRequest.getPhone() != null && !apgUtilisateurRequest.getPhone().isBlank())
				utilisateur.setPhone(apgUtilisateurRequest.getPhone()); 
			if(apgUtilisateurRequest.getIsDongle() != null && apgUtilisateurRequest.getIsDongle()) {
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("Reference : "), apgUtilisateurRequest.getReference());
				utilisateur.setIsDongle(apgUtilisateurRequest.getIsDongle());
				utilisateur.setReference(apgUtilisateurRequest.getReference());
			
			}
			if(Boolean.TRUE.equals(apgUtilisateurRequest.getIsMasterDealer()))
				utilisateur.setIsMasterDealer(true);
			if(utilisateur.getGroupeUtilisateur().getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_CAISSIER_P) && Boolean.TRUE.equals(apgUtilisateurRequest.getHasUssd())) {
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("ussdLogin : "), apgUtilisateurRequest.getUssdLogin());
				if(apgUtilisateurRequest.getUssdLogin().length() != 12)
					utilVue.CommonLabel(null,TAG,ErrorResponse.USSD_LOGIN_INCORRECT_OPTIMA.getCode(),ErrorResponse.USSD_LOGIN_INCORRECT_OPTIMA.getMessage("ussdLogin : "), null);
				String pin = StringOperation.getTransactionCode(4);
				apgUtilisateurRequest.setUssdPassword(pin);
				String[] param = {"ussdLogin"};
				Object[] dt = {apgUtilisateurRequest.getUssdLogin()};
				List<Utilisateur> lU = session.executeNamedQueryList(Utilisateur.class, "findUserByUssdLogin", param, dt);
				if(!lU.isEmpty()) 
					utilVue.CommonLabel(null,TAG,ErrorResponse.USSD_DUPLICATA_OPTIMA.getCode(),ErrorResponse.USSD_DUPLICATA_OPTIMA.getMessage("ussdLogin"), null);
				utilisateur.setUssdLogin(apgUtilisateurRequest.getUssdLogin());
				utilisateur.setUssdPassword(utilVue.apgSha(apgUtilisateurRequest.getUssdLogin()+apgUtilisateurRequest.getUssdPassword()));
			}
			if(apgUtilisateurRequest.getIsActive() != null && !apgUtilisateurRequest.getIsActive().isBlank()) {
				if((apgUtilisateurRequest.getIsActive().equals("1") || apgUtilisateurRequest.getIsActive().equals(Boolean.TRUE.toString())) && !Boolean.TRUE.equals(utilisateur.getIsActive())) {
					LOG.info("ICI ICI >>>>>>>>>>>>>>>> "+apgUtilisateurRequest.getIsActive());
					utilisateur.setIsActive(true);
					String smsPassword = StringOperation.getPasswwd();
					utilisateur.setPassword(APIUtilVue.getInstance().apgSha(smsPassword));
					String domaine = "";
					if(utilisateur.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER))
						domaine = "Merci de vous connecter sur https://pms.optima.world";
					String msg="Bonjour, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Poste "+utilisateur.getGroupeUtilisateur().getLibelle()+" Votre identifiant : "+utilisateur.getLogin()+" votre mot de passe : "+smsPassword+". "+domaine;
					utilVue.notifyUtilisateur(utilisateur, msg);
					/*
					 * Notify MAIL
					 */
					String subject = "VOTRE COMPTE APG",header = utilisateur.getEmail();
					
					if("logo-dp".equalsIgnoreCase(utilisateur.getPartner().getLogo()))
						subject  = "VOTRE COMPTE LIMOPAY";
					
					String datas = "";
					String opt = user.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) ? "\"<tr style=\"color:white;\">\n" + 
							 "<th colspan=\"2\" bgcolor=\"black\">Merci de vous connecter sur https://pms.optima.world</th>\n" + 
							 "</tr>\n" : "";
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
					
						messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null, header, subject, msg, datas));
				}else {
					LOG.info("ELSE ICI ICI >>>>>>>>>>>>>>>> "+apgUtilisateurRequest.getIsActive());
					if(user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN_CHECKER_EP)) {
						return Response.ok().entity(new AbstractResponse("0","Not authorized Action"))
								.header(BEConstantes.AccessControlAllowOrigin,"*")
								.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
								.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
								.build();	
					}
					utilisateur.setIsActive(false);
				}
			}
			String newPassword = apgUtilisateurRequest.getNewPassword();
			String confPassword = apgUtilisateurRequest.getConfPassword();
			if(newPassword != null && confPassword != null) {
				if(!newPassword.equals(confPassword)) {
					utilVue.CommonLabel(null,"AdministrationChangePassword",ErrorResponse.SYNTAXE_ERRORS_1807.getCode(),ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("newPassword / confPassword "), newPassword+ " / "+confPassword);
				}
				if(!user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_OPERATION_MANAGER) && !user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_OPERATION_MANAGER)){
					utilVue.CommonLabel(null,"AdministrationChangePassword",ErrorResponse.SYNTAXE_ERRORS_1807.getCode(),ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("newPassword / confPassword "), newPassword+ " / "+confPassword);
				}

				utilisateur.setPassword(APIUtilVue.getInstance().apgSha(newPassword));

				String subject = "YOUR PASSWORD HAS BEEN CHANGED",
						header = utilisateur.getEmail();
				String datas = "";
				String msg = BEConstantes.MESSAGE_HTML+"<table rules=\"all\" cellpadding=\"5px\" style=\"width:500px; border:solid 1px black; font-family:verdana; font-size:12px;\">\n" + 
						"   <tr>\n" + 
						"      <th bgcolor=\"white\" style=\"font-size:large\" colspan=\"2\" align=\"center\">Vos parametres de connexion</th>\n" + 
						"   </tr>\n" + 
						"   <tr style=\"color:white;\">\n" + 
						"      <th bgcolor=\"black\">Nouvel identifiant</th>\n" + 
						"      <th bgcolor=\"black\">Nouveau mot de passe</th>\n" + 
						"   </tr>\n" + 
						"   <tr style=\"color:black;\">\n" + 
						"      <th bgcolor=\"white\"> "+utilisateur.getLogin()+" </th>\n" + 
						"      <th bgcolor=\"white\"> "+newPassword+" </th>\n" + 
						"   </tr>\n" + 
						"</table> ";
				
				
				messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null, header, subject, msg, datas));
			}

			session.updateObject(utilisateur);

			List<APGUtilisateur> lUtilisateurs = new ArrayList<APGUtilisateur>();
			APGUtilisateur apgUtilisateur = new APGUtilisateur();
			apgUtilisateur.setId(utilisateur.getIdUtilisateur());
			apgUtilisateur.setLogin(utilisateur.getLogin());
			apgUtilisateur.setAdresse(utilisateur.getAdresse());
			apgUtilisateur.setEmail(utilisateur.getEmail());
			apgUtilisateur.setNom(utilisateur.getNom());
			apgUtilisateur.setPhone(utilisateur.getPhone());
			apgUtilisateur.setPrenom(utilisateur.getPrenom());
			apgUtilisateur.setProfil(utilisateur.getIdGroupeUtilisateur());
			apgUtilisateur.setIsInit(utilisateur.getIsInit());
			apgUtilisateur.setIsActive(utilisateur.getIsActive());
			apgUtilisateur.setIsDongle(utilisateur.getIsDongle());
			apgUtilisateur.setReference(utilisateur.getReference());
			apgUtilisateur.setIsMasterDealer(utilisateur.getIsMasterDealer());

			lUtilisateurs.add(apgUtilisateur);
			LOG.info("####### Fin Edit Utilisateur from APG >>>>>>>");
			return Response.ok().entity(new APGUtilisateurResponse("0", "OK",lUtilisateurs))
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
