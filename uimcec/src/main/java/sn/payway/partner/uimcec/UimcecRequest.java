package sn.payway.partner.uimcec;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UimcecRequest {

	
	private Long id;
	private String acquerreurBanque;
	private BigDecimal fees;
	private BigDecimal amount;
	private String acquerreurCode;
	private String acquerreurName;
	private String reference;
	private String channelType;
	private String cardCin;
	private String posNumber;
	private String address;
	private String customerPhone;
}
