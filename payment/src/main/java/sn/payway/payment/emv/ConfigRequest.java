package sn.payway.payment.emv;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigRequest {

	
	
	@NotBlank(message = "merchantNumber")
	private String merchantNumber;
	@NotBlank(message = "posNumber")
	private String posNumber;
	@NotBlank(message = "terminalNumber")
	private String terminalNumber;
	@NotBlank(message = "terminalSn")
	@JsonAlias("terminalSN")
	private String terminalSn;
	
	private String name;
	private String value;
	


	@Override
	public String toString() {
		return "ConfigRequest {merchantNumber=" + merchantNumber + ", posNumber=" + posNumber + 
				", terminalNumber="+ terminalNumber + ", terminalSn=" + terminalSn + "}";
	}
	
}
