package sn.adiya.card.generation;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.logging.Logger;

import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.Card;
import sn.fig.entities.CardGenerationLot;
import sn.fig.entities.Currency;
import sn.fig.entities.Partner;
import sn.fig.entities.PartnerBin;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.card.exception.CardException;
import sn.adiya.card.generation.dto.CardGenerationDto;

@Stateless
public class CardGenService {

public static final Logger LOG = Logger.getLogger(CardGenService.class);
	
	public static final String KEY = "adiyaMON";
	
	public List<Card> generateCards( Session session,PartnerBin partnerBin,Partner partner,int quantite) throws CardException {
		
		List<Card> cards = new ArrayList<>();
		Currency currency = session.executeNamedQuerySingle(Currency.class, "Currency.findByName", new String[] {"currencyName"},
				new String[] {partner.getCurrencyName()});
		CardGenerationLot lot = new CardGenerationLot();
		lot.setDateWriting(new Date());
		lot.setNumberCardsGenerated(quantite);
		lot.setPartnerBin(partnerBin);
		lot.setCardType("DEBIT");
		lot.setCurrency(currency);
		lot.setGracePeriod(3);
		lot.setValidity(3);
		int seqNumber = partnerBin.getPanCounter();
		int limit = seqNumber + quantite;
		if (limit > 99999) {
			throw new CardException(ErrorResponse.CARD_LIMIT_SEQ_NUMBER.getCode(),
					ErrorResponse.CARD_LIMIT_SEQ_NUMBER.getMessage(""));

		}
		List<Card> lCards = new ArrayList<>();
		LOG.info("sequence number " + seqNumber);
		CardGenerationDto data = new CardGenerationDto();
		data.setCurrency(currency);
		data.setGracePeriod(lot.getGracePeriod());
		data.setType(lot.getCardType());
		data.setValidity(lot.getValidity());
		for (int i = 1; i <= quantite; i++) {
			data.setSequenceNumber(seqNumber + i);
			lCards.add(generateCard(partnerBin, partner,data));

		}
		LOG.info("checking doublons");
		if (findDoublonsCin(lCards)){
			throw new CardException(ErrorResponse.TRANSACTION_DOUBLOON_ERROR.getCode(),
					"doublons detectées sur le lot de carte");
		}else {
			LOG.info("no doublons doublons");
			lot = (CardGenerationLot) session.saveObject(lot);
			 cards = saveCardLot(lCards, lot);
			LOG.info("saving cards finish");
			partnerBin.setPanCounter(seqNumber + quantite);
			session.updateObject(partnerBin);
		} 
		return cards;
	}
	
	private Card generateCard(PartnerBin partnerBin,Partner partner,CardGenerationDto data) {

		APIUtilVue apiUtilVue = APIUtilVue.getInstance();
		int digits = partnerBin.getParentBin().getDigitsNumber();
		int complen = partnerBin.getBin().length() + 1;
		digits = digits - complen;
		String seq = "000000" + data.getSequenceNumber();
		String cardCin = "00000" + data.getSequenceNumber();
		cardCin = partnerBin.getCinPrefix() + partnerBin.getBin().substring(partnerBin.getBin().length() - 2)
				+ cardCin.substring(cardCin.length() - 5);
		seq = seq.substring(seq.length() - digits);

		String pan = partnerBin.getBin() + seq;
		Date dateWriting = new Date();
		String format = "yyMM";
		Integer luhnResult = apiUtilVue.getLuhn(pan);
		Date expiryDate = calculateExpiryDate(data.getValidity(), data.getGracePeriod());
		pan = pan + luhnResult.toString();
		String securedPan = partnerBin.getBin()+"XXXXXX"+pan.substring(pan.length()-4);
		String hashPan = DigestUtils.sha512Hex(pan+KEY);
		String hashCin = DigestUtils.sha512Hex(cardCin+KEY);
		Card card = new Card();
		card.setDate(dateWriting);
		card.setPan(pan);
		card.setCin(cardCin);
		card.setHashPan(hashPan);
		card.setHashCin(hashCin);
		card.setSecuredPan(securedPan);
		card.setMontant(BigDecimal.ZERO);
		card.setIsValidated(Boolean.TRUE);
		card.setIsBlocked(Boolean.FALSE);
		card.setPrincipalAccountNumber(pan);
		card.setAmountToBeLevied(BigDecimal.ZERO);
		card.setStatus("INACTIVE");
		card.setLinked(false);
		card.setIsManagedByAPG(true);
		card.setType(data.getType());
		card.setInFaux(0L);
		card.setPartner(partner);
		card.setExpiryDate(new SimpleDateFormat(format).format(expiryDate));
		card.setCurrency(data.getCurrency());
		card.setServiceCode("200");

		return card;
	}
	private Date calculateExpiryDate(int validity, int gracePeriod) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, validity);
		cal.add(Calendar.MONTH, gracePeriod);
		return cal.getTime();
	}
	@Transactional(value = TxType.REQUIRES_NEW)
	private List<Card> saveCardLot(List<Card> cardList, CardGenerationLot lot) {
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		EntityManager manager = session.getManager();
		try {
			int batchSize = 1000;

			int count = 0;
			for (Card crd : cardList) {
				crd.setLot(lot);
				if (count > 0 && count % batchSize == 0) {
					manager.flush();
					manager.clear();
				}
				count++;
				manager.persist(crd);
			}
			return cardList;
		} catch (RuntimeException e) {
			LOG.error("saveCard", e);
			throw e;
		}
	}
	private boolean findDoublonsCin(List<Card> cardList) {

		List<String> listCin = new ArrayList<String>();
		List<String> listPan = new ArrayList<String>();
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		for (Card card : cardList) {
			listCin.add(card.getCin());
			listPan.add(card.getPan());
			if (listCin.size() >= 900) {
				List<Card> l = session.executeNamedQueryList(Card.class, "Card.FindByPanOrCinList",
						new String[] { "pan", "cin" }, new Object[] { listPan, listCin });
				if (l != null && !l.isEmpty()) {
					return true;
				}
				listCin.clear();
				listPan.clear();
			}
		}
		if (listCin.isEmpty()) {
			return false;
		}
		List<Card> l = session.executeNamedQueryList(Card.class, "Card.FindByPanOrCinList",
				new String[] { "pan", "cin" }, new Object[] { listPan, listCin });

		return l != null && !l.isEmpty();
	}
}
