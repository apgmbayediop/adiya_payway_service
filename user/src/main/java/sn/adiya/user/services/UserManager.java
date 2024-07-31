package sn.adiya.user.services;

import javax.ejb.Stateless;

import sn.adiya.common.utils.AdiyaException;
import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.BeanLocator;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;

@Stateless
public class UserManager {

	
	    public Utilisateur verifyFlashcode(String flashcode) throws AdiyaException {
	    		if(flashcode == null || flashcode.isBlank()) {
	    			throw new AdiyaException(ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("flashcode : "));
	    		}
	    		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
	    		String[] parameters = {"flashCode"};
				String[] data = {flashcode};
				Utilisateur utilisateur = session.executeNamedQuerySingle(Utilisateur.class,"findByFlashCode", parameters, data) ;
			
			if(utilisateur == null){	
				throw new AdiyaException(ErrorResponse.SYNTAXE_ERRORS_1807.getCode(), ErrorResponse.SYNTAXE_ERRORS_1807.getMessage("flashcode not found"));
			}
			if(utilisateur.getIsActive()) {
			   return utilisateur;	
			}else {
				throw new AdiyaException(ErrorResponse.SYNTAXE_ERRORS_1802.getCode(), ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("Inactive account"));
			}
			
	    }
}
