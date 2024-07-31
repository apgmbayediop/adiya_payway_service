package sn.adiya.card.personalization;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jboss.logging.Logger;

import sn.fig.entities.Card;
import sn.fig.entities.Register;

@Stateless
public class PersoUimcecService {

	private static final Logger LOG = Logger.getLogger(PersoUimcecService.class);
	public static final String UIMCEC_BIN="6089670010";
	private static final String UIMCEC = "UIMCEC";
	public static final String INCOMING_UIMCEC = PersonalisationService.INCOMING + "/" + UIMCEC;
	public static final String OUTGOING_UIMCEC = PersonalisationService.OUTGOING + "/" + UIMCEC;
	public static final String ARCHIVES_UIMCEC = PersonalisationService.ARCHIVES + "/" + UIMCEC;
	public static final String UIMCEC_BANK_CODE = "06381";
	public static final String UIMCEC_BANK_NAME = "UIMCEC";

	@Inject
	private PersonalisationService persoService;

	@PostConstruct
	public void init() {
		try {

			File incoming = new File(Paths.get(INCOMING_UIMCEC).toUri());
			File outgoing = new File(Paths.get(OUTGOING_UIMCEC).toUri());
			File archives = new File(Paths.get(ARCHIVES_UIMCEC).toUri());
			if (!incoming.exists()) {
				incoming.mkdirs();
				outgoing.mkdirs();
				archives.mkdirs();
			}

		} catch (Exception e) {
			LOG.error("initPerso", e);
		}
	}

	

	public void processIncomingFiles() {
		File incoming = new File(INCOMING_UIMCEC);
		File[] files=incoming.listFiles();
		   int length = files==null?0:files.length;
		   LOG.info("nombre de fichiers "+length);
		   if(length>0) {
		   HashMap<String, List<Register>>registers = new HashMap<>();
		   for(File file:files) {
			   registers.putAll(readFiles(file));
		   }
		   HashMap<String, List<String>> persoContent =new HashMap<>();
		   List<Long>idCards =new ArrayList<>();
		   idCards.add(0L);
		   for(Entry<String, List<Register>> sheet:registers.entrySet()) {
			  List<Card> cards=persoService.affectCards(sheet.getValue(),UIMCEC_BIN,idCards,3);
			  List<Long> ids=cards.stream().map(Card::getIdCard).collect(Collectors.toList());
			  idCards.addAll(ids);
		  persoContent.put(sheet.getKey(),persoService.generatePersoFile(cards));
		   LOG.info(sheet.getKey()+" nombre de porteurs "+sheet.getValue().size());
		   }
		   if(persoContent.size()>0) {
			   try {
			   for(File file:files) {
			   Files.move(file.toPath(), new File(ARCHIVES_UIMCEC+"/"+System.currentTimeMillis()+"_"+file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
			   
			   }
			   }
			   catch (IOException e) {
				LOG.error("moveError",e);
			}
		   persoService.createOutgoingFile(OUTGOING_UIMCEC,persoContent);
		   }
		}
		

	}

	public HashMap<String, List<Register>> readFiles(File file) {
		HashMap<String, List<Register>> maps = new HashMap<>();
		try (Workbook workbook = new XSSFWorkbook(file)) {
			int size = workbook.getNumberOfSheets();
			LOG.info("nombres de feuilles "+size);
			for (int sh = 0; sh < size; sh++) {
				LOG.info("sheet "+sh);
				maps.put(workbook.getSheetAt(sh).getSheetName(), readSheet(workbook.getSheetAt(sh)));
			}
		} catch (InvalidFormatException e) {
			LOG.error("invalidErr", e);
		} catch (IOException e) {
			LOG.error("invalidErr", e);
		}
		return maps;
	}

	public List<Register> readSheet(Sheet sheet) {
		Iterator<Row> iterator = sheet.iterator();
		iterator.next();
		List<Register> registers = new ArrayList<>();
		while (iterator.hasNext()) {
			Row nextRow = iterator.next();
			String account =persoService.getCellValue(nextRow.getCell(0));
			if(!account.isBlank()) {
				String acct = account.startsWith("0")?account:"0"+account;
				Register register = new Register();
				String phoneCellValue=persoService.getCellValue(nextRow.getCell(5));
				String phoneNumber=persoService.getPhoneNumber(phoneCellValue);
				register.setDate(new Date());
				register.setSessionId(acct);
				register.setFirstname(persoService.getCellValue(nextRow.getCell(1)));
				register.setLastname(persoService.getCellValue(nextRow.getCell(2)));
				register.setBirthdate(persoService.getCellValue(nextRow.getCell(3)));
				register.setBirthplace(persoService.getCellValue(nextRow.getCell(4)));
				register.setPhonenumber(phoneNumber);
				register.setCountry("SN");
				register.setCustomerIndicatif("221");
				register.setDocumentNumber(persoService.getCellValue(nextRow.getCell(6)));
				register.setDocumentType("NI");
				register.setVille(persoService.getCellValue(nextRow.getCell(9)));
				register.setFlashCode(persoService.getCellValue(nextRow.getCell(7)));
				register.setLogin(persoService.getCellValue(nextRow.getCell(8)));
				registers.add(register);
			}
			
		}
		return registers;
	}

	
}
