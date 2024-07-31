package sn.payway.card.personalization;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.ejb.Stateless;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.jboss.logging.Logger;

import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.MailUtils;
import sn.apiapg.entities.Card;
import sn.apiapg.entities.Register;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;

@Stateless
public class PersonalisationService {

	private static final Logger LOG = Logger.getLogger(PersonalisationService.class);

	public static final String PERSO = BEConstantes.PATH_INIT + "/MONETIQUE/PERSO";
	public static final String INCOMING = PERSO + "/INCOMING";
	public static final String OUTGOING = PERSO + "/OUTGOING";
	public static final String OUTGOING_APG = PERSO + "/OUTGOING/APG";

	public static final String ARCHIVES = PERSO + "/ARCHIVES";

	public static final String EMAIL_TO_NOTIFIY="cheikhouna.diop@afripayway.com,mbaye.diop@afripayway.com";
	public String getPhoneNumber(String phone) {
		if (phone == null || phone.isBlank()) {
			return "";
		} else {

		
		try {
			BigDecimal val =new BigDecimal(phone.replaceAll("[^\\d.E]", ""));	  
			
			return ""+val.longValue();
		} catch (Exception e) {
			return "";
		}
		}
	}
	
	
	public String getCellValue(Cell cell) {
		String value;
		if(cell==null||cell.getCellType() ==null) {
			value ="";
		}else {
		switch (cell.getCellType()) {
		case NUMERIC: value =""+cell.getNumericCellValue();
		if(HSSFDateUtil.isCellDateFormatted(cell)){
			 DateFormat df = new SimpleDateFormat(BEConstantes.FORMAT_DATE_DAY_MM_YYYY);
	          Date date = cell.getDateCellValue();
	          value = df.format(date);
		}else {
			value = new BigDecimal(cell.getNumericCellValue()).toPlainString();
		} break;
		case STRING: value =cell.getStringCellValue();break;
		case BLANK:value="";break;
		default: value="";break;
		}
		}
		return value;
	}
	public List<Card> affectCards(List<Register> registers,String sousbin,List<Long>excludes,int nbYear) {
		List<Card> cardsAllocated=new ArrayList<>();
		
		try  {
		int rows = registers.size();
		Session sess =(Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		List<Card> cards =sess.executeNamedQueryListWithLimit(Card.class, "findAvailableCards", new String[] {"status","excludeList","sousbin"}, 
				new Object[] {"INACTIVE",excludes,sousbin+"%"},rows);
		LOG.info(" nombre de cartes dispo "+cards.size());
		LOG.info("nombre de porteurs "+rows);
		LocalDate expiration = LocalDate.now().plusYears(3).plusMonths(2);
		String exp = DateTimeFormatter.ofPattern("yyMM", Locale.US).format(expiration);
		int holder=0;
		if(rows == cards.size()) {
			for (Card card:cards) {
				Register register = (Register)sess.saveObject(registers.get(holder));
				card.setCinAlias(register.getSessionId());
				card.setExpiryDate(exp);
				card.setRegister(register);
				sess.updateObject(card);
				cardsAllocated.add(card);
				holder++;
			}
			
		}else {
			String msg="Le nombre de carte disponible est de "+cards.size()+" nombre de porteurs "+rows;
			MailUtils.sendEmails(EMAIL_TO_NOTIFIY, "NOMBRE DE CARTES DISPO INSUFISANT", msg, true, null, null);
		}
		}
		catch (Exception e) {
			LOG.error("affectCard",e);
		}
		return cardsAllocated;
	}
	public List<String> generatePersoFile(List<Card> cardsAllocated) {
		List<String> content = new ArrayList<>();
		if(cardsAllocated.isEmpty()) {
			LOG.info("no card generated");
		}else {
		String start=";";
		String bankCode=getBankCode(cardsAllocated.get(0).getPan());// 2 5
		String bankName=StringUtils.rightPad(getBankName(bankCode),32); //8 32
		String agenceCode=bankCode; // 41 5
		String productCode="001";// 47 3
		String sequenceNumber="0000001";//51 7
		String pan;//59 16
		String expirationDate;//76 4
		String holderName;// 81 20
		String accountCode=""; // 15
		String serviceCode="200";//102 3
		String cvv; // 106 3
		String cvv2; //110 3
		String pvv; // 114 4
		String lot="001"; // 119 3
		String dateGenerationHolder = DateTimeFormatter.ofPattern("ddMMyyyy", Locale.FRANCE).format(LocalDateTime.now()); //123 8
		String referenceAPG;// 132 20
		String end ="[END]";
		
		
		for (Card card :cardsAllocated) {
			pan = card.getPan();
			expirationDate = card.getExpiryDate().substring(2)+card.getExpiryDate().substring(0,2);
			cvv = generateDigits(3);
			cvv2 = generateDigits(3);
			pvv = generateDigits(4);
			
			accountCode = card.getCinAlias() ==null?card.getCin():card.getCinAlias();
			accountCode =StringUtils.rightPad(accountCode, 15);
			holderName = StringUtils.rightPad(getHolderName(card),20);
			referenceAPG = StringUtils.rightPad(card.getCin(),40);
			content.add(start+String.join(start, bankCode,bankName,agenceCode,productCode,sequenceNumber,
					pan,expirationDate,holderName,accountCode,serviceCode,cvv,cvv2,pvv,
					lot,dateGenerationHolder,referenceAPG,end));
		}
		}
		return content;
		
	}
	private String getBankCode(String pan) {
		String bankCode="00000";
		if(pan.startsWith(PersoUimcecService.UIMCEC_BIN)) {
			bankCode=PersoUimcecService.UIMCEC_BANK_CODE;
		}
		return bankCode;
	}
	private String getBankName(String bankCode) {
		String bankName="APG";
		if(bankCode.startsWith(PersoUimcecService.UIMCEC_BANK_CODE)) {
			bankName=PersoUimcecService.UIMCEC_BANK_NAME;
		}
		return bankName;
	}
	private String generateDigits(int length) {
		 StringBuilder builder = new StringBuilder(3);
		 SecureRandom random = new SecureRandom();
		 while(builder.length()<length) {
			 builder.append(random.nextInt(10));
		 }
		 return builder.toString();
	}
	private String  getHolderName(Card card) {
		String firstName = card.getRegister().getFirstname()==null?"":card.getRegister().getFirstname().replaceAll("\\s+", " ").trim();
		String lastName=card.getRegister().getLastname()==null?"":card.getRegister().getLastname().replaceAll("\\s+", " ").trim();
		String name = lastName.length()>=20?lastName:firstName+" "+lastName;
		
		try {
		firstName =firstName.trim();
		lastName = lastName.trim();
		name = name.trim();
		if(name.isBlank()) {
			name=card.getCin();
		}else if(name.length()>20) {
			 int len = 20-lastName.length();
			 int minLen = len<firstName.length()?len:firstName.length();
			 name = firstName.substring(0, minLen-1)+" "+lastName;
		}
		}
		catch (Exception e) {
			LOG.error(name);
			LOG.error("holderName",e);
		}
		return Normalizer.normalize(name, Normalizer.Form.NFKD).replaceAll("\\p{M}", "").toUpperCase(Locale.US);
	}
	public void createOutgoingFile(String outgoingPath, HashMap<String, List<String>> persoContent) {
		try {
		String date=DateTimeFormatter.ofPattern("ddMMyyyyHHmm", Locale.FRANCE).withZone(ZoneId.of("UTC")).format(LocalDateTime.now());
		for(Entry<String, List<String>> file:persoContent.entrySet()) {
			if(file.getValue().isEmpty()) {
				String msg="carte non generees pour la feuille "+file.getKey();
				MailUtils.sendEmails(EMAIL_TO_NOTIFIY, "FEUILLE NON TRAITE", msg, true, null, null);
			}else {
			Path path =Paths.get(outgoingPath+"/"+file.getKey()+"_"+date);
			Files.write(path, file.getValue(),StandardCharsets.UTF_8, StandardOpenOption.CREATE);
			}
		}
		}
		catch (IOException e) {
			LOG.error("outGO",e);
		}
		
	}
}
