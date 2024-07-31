package sn.payway.merchant.services;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;

import org.jboss.logging.Logger;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.aci.Terminal;
import sn.apiapg.lis.utils.SysVar;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.merchant.dto.PosResponse;
import sn.payway.merchant.dto.TerminalDto;

@Stateless
public class TerminalService {

	private static final String OLNPAY="OLNPAY";
	private static final Logger log = Logger.getLogger(TerminalService.class);
	
	public PosResponse create(Long idUser, TerminalDto request) {
		PosResponse resp =new PosResponse();
		try {
			log.info("**********createTerminal**********");
			log.info("user = " + idUser);
			log.info(request.toString());
			 Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			
			 String numeroSerie =request.isVirtuel()?generateNumeroSerie(session):request.getNumeroSerie();
			 Terminal terminal = new Terminal();
			 Partner partner = request.getPartner()== null?null:session.findObjectById(Partner.class, request.getPartner(), null);
			
			Date now = new Date();
			Utilisateur user = session.findObjectById(Utilisateur.class, idUser, null);
			terminal.setNumeroSerie(numeroSerie);
			terminal.setDesignation(request.getDesignation());
			terminal.setVesionApplication(request.getVersionApplication());
			terminal.setDateCreation(now);
			terminal.setDateLastModification(now);
			terminal.setTypeOperation(SysVar.TypeOperation.INSERT.name());
			terminal.setUserCreation(user);
			terminal.setUserLastModification(user);
			terminal.setPartner(partner);
			session.saveObject(terminal);
			resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
			resp.setSerialNumber(numeroSerie);
			log.info("********* Fin createTerminal ****************");
			
		} 
		catch (Exception e) {
			log.info("errorCrTerminal", e);
			resp.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			resp.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		return resp;
	}
	
	private String generateNumeroSerie( Session session) {
	 List<Terminal> terminals = session.executeNamedQueryList(Terminal.class, "Terminal.findByNumeroSerie",
				new String[] {PosService.NUM_SERIE }, 
				new String[] { "%"+OLNPAY+"%"});
		String index="0";
		if(terminals !=null &&!terminals.isEmpty()) {
			index = terminals.get(0).getNumeroSerie();
		}
		index =index.replace(OLNPAY, "");
		int id = Integer.parseInt(index)+1;
		String numeroSerie = "0000000000" + id;
		return OLNPAY+numeroSerie.substring(numeroSerie.length() - 10);
	}
}
