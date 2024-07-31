package sn.adiya.partner.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import sn.fig.account.entities.DistributeurAccountP;
import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.exception.TransactionException;
import sn.fig.common.utils.APGCommonRequest;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.HistoDecouvert;
import sn.fig.entities.Partner;
import sn.fig.entities.PartnerType;
import sn.fig.entities.PartnerUtilisateur;
import sn.fig.log.entities.LogAccount;
import sn.fig.session.OperationSession;
import sn.fig.session.OperationSessionBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.utils.Constantes;

/**
 * @author WORLD GROUP APG (African Payment Gateway) THE BEST
 * @since 26/06/2018
 * Cheikhouna DIOP Dramane BA Rose Adrienne
 * @version 1.0
 **/

@Stateless
public class AdministrationModPartner {
	final static String TAG = AdministrationModPartner.class+"";

	public Response Service(String flashcode,APGCommonRequest apgPartnerRequest) throws Exception{
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();

			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());

			System.out.println("####### Start Edit Partner from APG >>>>>>>");

			Utilisateur user = null;
			Response globalResponse = utilVue.CommonFlashCode(flashcode);
			if (globalResponse.getEntity() instanceof Utilisateur) {
				user = (Utilisateur) globalResponse.getEntity();
			}
			utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(),ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Utilisateur : "), user);
			Long idPartner = apgPartnerRequest.getId();
			if(idPartner == null){
				return Response.ok().entity(new AbstractResponse("1", "idPartner "+Constantes.NF_TRYAGAIN))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}	
			PartnerType ptt=null;
			if(apgPartnerRequest.getPartnerType() != null){
				String[] params = {"type"};
				Object[] dats = {apgPartnerRequest.getPartnerType()};
				ptt = session.executeNamedQuerySingle(PartnerType.class,"findPartnerTypeByName", params, dats);
			}
			String name = apgPartnerRequest.getName();
			String phone = apgPartnerRequest.getTelephonePartner();
			String canal = apgPartnerRequest.getCanal();
			String countryIsoCode = apgPartnerRequest.getCountryIsoCode();
			String currencyName = apgPartnerRequest.getCurrencyName();
			String email = apgPartnerRequest.getEmailContact();
			String isNotify = apgPartnerRequest.getIsNotify();
			String nomContact = apgPartnerRequest.getNomContact();
			String prenomContact = apgPartnerRequest.getPrenomContact();
			String tel1 = apgPartnerRequest.getTelephoneContact();
			String withOperationCode = apgPartnerRequest.getWithOperationCode();
			String minBalance = apgPartnerRequest.getMinBalance();
			String maxBalance = apgPartnerRequest.getMaxBalance();

			Boolean isDecouvert = apgPartnerRequest.getIsDecouvert();
			BigDecimal balanceDecouvert = apgPartnerRequest.getBalanceDecouvert();
			String logo = apgPartnerRequest.getLogo();
			String numeroCompte = apgPartnerRequest.getNumeroCompte();
			String modeReglement  = apgPartnerRequest.getModeReglement();
			BigDecimal alertBalance = apgPartnerRequest.getAlertBalance();
			String emailBalance = apgPartnerRequest.getEmailBalance();
			String emailCode = apgPartnerRequest.getEmailCode();
			String emailFX = apgPartnerRequest.getEmailFX();
			String emailAlert = apgPartnerRequest.getEmailAlert();

			Partner partner = (Partner) session.findObjectById(Partner.class, idPartner, null);
			if(partner == null){
				return Response.ok().entity(new AbstractResponse("1", "Partner "+Constantes.NF_TRYAGAIN))
						.header(BEConstantes.AccessControlAllowOrigin,"*")
						.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
						.build();
			}
			if(name != null && !name.equals(""))
				partner.setName(name);
			if(phone != null && !phone.equals(""))
				partner.setTelephonePartner(phone);
			if(canal != null && !canal.equals(""))
				partner.setCanal(canal);
			if(countryIsoCode != null && !countryIsoCode.equals(""))
				partner.setCountryIsoCode(countryIsoCode);
			if(currencyName != null && !currencyName.equals(""))
				partner.setCurrencyName(currencyName);
			if(email != null && !email.equals(""))
				partner.setEmailContact(email);

			if(alertBalance != null)
				partner.setAlertBalance(alertBalance);

			if(emailBalance != null && !emailBalance.equals(""))
				partner.setEmailBalance(emailBalance);

			if(emailCode != null && !emailCode.equals(""))
				partner.setEmailCode(emailCode);

			if(emailFX != null && !emailFX.equals(""))
				partner.setEmailFX(emailFX);

			if(emailAlert != null && !emailAlert.equals(""))
				partner.setEmailAlert(emailAlert);

			if(isNotify != null && !isNotify.equals("")){
				if(isNotify.equals("true") || isNotify.equals("1"))
					partner.setIsNotify(true);
				else
					partner.setIsNotify(false);
			}
			if(logo != null && !logo.equals(""))
				partner.setLogo(logo);
			if(prenomContact != null && !prenomContact.equals(""))
				partner.setPrenomContact(prenomContact);
			if(nomContact != null && !nomContact.equals(""))
				partner.setNomContact(nomContact);
			if(tel1 != null && !tel1.equals(""))
				partner.setTelephoneContact(tel1);

			if(apgPartnerRequest.getIsActive() != null) {
				try {
					partner.setIsActive(Boolean.valueOf(apgPartnerRequest.getIsActive()));
				} catch (Exception e) {
					utilVue.CommonLabel("", TAG, ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("minBalance"), null);
				}

			}
			if(ptt != null)
				partner.setPartnerType(ptt);

			partner.setPrefixeWallet(apgPartnerRequest.getPrefixeWallet());

			if(BEConstantes.PARTNER_ACCEPTEUR.equals(partner.getPType())) {
				if(modeReglement != null && !modeReglement.equals(""))
					partner.setModeReglement(apgPartnerRequest.getModeReglement());
				if(numeroCompte != null && !numeroCompte.equals(""))
					partner.setNumeroCompte(apgPartnerRequest.getNumeroCompte());
			}

			if(minBalance != null && !minBalance.equals("")){
				try {
					new BigDecimal(minBalance);
				} catch (Exception e) {
					return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1801.getCode(), ErrorResponse.SYNTAXE_ERRORS_1801.getMessage("minBalance")))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
							.build();
				}
				partner.setMinBalance(new BigDecimal(minBalance));
			}
			if(maxBalance != null && !maxBalance.equals("")){
				try {
					new BigDecimal(maxBalance);
				} catch (Exception e) {
					return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1801.getCode(), ErrorResponse.SYNTAXE_ERRORS_1801.getMessage("maxBalance")))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
							.build();
				}
				partner.setMinBalance(new BigDecimal(minBalance));
			}

			if(Boolean.TRUE.equals(isDecouvert)) {
				if(balanceDecouvert.compareTo(BigDecimal.ZERO) < 0) {
					return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("balanceDecouvert")))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
							.build();
				}
				BigDecimal oldBalanceDecouvert = partner.getBalanceDecouvert()==null?BigDecimal.ZERO:partner.getBalanceDecouvert();
				if(!Boolean.TRUE.equals(Boolean.valueOf(partner.getIsActive()))) {
					return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), partner.getName()+" est desactive partner.getIdPartner() "+partner.getIdPartner()+" / partner.getIsActive() "+Boolean.valueOf(partner.getIsActive())))
							.header(BEConstantes.AccessControlAllowOrigin,"*")
							.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
							.build();
				}
				utilVue.CommonObject(null, TAG, ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("balanceDecouvert"), balanceDecouvert);
		/*		BigDecimal mainBalanceParent = BigDecimal.ZERO;
				if(partner.getParent().getPType().equals(BEConstantes.PARTNER_AGREGATOR) || partner.getParent().getPType().equals(BEConstantes.PARTNER_SUPER_AGREGATOR))
					mainBalanceParent = operationSession.balancePartnerNative(partner.getParent(), partner.getCurrencyName());
				else
					mainBalanceParent = operationSession.balancePartner(partner.getParent());
				
				if(partner.getParent().getPType().equals(BEConstantes.PARTNER_PROVIDER)) {
					session.saveObject(new PartnerAccountP(new Date(), balanceDecouvert, mainBalanceParent.subtract(balanceDecouvert), "Ajout decouvert "+partner.getParent().getName()+"", BEConstantes.OPERATION_PARTNER_PROVIDER, partner.getParent().getPrincipalAccountNumber(), true, partner.getParent().getCurrencyName(), partner.getParent().getCode(), ""));
				}else if(partner.getParent().getPType().equals(BEConstantes.PARTNER_AGREGATOR)) {
					session.saveObject(new AgregatorAccount(new Date(), balanceDecouvert, mainBalanceParent.subtract(balanceDecouvert), "Ajout decouvert "+partner.getParent().getName()+"", BEConstantes.OPERATION_PARTNER_AG, partner.getParent().getPrincipalAccountNumber(), true, partner.getParent().getCurrencyName(), partner.getParent().getCode(), ""));
				}	*/
				String[] params = {"idUtilisateur"};
				Object[] dats = {user.getIdUtilisateur()};
				List<PartnerUtilisateur> lPartnerUtilisateurs = session.executeNamedQueryList(PartnerUtilisateur.class,"findValidatedEntiteByUtilisateur", params, dats);
				if(!lPartnerUtilisateurs.isEmpty()) {
					PartnerUtilisateur partnerUtilisateur = lPartnerUtilisateurs.get(0);
					Partner part = partnerUtilisateur.getPartner();
					if(!part.getPType().equals(BEConstantes.PARTNER_PROVIDER)) {
						return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage(part.getName()+" doit etre un "+BEConstantes.PARTNER_PROVIDER)))
								.header(BEConstantes.AccessControlAllowOrigin,"*")
								.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
								.build();
					}
					if(!partner.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR)) {
						return Response.ok().entity(new AbstractResponse(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage(partner.getName()+" doit etre un "+BEConstantes.PARTNER_DISTRIBUTEUR)))
								.header(BEConstantes.AccessControlAllowOrigin,"*")
								.header(BEConstantes.AccessControlAllowMethods, BEConstantes.AccessControlAllowMethodsVALUE)
								.build();
					}
					partner.setIsDecouvert(isDecouvert);
					partner.setBalanceDecouvert(balanceDecouvert);
					String description = "Activation decouvert montant "+(partner.getBalanceDecouvert()== null ? balanceDecouvert : partner.getBalanceDecouvert().add(balanceDecouvert))+"  idUtilisateur "+user.getIdUtilisateur()+" "+user.getPrenom()+" "+user.getNom()+" a la date du "+new Date()+" ";
					session.saveObject(new DistributeurAccountP(new Date(), BigDecimal.ZERO, operationSession.balancePartner(partner), BigDecimal.ZERO, description , BEConstantes.OPERATION_DISTRIBUTEUR, partner.getPrincipalAccountNumber(), BigDecimal.ZERO, true, partner.getCurrencyName(), partner.getCode(), user.getIdUtilisateur()+"", "",BigDecimal.ZERO));

					session.saveObject(new HistoDecouvert(new Date(), balanceDecouvert, oldBalanceDecouvert, partner,user));

					LogAccount logAccount = new LogAccount(new Date(), partner.getConsumerId(), user.getIdUtilisateur()+"", partner.getIdPartner()+"", TAG, "addDecouvert");
					session.logAccount(logAccount);
				}else if(user.getIdGroupeUtilisateur().equals("ADMIN")){
					partner.setIsDecouvert(isDecouvert);
					partner.setBalanceDecouvert(balanceDecouvert);
					session.saveObject(new HistoDecouvert(new Date(), balanceDecouvert, oldBalanceDecouvert, partner,user));

					LogAccount logAccount = new LogAccount(new Date(), partner.getConsumerId(), user.getIdUtilisateur()+"", partner.getIdPartner()+"", TAG, "addDecouvert");
					session.logAccount(logAccount);
				}
			}
			if(withOperationCode != null){
				if(withOperationCode.equals("true") || withOperationCode.equals("1"))
					partner.setWithOperationCode(true);
				else
					partner.setWithOperationCode(false);
			}
			session.updateObject(partner);
			List<APGPartner> lpts = new ArrayList<APGPartner>();
			APGPartner pt = new APGPartner();
			pt.setId(partner.getIdPartner());
			pt.setName(partner.getName());
			//		pt.setPrincipalAccountNumber(partner.getPrincipalAccountNumber());
			pt.setConsumerId(partner.getConsumerId());
			pt.setEmail(partner.getEmailContact());
			if(partner.getPartnerType() != null){
				pt.setPartnerType(partner.getPType()+"");
				pt.setPartnerTypeId(partner.getPartnerType().getIdPartnerType()+"");
			}
			pt.setNomContact(partner.getNomContact());
			pt.setPrenomContact(partner.getPrenomContact());
			pt.setTelephonePartner(partner.getTelephonePartner());
			pt.setTelephoneContact(partner.getTelephoneContact());
			pt.setAlertBalance(partner.getAlertBalance());
			pt.setEmailBalance(partner.getEmailBalance());
			pt.setEmailCode(partner.getEmailCode());
			pt.setEmailFX(partner.getEmailFX());
			pt.setEmailAlert(partner.getEmailAlert());
			//	pt.setIsNotify(partner.getIsNotify());
			/*
			if(partner.getWithOperationCode() != null && !partner.getWithOperationCode())
				pt.setWithOperationCode(true);
			else
				pt.setWithOperationCode(false);
			 */

			pt.setPrefixeWallet(partner.getPrefixeWallet());
			pt.setLogo(logo);
			pt.setActive(partner.getIsActive());

			lpts.add(pt);

			System.out.println("####### Fin Edit Partner from APG >>>>>>>");
			return Response.ok().entity(new APGPartnerResponse("0", "OK",lpts))
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
