package sn.adiya.common.excel;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@NoArgsConstructor
@Getter
@Setter
@ToString
public class ExcelFileDto {

	
	
	private List<String>headers;
	private List<String[]> content;
	private String path;
	private String sheetName;
}
