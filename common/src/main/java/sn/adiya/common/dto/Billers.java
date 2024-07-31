package sn.adiya.common.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial") 
@Getter @Setter
public class Billers implements Serializable {
	private String billerCategory;
	private String billerCategoryCode;
	private String operator;
	private String operatorCode;

	public Billers(String billerCategory, String billerCategoryCode, String operator, String operatorCode) {
		super();
		this.billerCategory = billerCategory;
		this.billerCategoryCode = billerCategoryCode;
		this.operator = operator;
		this.operatorCode = operatorCode;
	}
}
