package sn.payway.user.services;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.exception.TransactionException;
import sn.apiapg.common.utils.APGCommonRequest;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.user.object.SessionUserResponse;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationResetNotifToken {
	
	final static String TAG = AdministrationResetNotifToken.class+"";
	public Response Service(String flashcode, APGCommonRequest apgResetPasswordRequest){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			System.out.println("####### Start Reset Password from APG >>>>>>> ");
			Utilisateur USER = null;
			Utilisateur user = null;
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

			System.out.println("####### End Reset Password User >>>>>>>");
			user = (Utilisateur) session.findObjectById(Utilisateur.class, ID, null);
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("user To reset"), user);
			user.setNbToken(0L);

			session.updateObject(user);

			System.out.println("####### Fin APG >>>>>>>");
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
