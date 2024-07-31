package sn.adiya.payment.emv;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import sn.fig.common.config.entities.ParametresGeneraux;
import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.common.utils.ChannelResponse;
import sn.fig.entities.Currency;
import sn.fig.entities.Partner;
import sn.fig.entities.aci.AID;
import sn.fig.entities.aci.Caisse;
import sn.fig.entities.aci.CardKey;
import sn.fig.entities.aci.CommissionMonetique;
import sn.fig.entities.aci.PointDeVente;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.hsm.HSMHandler;
import sn.adiya.common.utils.Constantes;
import sn.adiya.common.utils.AdiyaCommonTools;
import sn.adiya.common.utils.ValidateData;

@Stateless
public class AciConfig {

	private static final Logger LOG = Logger.getLogger(AciConfig.class);
	@Inject
	private ValidateData validation;

	@Inject
	private HSMHandler hsm;

	@Inject
	private AdiyaCommonTools properties;

	public AbstractResponse configTerminal(ConfigRequest request) {
		return configTerminal(request.getTerminalSn());
	}

	public AbstractResponse configTerminal(String terminalSN) {
		AbstractResponse resp = new AbstractResponse();
		try {
			LOG.info("**********configTerminal**********");
			LOG.info(terminalSN);

			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			Caisse caisse = session.executeNamedQuerySingle(Caisse.class, "Caisse.findByTerminal",
					new String[] { "numeroSerie" }, new String[] { terminalSN });
			if (caisse == null) {
				resp.setCode(ErrorResponse.CAISSE_NOT_FOUND.getCode());
				resp.setMessage(ErrorResponse.CAISSE_NOT_FOUND.getMessage(""));
				LOG.info(resp.getMessage());
			} else {
				PointDeVente pos = caisse.getPointDeVente();
				Partner commercant = pos.getCommercant();
				List<CommissionMonetique> commissionMonetiques = session.executeNamedQueryList(
						CommissionMonetique.class, "CommissionMonetique.FindByPartenaireId",
						new String[] { "idPartner" }, new Long[] { commercant.getIdPartner() });
				if (commissionMonetiques == null || commissionMonetiques.isEmpty()) {
					resp.setCode(ErrorResponse.COMMISSION_NOT_FOUND.getCode());
					resp.setMessage(ErrorResponse.COMMISSION_NOT_FOUND.getMessage(""));
					LOG.info(resp.getMessage());
				} else {
					List<AID> aids = session.findAllObject(AID.class);
					List<CardKey> listKeys = session.findAllObject(CardKey.class);

					List<String> aidList = new ArrayList<>();
					for (AID aid : aids) {
						aidList.add(aid.getAid());
						Partner distributeur = commercant.getParent();
						String header = distributeur.getTicketHeader();
						if (header == null) {
							header = "APG";
						}
						Currency currency = session.executeNamedQuerySingle(Currency.class, "Currency.findByName",
								new String[] { "currencyName" }, new String[] { commercant.getCurrencyName() });

						if (currency.getNumericCode() == null) {
							resp.setCode(ErrorResponse.NO_AID_CONFIGURED.getCode());
							resp.setMessage("currency numeric code is null");
							LOG.info("pos " + resp.getMessage());
						} else {
							if (commercant.getCountry().getNumericCode() == null) {
								resp.setCode(ErrorResponse.NO_AID_CONFIGURED.getCode());
								resp.setMessage("country numeric code is null");
								LOG.info("pos " + resp.getMessage());
							} else {

								String merchantType = ChannelResponse.CARD2CASHME.getCode();
								for (CommissionMonetique comm : commissionMonetiques) {
									if (comm.getChannelType().equals(ChannelResponse.CARD2PAY.getCode())) {
										merchantType = ChannelResponse.CARD2PAY.getCode();
										break;
									}
								}
								LOG.info("merchantType " + merchantType);
								TerminalData configData = new TerminalData();
								configData.setCaisseNumber(caisse.getNumeroCaisse());
								configData.setCountryCode(commercant.getCountry().getNumericCode());
								configData.setCurrencyCode(currency.getNumericCode());
								configData.setCurrencyName(currency.getCurrencyName());
								configData.setHeader(header);
								configData.setHeureTelecollecte(caisse.getTerminal().getHeureTelecollecte());
								configData.setMerchantAddress(commercant.getAdresse());
								configData.setMerchantName(commercant.getName());
								configData.setMerchantNumber(commercant.getIdPartner().toString());
								configData.setMerchantType(merchantType);
								configData.setPosName(pos.getNom());
								configData.setPosNumber(pos.getNumeroPointDeVente());
								configData.setEnv(properties.getProperty(Constantes.ENV_PRPOPERTY));
								configData.setTerminalSn(terminalSN);
								configData.setMerchantCategorie(commercant.getMcc());
								configData.setAidList(aidList);
								configData.setListKeys(listKeys);
								ConfigResponse response = new ConfigResponse();
								response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
								response.setMessage(ErrorResponse.REPONSE_SUCCESS.getMessage(""));
								response.setConfigData(configData);
								resp = response;
							}
						}
					}
				}

			}

		} catch (Exception e) {
			LOG.error("errorConfigTer", e);
			resp = new AbstractResponse(ErrorResponse.UNKNOWN_ERROR.getCode(),
					ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		return resp;
	}

	public AbstractResponse getTerminalKey(ConfigRequest request) {
		try {
			LOG.info("terminalKey");
			LOG.info(request);
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			AbstractResponse resp = new AbstractResponse();
			Caisse caisse = sess.executeNamedQuerySingle(Caisse.class, "Caisse.findByTerminal",
					new String[] { "numeroSerie" }, new String[] { request.getTerminalSn() });
			if (caisse == null) {
				resp.setCode(ErrorResponse.CAISSE_NOT_FOUND.getCode());
				resp.setMessage(ErrorResponse.CAISSE_NOT_FOUND.getMessage(""));
				LOG.info(resp.getMessage());
				return resp;
			}
			String ksnStr = HSMHandler.generateKsn(request.getTerminalSn());
			LOG.info(ksnStr);
			String param = "PARAM_HSM_APG";
			if (BEConstantes.COD_BANQUE_FINAO_GIM
					.equals(caisse.getPointDeVente().getCommercant().getParent().getCodeBanque())) {
				param = "PARAM_HSM_FINAO";
			} else if (BEConstantes.COD_BANQUE_CFP_GIM
					.equals(caisse.getPointDeVente().getCommercant().getParent().getCodeBanque())) {
				param = "PARAM_HSM_CFP";
			}
			LOG.info(param);
			LOG.info(caisse.getPointDeVente().getCommercant().getParent().getCodeBanque());
			String baseKey = getBaseKey(param);
			if (baseKey == null || baseKey.isEmpty()) {
				resp.setCode(ErrorResponse.CONFIG_TERMINAL_ERROR.getCode());
				resp.setMessage("Terminal base key not added");
				LOG.info(resp.getMessage());
				return resp;
			}
			byte[] ksn = HSMHandler.hexStringToByteArray(ksnStr);
			hsm.connect();
			byte[] ipek = hsm.generateIPEK(ksn, baseKey);
			resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
			resp.setMessage(HSMHandler.hexToString(ksn) + "!" + HSMHandler.hexToString(ipek));
			LOG.info("IPEK generated ");
			return resp;
		} catch (Exception e) {
			LOG.error("errorKey", e);
			return new AbstractResponse(ErrorResponse.UNKNOWN_ERROR.getCode(),
					ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
	}

	public AbstractResponse addAid(String utlisateur, List<AID> aidList) {
		try {
			AbstractResponse response = new AbstractResponse();
			if (aidList == null || aidList.isEmpty()) {
				response.setCode(ErrorResponse.SYNTAXE_ERRORS_1802.getCode());
				response.setMessage("AID LIST is empty");
				return response;
			}
			LOG.info(aidList.toString());
			for (AID aid : aidList) {
				if (aid.getAid() == null || aid.getAid().isBlank()) {
					response.setCode(ErrorResponse.SYNTAXE_ERRORS_1802.getCode());
					response.setMessage(ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("aid"));
					return response;
				}
				if (aid.getProduct() == null || aid.getProduct().isEmpty()) {
					response.setCode(ErrorResponse.SYNTAXE_ERRORS_1802.getCode());
					response.setMessage(ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("aid"));
					return response;
				}
			}
			Date date = new Date();
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			Utilisateur user = sess.findObjectById(Utilisateur.class, Long.parseLong(utlisateur), null);
			AID findAId;
			int added = 0;
			String cleanAid;
			for (AID aid : aidList) {
				cleanAid = aid.getAid().replace(" ", "");
				findAId = sess.executeNamedQuerySingle(AID.class, "AID.findByAid", new String[] { "aid" },
						new String[] { cleanAid });
				if (findAId == null) {
					aid.setAid(cleanAid);
					aid.setDateCreation(date);
					aid.setDateLastModification(date);
					aid.setTypeOperation("INSERT");
					aid.setUserCreation(user);
					aid.setUserLastModification(user);
					sess.saveObject(aid);
					added++;
				}
			}
			LOG.info("comming aids " + aidList.size());
			LOG.info("added aids " + added);
			response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
			response.setMessage(Integer.toString(added));
			return response;
		} catch (Exception e) {
			LOG.error("error", e);
			return new AbstractResponse(ErrorResponse.UNKNOWN_ERROR.getCode(),
					ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
	}

	public AbstractResponse addCardKey(String utlisateur, List<CardKey> list) {
		try {
			AbstractResponse response = new AbstractResponse();
			if (list == null || list.isEmpty()) {
				response.setCode(ErrorResponse.SYNTAXE_ERRORS_1802.getCode());
				response.setMessage("AID LIST is empty");
				return response;
			}
			LOG.info(list.toString());
			String[] props = { "index", "rid", "modulus", "sid" };
			for (CardKey ck : list) {
				response = validation.validataData(ck, props);
				if (!response.getCode().equals(ErrorResponse.REPONSE_SUCCESS.getCode())) {
					return response;
				}
			}
			Date date = new Date();
			Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			CardKey findKey;
			int added = 0;
			String cleanRid;
			String cleanIndex;
			Utilisateur user = sess.findObjectById(Utilisateur.class, Long.parseLong(utlisateur), null);
			for (CardKey key : list) {
				cleanRid = key.getRid().replace(" ", "");
				cleanIndex = key.getIndex().replace(" ", "");
				findKey = sess.executeNamedQuerySingle(CardKey.class, "CardKey.findByRidAndIndex",
						new String[] { "rid", "index" }, new String[] { cleanRid, cleanIndex });
				if (findKey == null) {
					key.setIndex(cleanIndex);
					key.setRid(cleanRid);
					key.setModulus(key.getModulus().replace(" ", ""));
					key.setExponent(key.getExponent().replace(" ", ""));
					key.setDateCreation(date);
					key.setDateLastModification(date);
					key.setTypeOperation("INSERT");
					key.setUserCreation(user);
					key.setUserLastModification(user);
					sess.saveObject(key);
					added++;
				}
			}
			LOG.info("comming keys " + list.size());
			LOG.info("added keys " + added);
			response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
			response.setMessage(Integer.toString(added));
			return response;
		} catch (Exception e) {
			LOG.error("error", e);
			return new AbstractResponse(ErrorResponse.UNKNOWN_ERROR.getCode(),
					ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
	}

	private String getBaseKey(String key) {
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		ParametresGeneraux param = session.findObjectById(ParametresGeneraux.class, null, key);
		if (param != null) {
			String[] data = param.getLibelle().split(";");
			return data[0];
		}
		return null;
	}

}
