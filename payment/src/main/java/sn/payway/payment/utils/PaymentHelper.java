package sn.payway.payment.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.jbosslog.JBossLog;
import sn.apiapg.commission.entities.PayerCommission;
import sn.apiapg.commission.entities.SenderCommission;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.ChannelResponse;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.aci.Caisse;
import sn.apiapg.entities.aci.IsoAcquisition;
import sn.apiapg.session.Session;
import sn.payway.common.utils.Constantes;
import sn.payway.payment.dto.PaymentDto;

@Stateless
@JBossLog
public class PaymentHelper {
	
	private static final BigDecimal HUNDRED=new BigDecimal("100");

	public IsoAcquisition createIsoAcquisition(PaymentDto request, String acquerreur, String transmetteur) {

		IsoAcquisition acqui = new IsoAcquisition();
		BigDecimal amt = new BigDecimal(request.getAmount().toString());
		if (Constantes.CURRENCY_XOF.equals(request.getCurrencyName())
				|| Constantes.CURRENCY_XAF.equals(request.getCurrencyName())) {
			amt = new BigDecimal(amt.intValue());
		} else {
			amt = amt.setScale(2, RoundingMode.HALF_UP);
		}
		String amount = StringUtils.leftPad(amt.toString(), 12, '0');
		String dateYYMMddHHmmss = DateTimeFormatter.ofPattern("yyMMddHHmmss").withZone(ZoneId.systemDefault())
				.format(Instant.now());
		String systemAudit = RandomStringUtils.randomNumeric(6);
		String numeroRecouvrement = systemAudit + RandomStringUtils.randomNumeric(6);
		String cardType ;
       if(request.getTrack2()==null || request.getTrack2().isEmpty()) {
	        cardType="0";
		}else {
			cardType = request.getTrack2().contains("=") ? "2" : "5";
		}
		acqui.setRecon(0);
		acqui.setStatus(0);
		acqui.setChannelType(request.getTransactionType());
		acqui.setTypeCarte(Integer.parseInt(cardType));
		acqui.setDateCreation(new Date());
		acqui.setDateExpiration(Instant.now(Clock.system(ZoneId.of("GMT"))).plus(25, ChronoUnit.DAYS));
		acqui.setField0("1100");
		acqui.setField2(request.getPan());
		acqui.setField3("000000");
		acqui.setField18("5812");
		if (ChannelResponse.CARD2CASHME.getCode().equals(request.getTransactionType())) {
			acqui.setField18("6010");
			acqui.setField3("170000");
		}
		acqui.setField4(amount);
		acqui.setField6(amount);
		acqui.setField5(amt.toString());
		acqui.setField7(dateYYMMddHHmmss.substring(0, 10));
		acqui.setField10("00000001");
		acqui.setField11(systemAudit);
		acqui.setField12(dateYYMMddHHmmss);
		if (request.getExpirationDate() != null) {
			acqui.setField14(request.getExpirationDate().substring(0, 4));
		}
		acqui.setField15(dateYYMMddHHmmss.substring(0, 6));
		acqui.setField16(dateYYMMddHHmmss.substring(2, 6));

		acqui.setField19("686");
		acqui.setField21("686");
		acqui.setField22("510101" + cardType + "14044");
		if (request.getPanSeq() != null) {
			acqui.setField23(request.getPanSeq());
		}
		acqui.setField24("100");
		acqui.setField32(acquerreur);
		acqui.setField33(transmetteur);
		acqui.setField35(request.getTrack2());
		acqui.setField37(numeroRecouvrement);
		acqui.setField41(request.getTerminalNumber().substring(request.getTerminalNumber().length() - 8));
		acqui.setField42(request.getMerchantNumber());
		acqui.setField43(request.getMerchantAddress());
		acqui.setField49(request.getCurrencyName());
		acqui.setField50(request.getCurrencyName());
		acqui.setField51(request.getCurrencyName());
		acqui.setCanal(request.getCanal());
		acqui.setMoyenReception(request.getMoyenReception());
		
		try {
			String customer = request.getCustomer() == null ? null
					: new ObjectMapper().writeValueAsString(request.getCustomer());
			acqui.setField45(customer);
			String beneficiary = request.getBeneficiary() == null ? null
					: new ObjectMapper().writeValueAsString(request.getBeneficiary());
			acqui.setBeneficiaryData(beneficiary);
		} catch (JsonProcessingException e) {
			log.error(e);
		}
		acqui.setField53("0201000000");
		acqui.setField128("00000000");
		if (request.getPin() == null) {
			acqui.setField53("0099000000");
		}
		if (request.getInfosCaisseProvider() != null) {
			acqui.setInfosCaisseProvider(request.getInfosCaisseProvider());
		}

		return acqui;
	}

	
public  BigDecimal roundingFees(String currency, BigDecimal fees) {
		
		BigDecimal result;
		if(Constantes.CURRENCY_XOF.equals(currency)||Constantes.CURRENCY_XAF.equals(currency)) {
			if(BigDecimal.ZERO.compareTo(fees)==0) {
			result =BigDecimal.ZERO;	
			}else {
			 BigDecimal intFees= fees.setScale(0,RoundingMode.UP);
			BigDecimal five =new BigDecimal("5");
			  BigDecimal div  = new BigDecimal(intFees.divide(five).intValue());
			  BigDecimal mult  = div.multiply(five);
			  BigDecimal remainder = five.subtract(intFees.subtract(mult));
			  result = mult.subtract(intFees).compareTo(BigDecimal.ZERO)==0?intFees:intFees.add(remainder);
			}
		}else {
			result = fees.setScale(2,RoundingMode.UP);
		}
		return result;
	}
public  BigDecimal getCommissionPartner(BigDecimal amount, SenderCommission comm) {
	BigDecimal commission;

	BigDecimal flatFees = comm.getFlatCommission()==null?BigDecimal.ZERO:comm.getFlatCommission();
	if (BEConstantes.PARTNER_COMMISSION_VALEUR.equals(comm.getType())) {
		commission = comm.getCommission();
	} else {
		commission = amount.multiply(comm.getCommission()).divide(HUNDRED);
	}
	return commission.add(flatFees);
}
public  BigDecimal getCommissionPartner(BigDecimal amount, PayerCommission comm) {
	BigDecimal commission;

	if (BEConstantes.PARTNER_COMMISSION_VALEUR.equals(comm.getType())) {
		commission = comm.getCommission();
	} else {
		commission = amount.multiply(comm.getCommission()).divide(HUNDRED);
	}
	return commission;
}
public Caisse getFirstOnlineCaisse(Partner partner, Session sess) {

	Caisse  caisse = null;
	List<Caisse> caisses = sess.executeNamedQueryList(Caisse.class, "Caisse.findByPartnerId",
			new String[] { "partner" }, new Partner[] { partner });
	if (caisses.isEmpty()) {
		log.info("no caisse");
	} else {

		for (Caisse caiss : caisses) {
			if (caiss.getTerminal().getNumeroSerie().startsWith("OLNPAY")) {
				caisse = caiss;
				break;
			}
		}
	}
	return caisse;
}
public  String getResponseMessage(String code) {
	String message;
	switch (code) {
	case "100":
		message =  "Ne pas Honorer";break;
	case "101":
		message =  "Carte Périmé";break;
	case "106":
		message =  "Fraude suspectée";break;
	case "109":
		message =  "Commerce érroné";break;
	case "110":
		message =  "Montant érroné";break;
	case "114":
		message =  "Pas de compte du type demandé";break;
	case "116":
		message =  "Solde insufisant";break;
	case "117":
		message =  "PIN Incorrect";break;
	case "118":
		message =  "Carte non enregistrée";break;
	case "119":
		message =  "transaction non autorisée";break;
	case "120":
		message =  "Transaction non admise au terminal";break;
	case "121":
		message =  "Dépassement des limites de retrait";break;
	case "128":
		message =  "Erreur de synchronisation de la clef du PIN ";break;
	case "183":
		message =  "Cvv invalide";break;
	case "184":
		message =  "Date invalide";break;
	case "202":
		message =  "fraude suspectée";break;
	case "206":
		message =  "Nombre de tentative de validation de PIN dépassé,se referer à l'emetteur de la carte";break;
	case "909":
		message =  "Erreur interne, Veuillez réessayer";break;
	case "912":
		message =  "Emetteur carte indisponible";break;
	case "992":
		message =  "Emetteur non trouvé";break;
	case "993":
		message =  "Vérification PIN impossible";break;
	case "994":
		message =  "Erreur traitement de la transaction";break;
	case "995":
		message =  "Erreur traitement du serveur";break;

	default:
		message =  code;break;
	
	}
	return message;
}
public String getSubMeansTypeFromBank(String bank) {
	String subMeans = "ALL";
	switch (bank) {
	case Constantes.CODE_OM_API:
		subMeans = "OMSN";
		break;
	case Constantes.CODE_WAVE:
		subMeans = "WVSN";
		break;
	case Constantes.CODE_FREEMONEY:
		subMeans = "FMSN";
		break;
	case Constantes.CODE_PAYDUNYA:
		subMeans = "EMSN";
		break;
	case Constantes.CODE_RESTAU:
		subMeans = "EATSN";
		break;
	default:
		break;

	}
	return subMeans;

}
}
