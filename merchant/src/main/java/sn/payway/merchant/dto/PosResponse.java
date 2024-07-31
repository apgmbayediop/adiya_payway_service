package sn.payway.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import sn.apiapg.common.utils.AbstractResponse;

@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonInclude(value = Include.NON_EMPTY)
public class PosResponse extends AbstractResponse {/**
	 * 
	 */
	private static final long serialVersionUID = -3620892090601758233L;

	private String posNumber;
	private String merchantNumber;
	private String merchantName;
	private String terminalNumber;
	private String serialNumber;
}
