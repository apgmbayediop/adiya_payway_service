package sn.payway.user.services;

import javax.ejb.Stateless;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.utils.PaywayException;

@Stateless
public class UserManager {

	
	    public Utilisateur verifyFlashcode(String flashcode) throws PaywayException {
	    		if(flashcode == null || flashcode.isBlank()) {
	    			throw new PaywayException(ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("flashcode : "));
	    		}
	    		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
	    		String[] parameters = {"flashCode"};
				String[] data = {flashcode};
				Utilisateur utilisateur = session.executeNamedQuerySingle(Utilisateur.class,"findByFlashCode", parameters, data) ;
			
			if(utilisateur == null){	
				throw new PaywayException(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("flashcode not found"));
			}
			if(utilisateur.getIsActive()) {
			   return utilisateur;	
			}else {
				throw new PaywayException(ErrorResponse.SYNTAXE_ERRORS_1802.getCode(), ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("Inactive account"));
			}
			
	    }
}
