package sn.payway.partner.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.FileOperation;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.PartnerUtilisateur;
import sn.apiapg.session.OperationSession;
import sn.apiapg.session.OperationSessionBean;
import sn.apiapg.session.ServiceSession;
import sn.apiapg.session.ServiceSessionBean;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationListPartner {
	public Response Service(String flashcode, String auth, String id){
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName()); 
			ServiceSession serviceSession = (ServiceSession) BeanLocator.lookUp(ServiceSessionBean.class.getSimpleName());
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			System.out.println("####### Debut List Partner from APG >>>>>>>");
			Utilisateur USER = null;
			System.out.println("### --- --- - Auth :  *** * *** *  "+auth);   
			System.out.println("### --- --- - flashcode :  *** * *** *  "+flashcode);
			System.out.println("### --- --- - Id :  *** * *** *  "+id);

			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				USER = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, "AdministrationListPartner", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur"), USER);
			Partner part = operationSession.findHmacKey(utilVue.apgCrypt(auth));
			utilVue.CommonObject(null, "AdministrationListPartner", ErrorResponse.AUTHENTICATION_ERRORS_1707.getCode(), ErrorResponse.AUTHENTICATION_ERRORS_1707.getMessage("Partner"), part);
			if(id == null || Long.parseLong(id) == 0L || id.equals("")){
				List<Partner> lPartner = new ArrayList<Partner>();
				if(part.getCode().equals(BEConstantes.CODE_APG)) {
					String[] param = {"type"};

					Object[] datA = {BEConstantes.PARTNER_AGREGATOR};
					List<Partner> lAgregators = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", param, datA);
					lAgregators.add(part);
					lPartner.addAll(lAgregators);

					Object[] datSender = {BEConstantes.PARTNER_SENDER};
					List<Partner> lSenders = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", param, datSender);
					lPartner.addAll(lSenders);

					Object[] datPY = {BEConstantes.PARTNER_PAYER};
					List<Partner> lPYs = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", param, datPY);
					lPartner.addAll(lPYs);

					Object[] datSenderPayer = {BEConstantes.PARTNER_SENDER_PAYER};
					List<Partner> lSenderPayers = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", param, datSenderPayer);
					lPartner.addAll(lSenderPayers);

					Object[] datProvider = {BEConstantes.PARTNER_PROVIDER};
					List<Partner> lProviders = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", param, datProvider);
					lPartner.addAll(lProviders);

					Object[] datMonetic = {BEConstantes.PARTNER_MONETIC};
					List<Partner> lMonetics = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", param, datMonetic);
					lPartner.addAll(lMonetics);

					Object[] datEmetteur = {BEConstantes.PARTNER_EMETTEUR};
					List<Partner> lEmetteurs = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", param, datEmetteur);
					lPartner.addAll(lEmetteurs);

					Object[] datAccepteur = {BEConstantes.PARTNER_ACCEPTEUR};
					List<Partner> lAccepteurs = (List<Partner>) session.executeNamedQueryList(Partner.class,"findAllPartnerByType", param, datAccepteur);
					lPartner.addAll(lAccepteurs);
					
				}else if(part.getPType().equals(BEConstantes.PARTNER_AGREGATOR)) { 
					String[] paramFilsA = {"type", "idPartner"};
					Object[] datSenderFilsA = {BEConstantes.PARTNER_SENDER, part.getIdPartner()};
					List<Partner> lSenders = (List<Partner>) session.executeNamedQueryList(Partner.class,"findEPandPfilsByType", paramFilsA, datSenderFilsA);
					lSenders.add(part);
					lPartner.addAll(lSenders);
					Object[] datPayerFilsA = {BEConstantes.PARTNER_PAYER, part.getIdPartner()};
					List<Partner> lPYs = (List<Partner>) session.executeNamedQueryList(Partner.class,"findEPandPfilsByType", paramFilsA, datPayerFilsA);
					lPartner.addAll(lPYs);
					Object[] datSenderPayerFilsA = {BEConstantes.PARTNER_SENDER_PAYER, part.getIdPartner()};
					List<Partner> lSenderPayers = (List<Partner>) session.executeNamedQueryList(Partner.class,"findEPandPfilsByType", paramFilsA, datSenderPayerFilsA);
					lPartner.addAll(lSenderPayers);
					Object[] datProviderFilsA = {BEConstantes.PARTNER_PROVIDER, part.getIdPartner()};
					List<Partner> lProviders = (List<Partner>) session.executeNamedQueryList(Partner.class,"findEPandPfilsByType", paramFilsA, datProviderFilsA);
					lPartner.addAll(lProviders);
					Object[] datMoneticFilsA = {BEConstantes.PARTNER_MONETIC, part.getIdPartner()};
					List<Partner> lMonetics = (List<Partner>) session.executeNamedQueryList(Partner.class,"findEPandPfilsByType", paramFilsA, datMoneticFilsA);
					lPartner.addAll(lMonetics);
					Object[] datEmetteurFilsA = {BEConstantes.PARTNER_EMETTEUR, part.getIdPartner()};
					List<Partner> lEmetteurs = (List<Partner>) session.executeNamedQueryList(Partner.class,"findEPandPfilsByType", paramFilsA, datEmetteurFilsA);
					lPartner.addAll(lEmetteurs);
				}
				List<APGPartner> lpts = new ArrayList<APGPartner>();
				for(Partner partner : lPartner){
					boolean isCreated = FileOperation.createDirectory("partners",partner.getName().trim().replace(" ", "_").toLowerCase());
					if(isCreated)
						System.out.println("FOLDER PARTNER >>>>>>>>>>>>> "+partner.getName()+" CREATED");
					APGPartner pt = new APGPartner();
					pt.setId(partner.getIdPartner());
					pt.setConsumerId(partner.getConsumerId());
					pt.setAuth(utilVue.apgDeCrypt(partner.getToken()));
					pt.setName(partner.getName());
					pt.setTelephonePartner(partner.getTelephonePartner());
					pt.setTelephoneContact(partner.getTelephoneContact());
					pt.setEmail(partner.getEmailContact());
					pt.setNomContact(partner.getNomContact());
					pt.setPrenomContact(partner.getPrenomContact());
					pt.setCountryIsoCode(partner.getCountryIsoCode());
					pt.setCurrencyName(partner.getCurrencyName());
					pt.setCountryIsoCode(partner.getCountryIsoCode());
					pt.setCountryId("");
					pt.setAdresse(partner.getAdresse());
					pt.setType(partner.getFilsDistributeur());
					pt.setIsDecouvert(partner.getIsDecouvert());
					pt.setBalanceDecouvert(partner.getBalanceDecouvert());
					BigDecimal mainBalance = BigDecimal.ZERO;
					{
						mainBalance = BigDecimal.ZERO;
						if(!partner.getPType().equals(BEConstantes.PARTNER_SUPER_AGREGATOR) && !partner.getPType().equals(BEConstantes.PARTNER_AGREGATOR))
							mainBalance = operationSession.balancePartner(partner);
						else
							mainBalance = serviceSession.balancePivot(partner.getPrincipalAccountNumber(), partner.getCurrencyName());
					}

					pt.setBalance(mainBalance+"");
					pt.setPartnerType(partner.getPType());
					pt.setPartnerTypeId("");
					pt.setPartnerCode(partner.getCode());
					pt.setIsOpened(partner.getIsOpened());
					pt.setPrefixeWallet(partner.getPrefixeWallet());
					pt.setActive(partner.getIsActive());

					lpts.add(pt);
				}
				System.out.println("####### Fin List all Partner from APG >>>>>>>");
				return Response.ok().entity(new APGPartnerResponse("0", "OK",lpts))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();	
			}else{
				try {
					Long.parseLong(id);
				} catch (NumberFormatException e) {
					return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1801.getCode(), ErrorResponse.SYNTAXE_ERRORS_1801.getMessage("partner Id")))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)    
							.build();
				}
				Partner partner = (Partner) session.findObjectById(Partner.class, Long.parseLong(id), null);
				List<APGPartner> lps = new ArrayList<APGPartner>();
				if(partner.getPType().equals(BEConstantes.PARTNER_SUPER_AGREGATOR) ||
						partner.getPType().equals(BEConstantes.PARTNER_AGREGATOR) || 
						partner.getPType().equals(BEConstantes.PARTNER_SENDER) || 
						partner.getPType().equals(BEConstantes.PARTNER_PAYER) || 
						partner.getPType().equals(BEConstantes.PARTNER_SENDER_PAYER) || 
						partner.getPType().equals(BEConstantes.PARTNER_MONETIC) || 
						partner.getPType().equals(BEConstantes.PARTNER_PROVIDER) || 
						partner.getPType().equals(BEConstantes.PARTNER_EMETTEUR) || 
						partner.getPType().equals(BEConstantes.PARTNER_ACCEPTEUR)
						|| partner.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR)
						|| partner.getPType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)
						|| partner.getPType().equals(BEConstantes.PARTNER_AGENCE)
						|| partner.getPType().equals(BEConstantes.PARTNER_CAISSE)) {
					APGPartner pt = new APGPartner();
					pt.setId(partner.getIdPartner());
					pt.setConsumerId(partner.getConsumerId());
					pt.setName(partner.getName());
					pt.setTelephonePartner(partner.getTelephonePartner());
					pt.setEmail(partner.getEmailContact());
					pt.setNomContact(partner.getNomContact());
					pt.setPrenomContact(partner.getPrenomContact());
					pt.setTelephoneContact(partner.getTelephoneContact());
					pt.setCountryIsoCode(partner.getCountryIsoCode());
					pt.setCurrencyName(partner.getCurrencyName());
					pt.setIsDecouvert(partner.getIsDecouvert());
					pt.setBalanceDecouvert(partner.getBalanceDecouvert());
					pt.setCountryId("");
					BigDecimal mainBalance = BigDecimal.ZERO;
					if(!partner.getPType().equals(BEConstantes.PARTNER_SUPER_AGREGATOR) && !partner.getPType().equals(BEConstantes.PARTNER_AGREGATOR))
						mainBalance = operationSession.balancePartner(partner);
					else
						mainBalance = serviceSession.balancePivot(partner.getPrincipalAccountNumber(), partner.getCurrencyName());

					pt.setBalance(mainBalance+"");
					pt.setPartnerType(partner.getPType());
					pt.setPartnerTypeId("");
					pt.setPartnerCode(partner.getCode());
					pt.setAdresse(partner.getAdresse());
					pt.setIsOpened(partner.getIsOpened());
					pt.setType(partner.getFilsDistributeur());
					pt.setCodeAgence(partner.getCodeAgence());
					pt.setPrefixeWallet(partner.getPrefixeWallet());
					pt.setActive(partner.getIsActive());
					pt.setIsB2B(partner.getIsB2B());
					String param[] = {"idPartner"};
					Object data3[] = {partner.getIdPartner()};
					PartnerUtilisateur pu = session.executeNamedQuerySingle(PartnerUtilisateur.class, "findUtilisateurByEntite", param, data3);
					if(pu != null && pu.getIsValidated() != null && pu.getIsValidated()) {
						pt.setIsAssigned(true);
						pt.setAssignedUserId(pu.getUtilisateur().getIdUtilisateur());
						pt.setAssignedUserFirstName(pu.getUtilisateur().getPrenom());
						pt.setAssignedUserLastName(pu.getUtilisateur().getNom());
						pt.setAssignedUserPhone(pu.getUtilisateur().getPhone());
					}
					
					lps.add(pt);
				}
				System.out.println("####### Fin List Partner by ID from APG >>>>>>>");
				return Response.ok().entity(new APGPartnerResponse("0", "OK",lps))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();	
			}
		}catch (Exception e) {
			e.printStackTrace();
			return Response.ok().entity(new AbstractResponse("1", e.getMessage()))
					.header(BEConstantes.AccessControlAllowOrigin,"*")
					.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
					.header(BEConstantes.AccessControlAllowHeaders, BEConstantes.AccessControlAllowHeadersVALUE)
					.build();	
		}
	}

}
