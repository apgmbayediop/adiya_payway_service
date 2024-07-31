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
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.utils.Constantes;
import sn.adiya.user.object.APGProfil;
import sn.adiya.user.object.APGProfilResponse;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationAddProfil {
	public Response Service(String flashcode,APGCommonRequest apgProfilRequest){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			System.out.println("####### Debut Add Profil from APG >>>>>>>");
			APGProfilResponse response; 
			Utilisateur user = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				user = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, "AdministrationAddProfil", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur : "), user);
			
			String profilCode = apgProfilRequest.getCode();
			utilVue.CommonLabel(null,"AdministrationAddProfil.Service",ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("profilCode : "), profilCode);
			GroupeUtilisateur profil = (GroupeUtilisateur) session.findObjectById(GroupeUtilisateur.class, null, profilCode);
			System.out.println("### >>> --- Parent : "+apgProfilRequest.getParent());
			GroupeUtilisateur parent = (GroupeUtilisateur) session.findObjectById(GroupeUtilisateur.class, null, apgProfilRequest.getParent());
			if(parent == null){
				System.out.println("--- --- Empty Parent --- ---");
				return Response.ok().entity(new AbstractResponse("1", "Parent "+Constantes.NF_TRYAGAIN))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}
			if(profil != null){
				return Response.ok().entity(new AbstractResponse("1", "Profil "+Constantes.F_DUPLICATE))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}else{
				profil = new GroupeUtilisateur();
				profil.setDate(new Date());
				profil.setIsActive(true);
				profil.setIdGroupeUtilisateur(profilCode);
				profil.setLibelle(apgProfilRequest.getLibelle());
				profil.setRang(10);

				profil.setParent(parent);

				profil = (GroupeUtilisateur) session.saveObject(profil); 

				List<APGProfil> lprofils = new ArrayList<APGProfil>();
				APGProfil apgProfil = new APGProfil();
				apgProfil.setCode(profil.getIdGroupeUtilisateur());
				apgProfil.setLibelle(profil.getLibelle());
				if(profil.getParent() != null)
					apgProfil.setParent(profil.getParent().getIdGroupeUtilisateur());

				lprofils.add(apgProfil);

				System.out.println("####### Fin Add Profil from APG >>>>>>>");
				response = new APGProfilResponse("0", "OK",lprofils);
				return Response.ok().entity(response)
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
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
