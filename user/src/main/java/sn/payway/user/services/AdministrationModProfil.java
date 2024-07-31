package sn.payway.user.services;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import sn.apiapg.common.entities.GroupeUtilisateur;
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
import sn.payway.common.utils.Constantes;
import sn.payway.user.object.APGProfil;
import sn.payway.user.object.APGProfilResponse;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationModProfil {
	public Response Service(String flashcode,APGCommonRequest apgProfilRequest){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			System.out.println("####### Start Edit Profil from APG >>>>>>>");

			Utilisateur user = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				user = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, "AdministrationModProfil", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur : "), user);

			String codeProfil = apgProfilRequest.getCode();
			utilVue.CommonLabel(null,"AdministrationModProfil",ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("codeProfil : "), codeProfil);
			String libelle = apgProfilRequest.getLibelle();
			GroupeUtilisateur profil = (GroupeUtilisateur) session.findObjectById(GroupeUtilisateur.class, null, codeProfil);
			if(profil == null){
				return Response.ok().entity(new AbstractResponse("1", "Profil "+Constantes.NF_TRYAGAIN))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}else{
				String rang = apgProfilRequest.getRang();
				if(rang != null && !rang.equals(""))
					profil.setRang(Integer.parseInt(rang));
				if(libelle != null && !libelle.equals(""))
					profil.setLibelle(libelle);

				session.updateObject(profil);

				List<APGProfil> lprofils = new ArrayList<APGProfil>();
				APGProfil apgProfil = new APGProfil();
				apgProfil.setCode(profil.getIdGroupeUtilisateur());
				apgProfil.setLibelle(profil.getLibelle());
				apgProfil.setRang(profil.getRang()+"");
				if(profil.getParent() != null)
					apgProfil.setParent(profil.getParent().getIdGroupeUtilisateur());

				lprofils.add(apgProfil);

				System.out.println("####### Fin Edit Profil from APG >>>>>>>");
				return Response.ok().entity(new APGProfilResponse("0", "OK",lprofils))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();	
			}
		} catch (TransactionException e) {
			return Response.ok().entity(new AbstractResponse(e.getCode(), e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();	
		}
	}

}
