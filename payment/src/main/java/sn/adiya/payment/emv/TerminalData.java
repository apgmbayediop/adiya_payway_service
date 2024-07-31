package sn.adiya.payment.emv;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import sn.fig.entities.aci.CardKey;

@JsonInclude(Include.NON_EMPTY)
public class TerminalData {

	private String caisseNumber;
	private String countryCode;
	private String currencyCode;
	private String currencyName;
	private String header;
	private String heureTelecollecte;
	private String merchantAddress;
	private String merchantName;
	private String merchantNumber;
	private String merchantCategorie;
	private String merchantType;
	private String posName;
	private String posNumber;
	private String terminalSn;
	private String env;
	
	private List<String>aidList;
	private List<CardKey>listKeys;
	public TerminalData() {
		super();
	}

	public String getMerchantNumber() {
		return merchantNumber;
	}

	public void setMerchantNumber(String merchantNumber) {
		this.merchantNumber = merchantNumber;
	}

	public String getPosNumber() {
		return posNumber;
	}

	public void setPosNumber(String posNumber) {
		this.posNumber = posNumber;
	}

	public String getCaisseNumber() {
		return caisseNumber;
	}

	public void setCaisseNumber(String caisseNumber) {
		this.caisseNumber = caisseNumber;
	}

	public String getTerminalSn() {
		return terminalSn;
	}

	public void setTerminalSn(String terminalSn) {
		this.terminalSn = terminalSn;
	}

	public String getMerchantName() {
		return merchantName;
	}

	public void setMerchantName(String merchantName) {
		this.merchantName = merchantName;
	}

	public String getPosName() {
		return posName;
	}

	public void setPosName(String posName) {
		this.posName = posName;
	}

	public String getHeureTelecollecte() {
		return heureTelecollecte;
	}

	public void setHeureTelecollecte(String heureTelecollecte) {
		this.heureTelecollecte = heureTelecollecte;
	}

	public String getMerchantAddress() {
		return merchantAddress;
	}

	public void setMerchantAddress(String merchantAddress) {
		this.merchantAddress = merchantAddress;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	public String getCurrencyName() {
		return currencyName;
	}

	public void setCurrencyName(String currencyName) {
		this.currencyName = currencyName;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getMerchantType() {
		return merchantType;
	}

	public void setMerchantType(String merchantType) {
		this.merchantType = merchantType;
	}
	

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public List<String> getAidList() {
		return aidList;
	}

	public void setAidList(List<String> aidList) {
		this.aidList = aidList;
	}

	public List<CardKey> getListKeys() {
		return listKeys;
	}

	public void setListKeys(List<CardKey> listKeys) {
		this.listKeys = listKeys;
	}

	
	public String getEnv() {
		return env;
	}

	public void setEnv(String env) {
		this.env = env;
	}
	

	public String getMerchantCategorie() {
		return merchantCategorie;
	}

	public void setMerchantCategorie(String merchantCategorie) {
		this.merchantCategorie = merchantCategorie;
	}

	@Override
	public String toString() {
		return "TerminalData [merchantNumber=" + merchantNumber + ", posNumber=" + posNumber + ", caisseNumber="
				+ caisseNumber + ", terminalSn=" + terminalSn + ", merchantName=" + merchantName + ", posName="
				+ posName + ", heureTelecollecte=" + heureTelecollecte + ", merchantAddress=" + merchantAddress
				+ ", currencyCode=" + currencyCode + ", currencyName=" + currencyName + ", header=" + header
				+ ", merchantType=" + merchantType + "]";
	}
	
}
