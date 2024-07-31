package sn.adiya.user.services;

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
import sn.fig.common.utils.MailUtils;
import sn.fig.common.utils.StringOperation;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.user.object.SessionUserResponse;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationResetPassword {
	
	static final Logger LOG = Logger.getLogger(AdministrationResetPassword.class);
	static final String TAG = AdministrationResetPassword.class.getName();
			
	public Response Service(String flashcode, APGCommonRequest apgResetPasswordRequest){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			//MessageSenderService messageSenderService = (MessageSenderService) BeanLocator.lookUp(MessageSenderServiceBean.class.getSimpleName());

			LOG.info("####### Start Reset Password from APG >>>>>>> ");
			Utilisateur USER = null;
			Utilisateur user = null;
			if(flashcode != null) {
				Response globalResponse = utilVue.CommonFlashCode(flashcode);
				if (globalResponse.getEntity() instanceof Utilisateur) {
					USER = (Utilisateur) globalResponse.getEntity();
				}
				utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);

				Long ID = 0L;
				try {
					ID = apgResetPasswordRequest.getId();
					if(ID == null || ID == 0L){
						return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1802.getCode(), ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("User Id")))
								.header(BEConstantes.AccessControlAllowOrigin,"*")
								.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
								.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
								.build();
					}
				} catch (NumberFormatException e1) {
					return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1801.getCode(), ErrorResponse.SYNTAXE_ERRORS_1801.getMessage("number")))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
							.build();
				}

				LOG.info("####### End Reset Password User >>>>>>>");
				user = (Utilisateur) session.findObjectById(Utilisateur.class, ID, null);
				if(user.getPartner().getPType().equals(BEConstantes.PARTNER_SENDER_PAYER)){
					String profile[] = {BEConstantes.GROUPE_SUPER_ADMIN_EP,BEConstantes.GROUPE_OPERATION_MANAGER};
					utilVue.commonVerifyProfil(USER, TAG, profile);
					if(USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN_EP)) {
						LOG.info("---------------------PROFILE AMMALLAH----------------------------"+USER.getIdGroupeUtilisateur());
						LOG.info("---------------------PROFILE AMMALLAH----------------------------"+USER.getIdGroupeUtilisateur());
						String otp = apgResetPasswordRequest.getOtp();
						LOG.info("### --- --- OTP SUPER ADMIN: "+otp);
						utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("This otp does not match the expected user"), otp);
						String[] params = {"flashCode","otp"};
						Object[] dats = {flashcode,otp};
						Utilisateur utilisateur = session.executeNamedQuerySingle(Utilisateur.class,"findByOTP", params, dats);
						utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), utilisateur);
					}
				}
			}
			else {
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("login"), apgResetPasswordRequest.getLogin());
				String[] parameters = {"login","isActive"};
				Object[] data = {apgResetPasswordRequest.getLogin(),true};
				user = session.executeNamedQuerySingle(Utilisateur.class, "findByLoginAgent", parameters, data);
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("user To reset"), user);
			String smsPassword = StringOperation.getPasswwd();
			user.setPassword(APIUtilVue.getInstance().apgSha(smsPassword));
			user.setFirst(true);
			user.setNbPassword(0L);
			user.setNbToken(0L);

			session.updateObject(user);
			
			String msg="Bonjour, votre identifiant "+user.getLogin()+" et nouveau mot de passe est "+smsPassword;
			if(user.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) || user.getPartner().getPType().equals(BEConstantes.PARTNER_ACCEPTEUR)) {
				msg = "Bonjour, vos parametres de connexion Optima Business sont : Identifiant / "+user.getLogin()+", Nouveau Mot de passe / "+smsPassword+". "
						+ "Vous pouvez telecharger l'application Optima Business en cliquant sur https://bit.ly/3tSxaZU";
			}
			
			APIUtilVue.getInstance().notifyUtilisateur(user, msg);
			String subject = "YOUR PASSWORD HAS BEEN RESET",
					header = user.getEmail();
			String datas = "";
			String lien = "https://pms.optima.world";
			String opt = "";
			if(user.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER)  || (user.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_PARTENAIRE_ECOMMERCE))) {
			opt = "\"<tr style=\"color:white;\">\n" + 
					 "<th colspan=\"2\" bgcolor=\"black\">Merci de vous connecter sur "+lien+"</th>\n" + 
					 "</tr>\n" ;
			}
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
					"      <th bgcolor=\"white\"> "+user.getLogin()+" </th>\n" + 
					"      <th bgcolor=\"white\"> "+smsPassword+" </th>\n" + 
					"   </tr>\n" + 
					"</table> ";
			
				MailUtils.sendEmails(header, subject, msg, true, null, datas);
			
			LOG.info("####### Fin RESET Password from APG >>>>>>>");
			SessionUserResponse response = new SessionUserResponse(ErrorResponse.REPONSE_SUCCESS.getCode(), "OK",
					user.getPrenom(), 
					user.getNom(), 
					user.getEmail(), 
					user.getPhone(), 
					user.getIdGroupeUtilisateur(), 
					user.getGroupeUtilisateur().getLibelle(), 
					user.getIdUtilisateur()+"", 
					user.getAdresse(), 
					user.getGenre(), 
					user.getLogin(),
					user.getFirst(),
					user.getPartner().getCountryIsoCode(),
					user.getPartner().getIdPartner(),
					user.getPartner().getCode(),
					user.getMatricule(),
					user.getPartner().getLogo(),
					user.getPartner().getPType(),
					user.getPartner().getPrefixeWallet());
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
