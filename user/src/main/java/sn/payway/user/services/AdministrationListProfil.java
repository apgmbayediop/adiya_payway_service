package sn.payway.user.services;

import java.util.ArrayList;
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
public class AdministrationListProfil {
	final static String TAG = AdministrationListProfil.class+"";
	final static Logger LOG = Logger.getLogger(AdministrationListProfil.class);
	public Response Service(String flashcode,APGCommonRequest apgProfilRequest){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			LOG.info("####### Debut List Profil from APG >>>>>>>");
			APGProfilResponse response; 
			Utilisateur USER = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);
			if(USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_SUPER_ADMIN) || USER.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_ADMIN)) {
				List<GroupeUtilisateur> lProfils = session.findAllObject(GroupeUtilisateur.class);
				List<APGProfil> lprofils = new ArrayList<APGProfil>();
				for(GroupeUtilisateur profil : lProfils){  
					APGProfil pfil = new APGProfil();
					pfil.setCode(profil.getIdGroupeUtilisateur());
					pfil.setLibelle(profil.getLibelle());
					if(profil.getParent() != null)
						pfil.setParent(profil.getParent().getIdGroupeUtilisateur());
					pfil.setRang(profil.getRang()+"");

					lprofils.add(pfil);
				}
				LOG.info("####### Fin List all Profil from APG >>>>>>>");
				response = new APGProfilResponse("0", "OK",lprofils);
				return Response.ok().entity(response)
						.header(BEConstantes.AccessControlAllowOrigin, "*")
						.header("Access-Control-Allow-Credentials", "true")
						.header(BEConstantes.AccessControlAllowHeaders,"origin, content-type, accept, authorization")
						.header(BEConstantes.AccessControlAllowMethods, "GET, POST, PUT, DELETE, OPTIONS, HEAD")			
						.build();
			}else if(apgProfilRequest.getCode() == null || apgProfilRequest.getCode().equals("")){
				LOG.info("### --- --- - Espace "+USER.getGroupeUtilisateur().getEspace());
				LOG.info("### --- --- - Rang "+USER.getGroupeUtilisateur().getRang());

				String[] parameters = {"rang", "espace"};
				Object[] data = { USER.getGroupeUtilisateur().getRang(),USER.getGroupeUtilisateur().getEspace()};
				List<GroupeUtilisateur> lProfil = (List<GroupeUtilisateur>) session.executeNamedQueryList(GroupeUtilisateur.class,"findGroupeUtilisateur", parameters, data);
				List<APGProfil> lprofils = new ArrayList<APGProfil>();
				for(GroupeUtilisateur profil : lProfil){  
					APGProfil pfil = new APGProfil();
					pfil.setCode(profil.getIdGroupeUtilisateur());
					pfil.setLibelle(profil.getLibelle());
					if(profil.getParent() != null) pfil.setParent(profil.getParent().getIdGroupeUtilisateur());
					pfil.setRang(profil.getRang()+"");

					lprofils.add(pfil);
				}
				LOG.info("####### Fin List all Profil from APG >>>>>>>");
				response = new APGProfilResponse("0", "OK",lprofils);
				return Response.ok().entity(response)
						.header(BEConstantes.AccessControlAllowOrigin, "*")
						.header("Access-Control-Allow-Credentials", "true")
						.header(BEConstantes.AccessControlAllowHeaders,"origin, content-type, accept, authorization")
						.header(BEConstantes.AccessControlAllowMethods, "GET, POST, PUT, DELETE, OPTIONS, HEAD")			
						.build();			
			}else{
				String profilId = apgProfilRequest.getCode();
				if(profilId != null){
					GroupeUtilisateur profil = (GroupeUtilisateur) session.findObjectById(GroupeUtilisateur.class, null, apgProfilRequest.getCode());
					List<APGProfil> lprofils = new ArrayList<APGProfil>();
					APGProfil pFil = new APGProfil();
					pFil.setCode(profil.getIdGroupeUtilisateur());
					pFil.setLibelle(profil.getLibelle());
					if(profil.getParent() != null)
						pFil.setParent(profil.getParent().getIdGroupeUtilisateur());
					pFil.setRang(profil.getRang()+"");

					lprofils.add(pFil);

					LOG.info("####### Fin List Profil from APG >>>>>>>");
					response = new APGProfilResponse("0", "OK",lprofils);
					return Response.ok().entity(response)
							.header(BEConstantes.AccessControlAllowOrigin, "*")
							.header("Access-Control-Allow-Credentials", "true")
							.header(BEConstantes.AccessControlAllowHeaders,"origin, content-type, accept, authorization")
							.header(BEConstantes.AccessControlAllowMethods, "GET, POST, PUT, DELETE, OPTIONS, HEAD")			
							.build();	
				}else{
					return Response.ok().entity(new AbstractResponse("1", "profilId "+Constantes.NF_TRYAGAIN))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
							.build();
				}
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
