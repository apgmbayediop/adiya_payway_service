package sn.payway.payment.gim;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;

import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.ChannelResponse;
import sn.apiapg.entities.Card;
import sn.apiapg.entities.Partner;
import sn.apiapg.entities.Transaction;
import sn.apiapg.entities.aci.Caisse;
import sn.apiapg.entities.aci.CommissionMonetique;
import sn.apiapg.entities.aci.IsoAcquisition;
import sn.apiapg.lis.utils.SysVar;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.hsm.HSMHandler;
import sn.payway.common.utils.Constantes;
import sn.payway.payment.dto.PaymentDetails;
import sn.payway.payment.dto.PaymentDto;
import sn.payway.payment.services.finalize.FinalizePayment;
import sn.payway.payment.utils.PaymentHelper;

@Stateless
public class GimService {

	private static final Logger LOG = Logger.getLogger(GimService.class);

	public static final String FINAO_INDEX = "ZPKFINAO";
	public static final String CFP_INDEX = "ZPKCFP";
	public static final String BRM_INDEX = "ZPK";
	
	
	private final List<String> fallbackCodes = new ArrayList<>();
	@Inject 
	private BrmGimControllerOperation brm;
	@Inject
	private FinaoGimControllerOperation finao;
	@Inject
	private CfpGimControllerOperation cfp;
	@Inject
	private FinalizePayment finalize;
	@Inject
	private PaymentHelper payHelper;
	private String template;
	@PostConstruct
	public void init() {
		String sep = File.separator;
		template = BEConstantes.path + sep + "config" + sep + "isoascii.xml";
		LOG.info("template " + template);
		fallbackCodes.addAll(Arrays.asList(new String[] {"128","100","912"}));
	}
	
	public byte[] sendOperationGim(byte[] toSend,String banque) {

		LOG.info("sendOperationToGim");
		LOG.info(banque);
		switch (banque) {
		case BEConstantes.COD_BANQUE_BRM_GIM :return brm.sendTransactionToGim(toSend);
		case BEConstantes.COD_BANQUE_FINAO_GIM :return finao.sendTransactionToGim(toSend);
		case BEConstantes.COD_BANQUE_CFP_GIM :return cfp.sendTransactionToGim(toSend);
		default:return new byte[0];
		}
	}
	public PaymentDetails createTransactionGim(PaymentDto request, Caisse caisse, CommissionMonetique commMon) {
		LOG.info("createTransactionGim");
		IsoAcquisition acquisition = new IsoAcquisition();
		acquisition.setDateCreation(new Date());
		acquisition.setChannelType(request.getTransactionType());
		acquisition.setBeneficiaryData(null);
		String idTransaction = Long.toString(System.currentTimeMillis()) ;

		try {
			Partner commercant = caisse.getPointDeVente().getCommercant();
			String currency = request.getCurrencyName().substring(0, 3);
			Partner banque = caisse.getPointDeVente().getCommercant().getParent();
			String cardId = request.getPan().substring(0, 6) + "xxxxxx"
					+ request.getPan().substring(request.getPan().length() - 4);
			acquisition.setCardId(cardId);
			acquisition.setCaisse(caisse);
			ISOMsg msg;
			String msr="MSR";
			String cardType = msr.equals(request.getCardType()) ? "2" : "5";

			try(InputStream stream = Files.newInputStream(Paths.get(template))){
			GenericPackager packager = new GenericPackager(stream);
			LOG.info("create message");
			msg = packager.createISOMsg();
			msg.setPackager(packager);
			String amount = StringUtils.leftPad(request.getAmount().toString(), 12, '0');
			String dateYYMMddHHmmss = DateTimeFormatter.ofPattern("yyMMddHHmmss").withZone(ZoneId.systemDefault()).format(Instant.now());
			String systemAudit = RandomStringUtils.randomNumeric(6);
			String numeroRecouvrement = systemAudit + RandomStringUtils.randomNumeric(6);
			numeroRecouvrement = numeroRecouvrement.substring(numeroRecouvrement.length() - 12);
			msg.set(3, "000000");
			msg.set(18, commercant.getMcc());
			acquisition.setFees(BigDecimal.ZERO);
			if (ChannelResponse.CARD2CASHME.getCode().equalsIgnoreCase(request.getTransactionType())) {
				msg.set(3, "170000");
				msg.set(18, "6010");
				acquisition.setFees(SysVar.FRAIS_CASH_ADVANCE_GIM);
			}
			msg.setMTI("1100");
			msg.set(2, request.getPan());
			msg.set(4, amount);msg.set(6, amount);
			msg.set(7, dateYYMMddHHmmss.substring(0, 10));
			msg.set(10, "00000001");
			msg.set(11, systemAudit);msg.set(12, dateYYMMddHHmmss);
			msg.set(14, request.getExpirationDate().substring(0, 4));
			msg.set(15, dateYYMMddHHmmss.substring(0, 6));msg.set(16, dateYYMMddHHmmss.substring(2, 6));
			msg.set(19, commercant.getCountry().getNumericCode());msg.set(21, commercant.getCountry().getNumericCode());
			msg.set(22, "510311" + cardType + "14044");
			if (request.getPanSeq() != null) {
				msg.set(23, request.getPanSeq());
			}
			msg.set(24, "100");	msg.set(32, banque.getCodeBanque());
			msg.set(33, "101010");msg.set(35, request.getTrack2());msg.set(37, numeroRecouvrement);
			msg.set(41, caisse.getNumeroCaisse().substring(caisse.getNumeroCaisse().length() - 8));
			LOG.info("field42 " + StringUtils.rightPad(banque.getIdentifiantGim(), 15, ' '));
			msg.set(42, StringUtils.rightPad(banque.getIdentifiantGim(), 15, ' '));// 0550000022014 0203900022014
			msg.set(43, commercant.getName());
			msg.set(48,"P9500221");
			msg.set(49, currency);
			msg.set(50, currency);
			msg.set(51, currency);
			
			if (request.getPinBlock() == null) {
				msg.set(53, "0099000000");	
			} else {
				msg.set(52, HSMHandler.hexStringToByteArray(request.getPinBlock()));
				msg.set(53, "0201000000");

			}
			String byte00="00";
			if (request.getIssuerData() == null||byte00.equals(request.getIssuerData())) {
				LOG.info("no issuer data");
			}else{
				LOG.info(request.getIssuerData());
				msg.set(55, HSMHandler.hexStringToByteArray(request.getIssuerData()));
			}
			msg.set(128, HSMHandler.hexStringToByteArray("00000000"));
			byte[] pack = msg.pack();
			byte[] toSend = formIsoData(pack);
			LOG.info("send " + toSend.length);
			idTransaction = msg.getString(37);
			setFields(acquisition, msg);
			acquisition.setCaisse(caisse);
			acquisition.setPaymentMeansType(request.getMeansType());
			acquisition.setField39("100");
			
				acquisition.setField0(msg.getMTI());
				acquisition.setRecon(-1);
				acquisition.setStatus(0);
				byte[] data = sendOperationGim(toSend,commercant.getParent().getCodeBanque());
				 int length1 =1;
				if (data.length == 0) {
					LOG.info("not read data gim");
					return new PaymentDetails(ErrorResponse.UNKNOWN_ERROR,"");
				}else if (data.length == length1) {
					LOG.info("timeout gim");
					return new PaymentDetails(ErrorResponse.UNAUTHORIZED_PAYMENT.getCode(),
							payHelper.getResponseMessage("912"));
				} 
				else {
					PaymentDetails resp = new PaymentDetails();
					ISOMsg isoResp = new ISOMsg();
					isoResp.setPackager(packager);
					isoResp.unpack(data);
					acquisition.setRecon(0);
					setFields(acquisition, isoResp);
					LOG.info("field39 " + isoResp.getString(39));
					if (Constantes.ISO_SUCCESS_STATUS.equals(isoResp.getString(39))) {
						acquisition = saveTransaction(acquisition);
						resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
						resp.setMessage(ErrorResponse.REPONSE_SUCCESS.getMessage(""));
						resp.setAmount(request.getAmount());
						resp.setAutorisationCode(isoResp.getString(38));
						resp.setTransactionId(StringUtils.leftPad(acquisition.getId().toString(), 8, '0'));
						resp.setAuditNumber(StringUtils.leftPad(acquisition.getId().toString(), 8, '0'));
						resp.setMeansType(Constantes.CARTE);
						resp.setCardType("ETRANGER");
						resp.setCurrencyName(
								acquisition.getCaisse().getPointDeVente().getCommercant().getCurrencyName());
						resp.setTransactionDate(
								new SimpleDateFormat(BEConstantes.FORMAT_DATE_TIME,Locale.getDefault()).format(acquisition.getDateCreation()));
						resp.setPan(isoResp.getString(2));
					} else {
						if(fallbackCodes.contains(isoResp.getString(39))&&!msr.equals(request.getCardType())) {
							resp.setFallBack("BACK_MSR");
						}
						resp.setCode(ErrorResponse.UNAUTHORIZED_PAYMENT.getCode());
						resp.setMessage(payHelper.getResponseMessage(isoResp.getString(39)));
						acquisition.setField61(resp.getMessage());
						acquisition.setField60(idTransaction);
					}
					LOG.info(resp.toString());
					return resp;
				}
		}
		} catch (Exception e) {
			LOG.error("errorSendGim", e);
			return new PaymentDetails(ErrorResponse.UNKNOWN_ERROR.getCode(),
					ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		} finally {
			if (acquisition != null && acquisition.getId() == null) {
				acquisition = saveTransaction(acquisition);
			}
			if (acquisition != null) {
				request.setTransactionId(idTransaction);
				finalize.saveTransaction(request, acquisition, commMon, null,  "GIM",
						caisse.getPointDeVente().getCommercant());

			}
		}
	}
	private byte[] formIsoData(byte[] isob) {
		String strHearder = "000" + (isob.length + 11) + "ISO70100000";
		byte[] head = strHearder.substring(strHearder.length() - 15, strHearder.length()).getBytes();
		byte[] toSend = new byte[head.length + isob.length];
		System.arraycopy(head, 0, toSend, 0, head.length);
		System.arraycopy(isob, 0, toSend, head.length, isob.length);
		return toSend;
	}
	private IsoAcquisition saveTransaction(IsoAcquisition acquisition) {
		try {
			LOG.error("save trx");
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
			if (acquisition.getField2().startsWith(Constantes.BIN_VISA)) {
				acquisition.setTypeCarte(1);
			} else if (acquisition.getField2().startsWith("51") || acquisition.getField2().startsWith("55")
					|| acquisition.getField2().startsWith("50") || acquisition.getField2().startsWith("56")
					|| acquisition.getField2().startsWith("57") || acquisition.getField2().startsWith("58")) {
				acquisition.setTypeCarte(2);
			} else {
				acquisition.setTypeCarte(3);
			}
			acquisition.setDateCreation(new Date());
			IsoAcquisition acqui = (IsoAcquisition) session.saveObject(acquisition);

			LOG.info("acquisition " + acqui.getId());
			return acqui;
		} catch (Exception e) {
			LOG.error("errorSave", e);
			return acquisition;
		}
	}
	private void setFields(IsoAcquisition acquisition, ISOMsg msg) {
		try {
			if (msg.hasField(2)) {
				acquisition.setField2(msg.getString(2));
			}
			if (msg.hasField(3)) {
				acquisition.setField3(msg.getString(3));
			}
			if (msg.hasField(4)) {
				acquisition.setField4(msg.getString(4));
			}
			if (msg.hasField(5)) {
				acquisition.setField5(msg.getString(5));
			}
			if (msg.hasField(6)) {
				acquisition.setField6(msg.getString(6));
			}
			if (msg.hasField(7)) {
				acquisition.setField7(msg.getString(7));
			}
			if (msg.hasField(9)) {
				acquisition.setField9(msg.getString(9));
			}
			if (msg.hasField(10)) {
				acquisition.setField10(msg.getString(10));
			}
			if (msg.hasField(11)) {
				acquisition.setField11(msg.getString(11));
			}
			if (msg.hasField(12)) {
				acquisition.setField12(msg.getString(12));
			}
			if (msg.hasField(14)) {
				acquisition.setField14(msg.getString(14));
			}
			if (msg.hasField(15)) {
				acquisition.setField15(msg.getString(15));
			}
			if (msg.hasField(18)) {
				acquisition.setField18(msg.getString(18));
			}
			if (msg.hasField(19)) {
				acquisition.setField19(msg.getString(19));
			}
			if (msg.hasField(21)) {
				acquisition.setField21(msg.getString(21));
			}
			if (msg.hasField(22)) {
				acquisition.setField22(msg.getString(22));
			}
			if (msg.hasField(23)) {
				acquisition.setField23(msg.getString(23));
			}
			if (msg.hasField(24)) {
				acquisition.setField24(msg.getString(24));
			}
			if (msg.hasField(26)) {
				acquisition.setField26(msg.getString(26));
			}
			if (msg.hasField(27)) {
				acquisition.setField27(msg.getString(27));
			}
			if (msg.hasField(32)) {
				acquisition.setField32(msg.getString(32));
			}
			if (msg.hasField(33)) {
				acquisition.setField33(msg.getString(33));
			}
			if (msg.hasField(35)) {
				acquisition.setField35(msg.getString(35));
			}
			if (msg.hasField(37)) {
				acquisition.setField37(msg.getString(37));
			}
			if (msg.hasField(38)) {
				acquisition.setField38(msg.getString(38));
			}
			if (msg.hasField(39)) {
				acquisition.setField39(msg.getString(39));
			}
			if (msg.hasField(41)) {
				acquisition.setField41(msg.getString(41));
			}

			if (msg.hasField(42)) {
				acquisition.setField42(msg.getString(42));
			}
			if (msg.hasField(43)) {
				acquisition.setField43(msg.getString(43));
			}
			if (msg.hasField(44)) {
				acquisition.setField44(msg.getString(44));
			}
			if (msg.hasField(46)) {
				acquisition.setField46(msg.getString(46));
			}
			if (msg.hasField(48)) {
				acquisition.setField48(msg.getString(48));
			}
			if (msg.hasField(49)) {
				acquisition.setField49(msg.getString(49));
			}
			if (msg.hasField(50)) {
				acquisition.setField50(msg.getString(50));
			}
			if (msg.hasField(51)) {
				acquisition.setField51(msg.getString(51));
			}

			if (msg.hasField(53)) {
				acquisition.setField53(msg.getString(53));
			}
			if (msg.hasField(54)) {
				acquisition.setField54(msg.getString(54));
			}
			if (msg.hasField(56)) {
				acquisition.setField56(msg.getString(56));
			}

			if (msg.hasField(63)) {
				acquisition.setField63(msg.getString(63));
			}
			if (msg.hasField(64)) {
				acquisition.setField64(msg.getString(64));
			}
			if (msg.hasField(103)) {
				acquisition.setField103(msg.getString(103));
			}
			if (msg.hasField(128)) {
				acquisition.setField128(msg.getString(128));
			}
		} catch (Exception e) {
			LOG.error("errorSetField", e);
		}
	}
	

	public PaymentDetails cancel(IsoAcquisition acqu, Transaction trxs, Card merchantCard, Partner commercant) {
		try {
			ISOMsg msg ;
			String template="";
			try(InputStream stream = Files.newInputStream(Paths.get(template))){
			GenericPackager packager = new GenericPackager(stream);
			msg = new ISOMsg();
			msg.setPackager(packager);

			msg.setMTI("1420");
			if (acqu.getRecon() < -1) {
				msg.setMTI("1421");
			}
			msg.set(2, acqu.getField2());
			msg.set(3, acqu.getField3());
			msg.set(4, acqu.getField4());msg.set(6, acqu.getField6());
			msg.set(7, acqu.getField7());msg.set(10, acqu.getField10());
			msg.set(11, acqu.getField11());msg.set(12, acqu.getField12());
			msg.set(14, acqu.getField14());msg.set(15, acqu.getField15());
			msg.set(16, acqu.getField16());msg.set(18, acqu.getField18());
			msg.set(19, acqu.getField19());msg.set(21, acqu.getField21());
			msg.set(22, acqu.getField22());msg.set(23, acqu.getField23());
			msg.set(24, acqu.getField24());msg.set(27, acqu.getField27());
			msg.set(32, acqu.getField32());msg.set(33, acqu.getField33());
			msg.set(37, acqu.getField37());msg.set(38, acqu.getField38());
			msg.set(39, acqu.getField39());msg.set(41, acqu.getField41());
			msg.set(42, acqu.getField42());msg.set(43, acqu.getField43());
			msg.set(49, acqu.getField49());msg.set(50, acqu.getField50());
			msg.set(51, acqu.getField51());msg.set(53, acqu.getField53());
			msg.set(128, HSMHandler.hexStringToByteArray(acqu.getField128()));

			String field56 = "1100" + acqu.getField11();
			field56 = field56 + acqu.getField12() + acqu.getField32();
			msg.set(56, field56);

			if (acqu.getField9() != null) {
				msg.set(9, acqu.getField9());
			}
			return null;
			}
			//return cancelTransaction(msg, acqu, trx, merchantCard, commercant);
		} catch (Exception e) {
			LOG.error("exception to cancel transaction " + acqu.getId());
			LOG.error("error", e);
			return new PaymentDetails(ErrorResponse.UNKNOWN_ERROR, "");
		}
	}

	public PaymentDetails getBalance(PaymentDto request, Caisse caisse, CommissionMonetique commMon) {
		return new PaymentDetails(ErrorResponse.UNKNOWN_ERROR, "not implemented");
	}

	

}
