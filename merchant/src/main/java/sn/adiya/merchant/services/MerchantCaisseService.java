package sn.adiya.merchant.services;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.BeanLocator;
import sn.fig.common.utils.ClientHSM;
import sn.fig.common.utils.MailUtils;
import sn.fig.entities.Partner;
import sn.fig.entities.aci.Caisse;
import sn.fig.entities.aci.PointDeVente;
import sn.fig.entities.aci.Terminal;
import sn.fig.lis.utils.SysVar;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.merchant.dto.CaisseDto;
import sn.adiya.merchant.dto.PosResponse;
import sn.adiya.merchant.dto.TerminalDto;

@Stateless
public class MerchantCaisseService {

	private static final Logger log = Logger.getLogger(MerchantCaisseService.class);
	@Inject
	TerminalService terminalService;

	public PosResponse create(Long idUser, CaisseDto request)

	{
		PosResponse resp = new PosResponse();
		try {
			log.info("**********createCaisse**********");
			log.info("user = " + idUser);
			log.info(request.toString());
			log.info(request.getTypeCaisse());

			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			String numeroSerie = request.getTerminal();
			if ("WEB".equalsIgnoreCase(request.getTypeCaisse())) {
				TerminalDto terminalRequest = new TerminalDto();
				terminalRequest.setVirtuel(true);
				terminalRequest.setDesignation("WEB-" + System.currentTimeMillis());
				terminalRequest.setVersionApplication("1");
				PosResponse terminalResp = terminalService.create(idUser, terminalRequest);
				numeroSerie = terminalResp.getSerialNumber();
			}

			Caisse caisse = session.executeNamedQuerySingle(Caisse.class, "Caisse.findByPointDeVenteAndTerminal",
					new String[] { PosService.NUM_POS, PosService.NUM_SERIE },
					new String[] { request.getPointDeVente(), request.getTerminal() });

			if (caisse == null) {

				PointDeVente pointDeVente = session.executeNamedQuerySingle(PointDeVente.class,
						"PointDeVente.findPointDeVenteByNumero", new String[] { PosService.NUM_POS },
						new String[] { request.getPointDeVente() });

				if (pointDeVente == null) {
					resp.setCode(ErrorResponse.POINTDEVENTE_NOT_FOUND.getCode());
					resp.setMessage(ErrorResponse.POINTDEVENTE_NOT_FOUND.getMessage(""));
					log.info(resp.getMessage());
				} else {

					Terminal terminal = session.executeNamedQuerySingle(Terminal.class, "Terminal.findByNumeroSerie",
							new String[] { PosService.NUM_SERIE }, new String[] { numeroSerie });

					if (terminal == null) {
						resp.setCode(ErrorResponse.TERMINAL_NOT_FOUND.getCode());
						resp.setMessage(ErrorResponse.TERMINAL_NOT_FOUND.getMessage(""));
						log.info(resp.getMessage());

					} else {

						caisse = new Caisse();
						String email = request.getEmail() == null ? pointDeVente.getCommercant().getEmailContact()
								: request.getEmail();
						String telephone = request.getTelephone() == null
								? pointDeVente.getCommercant().getTelephoneContact()
								: request.getTelephone();
						String numCaisse = generationNumero(request.getPointDeVente(), session,
								SysVar.TailleComplementNumero.CAISSE);
						String cleSecrete = generateKey();
						Utilisateur user = session.findObjectById(Utilisateur.class, idUser, null);
						caisse.setPointDeVente(pointDeVente);
						caisse.setTerminal(terminal);
						caisse.setNom(request.getNom());
						caisse.setTerminalKey(cleSecrete);
						caisse.setNumeroCaisse(numCaisse);
						if (request.getCaisseSD() != null) {
							Partner caisseSD = session.findObjectById(Partner.class, request.getCaisseSD(), null);
							caisse.setCaisseSD(caisseSD);
						}
						if (!telephone.startsWith(pointDeVente.getCommercant().getCountryIndicatif())) {
							telephone = pointDeVente.getCommercant().getCountryIndicatif() + telephone;
						}
						caisse.setTelephone(telephone);
						caisse.setEmail(email);

						caisse.setDateCreation(new Date());
						caisse.setDateLastModification(new Date());
						caisse.setTypeOperation(SysVar.TypeOperation.INSERT.name());
						caisse.setUserCreation(user);
						caisse.setUserLastModification(user);
						session.saveOrUpdateObject(caisse);

						resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
						resp.setMessage(caisse.getNumeroCaisse());
						resp.setTerminalNumber(caisse.getNumeroCaisse());
						resp.setPosNumber(pointDeVente.getNumeroPointDeVente());
						resp.setMerchantNumber(pointDeVente.getCommercant().getIdPartner().toString());
						resp.setMerchantName(pointDeVente.getCommercant().getName());
						resp.setSerialNumber(numeroSerie);
						if ("WEB".equalsIgnoreCase(request.getTypeCaisse()) && caisse.getEmail() != null) {
							String message = "Bonjour," + "\n"
									+ "Merci de trouver ci-dessous vos paramètres marchand : \n Numéro Terminal = "
									+ numCaisse + "\n Clé secrète=" + cleSecrete;
							String emailAdmin="cheikhouna.bah@afriadiya.com";
							String emailP =caisse.getEmail()==null?emailAdmin:caisse.getEmail()+","+emailAdmin;
							MailUtils.sendEmails(emailP,"PARAMETRE MARCHAND", message, Boolean.TRUE, null, null);
						}
					}
				}
			} else {
				resp.setCode(ErrorResponse.CAISSE_ALREADY_EXIST.getCode());
				resp.setMessage(ErrorResponse.CAISSE_ALREADY_EXIST.getMessage(""));
				log.info(resp.getMessage());
			}

			log.info("*********Fin createCaisse****************");

		} catch (Exception e) {
			log.info("errorCrCaisse", e);
			resp.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			resp.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		return resp;
	}

	private String generateKey() {

		byte[] data = new byte[15];
		SecureRandom rndKey = new SecureRandom();
		rndKey.nextBytes(data);
		return ClientHSM.hexToString(data).toUpperCase(Locale.getDefault());
	}

	private String generationNumero(String prefix, Session session, int longComplement) {
		Integer nb = 0;
		List<Caisse> list = session.executeNamedQueryList(Caisse.class, "Caisse.CountCaisse",
				new String[] { PosService.NUM_POS }, new String[] { prefix + "%" });
		nb = list == null || list.isEmpty() ? 0 : list.size();
		return prefix + String.format("%0$" + longComplement + "s", nb + 1).replace(' ', '0');
	}
}
