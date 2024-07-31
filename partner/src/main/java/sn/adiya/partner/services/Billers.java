package sn.adiya.partner.services;

import java.io.UnsupportedEncodingException;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.exception.TransactionException;
import sn.fig.common.utils.APGCommonRequest;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.Partner;
import sn.fig.entities.Wallet;
import sn.fig.session.OperationSession;
import sn.fig.session.OperationSessionBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.utils.APGUtilVue;

@Stateless
public class Billers {
	public Response Service(String auth,APGCommonRequest apgBillerRequest) throws UnsupportedEncodingException{
		try {
			APIUtilVue utilVue = APIUtilVue.getInstance();
			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			System.out.println("####### Start Biller >>>>>>>");
			String from = apgBillerRequest.getFromCountry();
			utilVue.CommonLabel(null,"Billers",ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("from : "), from);
			if(auth != null) {
				Partner partner = operationSession.findHmacKey(utilVue.apgCrypt(auth));
				utilVue.CommonObject(null,"Billers",ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("partner : "), partner);
			}else {
				String wallet = apgBillerRequest.getFromWalletId();
				utilVue.CommonObject(null, "WalletHistory", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Wallet"), wallet);
				String pin = apgBillerRequest.getPin();
				utilVue.CommonObject(null, "WalletHistory", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("pin"), pin);
				String parameters[] = {"wallet", "pin"};
				Object [] data = {wallet,utilVue.apgSha(wallet+pin)};
				Wallet walt = (Wallet) session.executeNamedQuerySingle(Wallet.class,"findLoginWallet", parameters, data);
				utilVue.CommonObject(null, "WalletHistory", ErrorResponse.SYNTAXE_ERRORS_1709.getCode(), ErrorResponse.SYNTAXE_ERRORS_1709.getMessage("Wallet"), walt);
			}
			System.out.println("####### End Biller >>>>>>>");
			//return Response.ok().entity(new APGListBillerResponse(ErrorResponse.REPONSE_SUCCESS.getCode(), ErrorResponse.REPONSE_SUCCESS.getMessage(""), APGUtilVue.getInstance().SwitchBiller(from)))
			return Response.ok().entity(new APGListBillerResponse(ErrorResponse.REPONSE_SUCCESS.getCode(), ErrorResponse.REPONSE_SUCCESS.getMessage(""), APGUtilVue.getInstance().SwitchBillers(from)))
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
