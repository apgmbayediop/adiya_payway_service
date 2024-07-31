package sn.payway.payment.emv;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import sn.apiapg.common.utils.AbstractResponse;

@JsonInclude(Include.NON_EMPTY)
public class ConfigResponse extends AbstractResponse {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private TerminalData configData;
	private String onUsSecData;
	private String onTheirSecData;
	

	public TerminalData getConfigData() {
		return configData;
	}

	public void setConfigData(TerminalData configData) {
		this.configData = configData;
	}

	public String getOnUsSecData() {
		return onUsSecData;
	}

	public void setOnUsSecData(String onUsSecData) {
		this.onUsSecData = onUsSecData;
	}

	public String getOnTheirSecData() {
		return onTheirSecData;
	}

	public void setOnTheirSecData(String onTheirSecData) {
		this.onTheirSecData = onTheirSecData;
	}

	@Override
	public String toString() {
		return "ConfigResponse {code ="+getCode()+", message = "+getMessage()+", configData=" + configData + ", onUsSecData=" + onUsSecData + 
				", onTheirSecData="+ onTheirSecData + "}";
	}
	
	

}
