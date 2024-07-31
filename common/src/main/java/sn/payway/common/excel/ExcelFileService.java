package sn.payway.common.excel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Stateless;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

@Stateless
public class ExcelFileService {

	private static final Logger LOG = Logger.getLogger(ExcelFileService.class.getSimpleName());
	
	public  boolean createFile(ExcelFileDto dto) {
		
		try {

			LOG.info("***************createFile****************************");
			try(Workbook workbook = new XSSFWorkbook()){
			Sheet sheet = workbook.createSheet(dto.getSheetName());
			Row headerRow = sheet.createRow(0);
			// header
			for (int i = 0; i < dto.getHeaders().size(); i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(dto.getHeaders().get(i));
			}
			int rowNum = 1;
			for (String[] data : dto.getContent()) {
				Row row = sheet.createRow(rowNum++);
				for (int i = 0; i < data.length; i++) {
					row.createCell(i).setCellValue(data[i]);
				}
			}
		
			
			try(OutputStream fileOut = Files.newOutputStream(Paths.get(dto.getPath()))){
			workbook.write(fileOut);
			}
		}
			return true;
		} catch (IOException e) {
			LOG.error("createFile", e);
			return false;
		}
	}
	public <T> List<T>  read(Class<T> cls, InputStream file,Map<String, String> map) throws IOException {
		List<T> cartes =  new ArrayList<>();
		try(Workbook workbook = new XSSFWorkbook(file)){
			Sheet sheet = workbook.getSheetAt(0);
			Row row = sheet.getRow(0);
			Map<String, Integer> headers = new ConcurrentHashMap<>();
			Map<String, Integer> readables = new ConcurrentHashMap<>();
			row.forEach(cell -> {
				String value = cell.getStringCellValue().trim().toUpperCase(Locale.FRANCE);
				if (!value.isBlank()) {
					readables.put(value, cell.getColumnIndex());
				}
			});
			LOG.info(readables);
			readables.forEach((key, value) -> {
				if (map.containsKey(key)) {
					headers.put(map.get(key), readables.get(key));
				}
			});
			LOG.info(headers);
			
			int numLastRow = sheet.getPhysicalNumberOfRows();
			LOG.info(numLastRow);
			for (int i = 1; i < numLastRow; i++) {
				LOG.info(" row "+i);
				T carte = createLine(cls,sheet.getRow(i),headers );
				if (carte != null) {
					cartes.add(carte);
				}
			}
		
		}
		return cartes;
	}
	public String[] headers(int numSheet,int numRow,InputStream input) throws IOException {
		List<String> readables = new ArrayList<>();
		
		try(Workbook workbook = new XSSFWorkbook(input)){
			Sheet sheet = workbook.getSheetAt(numSheet);
			Row row = sheet.getRow(numRow);
			row.forEach(cell -> {
				String value = cell.getStringCellValue().trim().toUpperCase(Locale.FRANCE);
				if (!value.isBlank()) {
					readables.add(value);
				}
			});
		}
		return readables.toArray(new String[] {});
	}
		
	private <T> T createLine( Class<T> cls,Row row, Map<String, Integer> data) {
		if(row ==null) {
			return null;
		}else {
		Map<String, Object> map = new ConcurrentHashMap<>();
		data.forEach((key, value) -> {
			LOG.info(key+" "+value);
			Cell cell = row.getCell(value);
			if(cell == null) {
				LOG.info(key+" is null");
			}else if (cell.getCellType() == CellType.NUMERIC) {
				map.put(key, BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString());
			} else if (cell.getCellType() == CellType.STRING) {
				map.put(key, cell.getStringCellValue());
			}
		});
		ObjectMapper mapp = new ObjectMapper();
		return mapp.convertValue(map, cls);
		}
	}
}
