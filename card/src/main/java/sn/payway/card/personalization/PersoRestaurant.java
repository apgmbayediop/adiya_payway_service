package sn.payway.card.personalization;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.GenericType;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import sn.apiapg.common.entities.Utilisateur;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.AbstractResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.Card;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.PartnerBin;
import sn.apiapg.entities.Register;
import sn.apiapg.entities.Wallet;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.card.exception.CardException;
import sn.payway.card.generation.CardGenService;
import sn.payway.card.link.LinkCardService;
import sn.payway.card.personalization.dto.PersoDto;
import sn.payway.card.wallet.CardWalletService;
import sn.payway.card.wallet.dto.CreateWalletResponse;
import sn.payway.common.excel.ExcelFileService;

@Stateless
public class PersoRestaurant {

	public static final Logger LOG = Logger.getLogger(PersoRestaurant.class);

	private static final String FILE_PARAM = "file";
	public static final String[] CARD_READABLE_HEADERS = { "NOM", "PRENOM", "TELEPHONE", "RECHARGE" };
	private static final String[] CARD_JSON_HEADERS = { "lastName", "firstName", "phoneNumber", "loadAmount" };

	private static final ConcurrentMap<String, String> CARD_CONVERSION_MAP = IntStream
			.range(0, CARD_JSON_HEADERS.length).boxed()
			.collect(Collectors.toConcurrentMap(i -> CARD_READABLE_HEADERS[i], i -> CARD_JSON_HEADERS[i]));
	public static final String[] CARD_READABLE_HEADERS_V1 = { "NOM", "PRENOM", "MATRICULE", "TELEPHONE", "CARTE" };

	private static final String[] CARD_JSON_HEADERS_V1 = { "lastName", "firstName",
			"documentNumber", "phoneNumber", "cin" };

	private static final ConcurrentMap<String, String> CARD_CONVERSION_MAP_V1 = IntStream
			.range(0, CARD_JSON_HEADERS_V1.length).boxed()
			.collect(Collectors.toConcurrentMap(i -> CARD_READABLE_HEADERS_V1[i], i -> CARD_JSON_HEADERS_V1[i]));

	@Inject
	private ExcelFileService excelFile;
	@Inject
	private PersonalisationService persoService;
	@Inject
	private CardGenService cardServices;
	@Inject
	private CardWalletService crdWltService;
	@Inject
	private LinkCardService lnkService;

	@PostConstruct
	public void init() {
		try {

			File outgoing = new File(Paths.get(PersonalisationService.OUTGOING_APG).toUri());
			if (!outgoing.exists()) {
				outgoing.mkdirs();
			}

		} catch (Exception e) {
			LOG.error("initPerso", e);
		}
	}

	public AbstractResponse personalize(Utilisateur user, MultipartFormDataInput form) throws CardException {
		AbstractResponse response = new AbstractResponse();
		try {
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			String subBin = form.getFormDataPart("subBin", new GenericType<String>() {
			});
			PartnerBin partnerBin = session.executeNamedQuerySingle(PartnerBin.class, "PartnerBin.findByBin",
					new String[] { "bin" }, new String[] { subBin });
			if (partnerBin == null) {
				response.setCode(ErrorResponse.CARD_ERROR_3001.getCode());
				response.setMessage("Le sous bin n est pas valide");
			} else {
				Map<String, List<InputPart>> map = form.getFormDataMap();
				List<InputPart> inputss = map.get(FILE_PARAM);
				InputPart inputP = inputss.get(0);
				InputStream file = inputP.getBody(InputStream.class, null);
				String idPartner = form.getFormDataPart("idPartner", new GenericType<String>() {
				});
				Partner partner = (idPartner == null || idPartner.isBlank()) ? partnerBin.getIdPartner()
						: session.findObjectById(Partner.class, Long.parseLong(idPartner), null);
				String[] headers = excelFile.headers(0, 0, file);
				if (Arrays.equals(CARD_READABLE_HEADERS_V1, headers)) {
					response =personnalizeV1(session, file, partner);

				} else {
					response = personnalizeV2(session, file, partner, partnerBin);
				}
			}
		} catch (IOException e) {
			response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			response.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
			LOG.error("ErrorPerso", e);

		}
		return response;
	}

	public AbstractResponse personnalizeV1(Session session, InputStream file, Partner partner) throws IOException {
		AbstractResponse response = new AbstractResponse();
		
		List<PersoDto> holders = excelFile.read(PersoDto.class, file, CARD_CONVERSION_MAP_V1);
		if (holders == null || holders.isEmpty()) {
			response.setCode(ErrorResponse.CARD_ERROR_6001.getCode());
			response.setMessage("Le fichier est vide");
		} else {
			
		}
		return response;
	}

	private AbstractResponse personnalizeV2(Session session, InputStream file, Partner partner, PartnerBin partnerBin)
			throws CardException, IOException {
		AbstractResponse response = new AbstractResponse();
		List<PersoDto> holders = excelFile.read(PersoDto.class, file, CARD_CONVERSION_MAP);
		if (holders == null || holders.isEmpty()) {
			response.setCode(ErrorResponse.CARD_ERROR_6001.getCode());
			response.setMessage("Le fichier est vide");
		} else {
			List<Card> cards = cardServices.generateCards(session, partnerBin, partner, holders.size());
			response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
			int index = 0;
			Date dateWriting = new Date();
			for (PersoDto holder : holders) {
				Register cardHolder = new Register();
				cardHolder.setDate(dateWriting);
				cardHolder.setFirstname(holder.getFirstName().strip());
				cardHolder.setLastname(holder.getLastName().strip());
				cardHolder.setPhonenumber(persoService.getPhoneNumber(holder.getPhoneNumber()));
				cardHolder.setCustomerIndicatif(partner.getCountryIndicatif());
				cardHolder.setCountry(partner.getCountryIsoCode());
				cardHolder = (Register) session.saveObject(cardHolder);
				cards.get(index).setRegister(cardHolder);
				BigDecimal loadAmount = holder.getLoadAmount() == null ? BigDecimal.ZERO
						: holder.getLoadAmount().abs().setScale(0);
				cards.get(index).setLoadAmount(loadAmount);
				cards.get(index).setActivatePartner(partner);
				cards.get(index).setActivationDate(new Date());
				cards.get(index).setAmountToBeLevied(BigDecimal.ZERO);
				cards.get(index).setMontant(BigDecimal.ZERO);
				cards.get(index).setStatus(BEConstantes.CARD_ACTIVE);
				cards.get(index).setPin(RandomStringUtils.randomNumeric(5));
				session.updateObject(cards.get(index));
				createAndLinkWallet(session, cards.get(index), partnerBin.getIdPartner());
				index++;
			}
			List<String> lines = persoService.generatePersoFile(cards);
			HashMap<String, List<String>> workbook = new HashMap<>();
			String name = partner.getName().length() > 10 ? partner.getName().substring(0, 10) : partner.getName();
			String fileName = "perso_" + name;
			workbook.put(fileName, lines);
			persoService.createOutgoingFile(PersonalisationService.OUTGOING_APG, workbook);
		}
		return response;
	}

	private void createAndLinkWallet(Session session, Card card, Partner partner) {
		CreateWalletResponse response = crdWltService.createWallet(card, partner);
		LOG.info(response.getCode());
		LOG.info(response.getWalletId());
		if (ErrorResponse.REPONSE_SUCCESS.getCode().equals(response.getCode())) {
			LOG.info("OK");
			LOG.info("Running");
			Wallet wallet = session.executeNamedQuerySingle(Wallet.class, "findMyWallet", new String[] { "wallet" },
					new String[] { response.getWalletId() });
			lnkService.linkWithWallet(card.getCin(), wallet);
		}

	}
}
