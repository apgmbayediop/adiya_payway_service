package sn.adiya.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import sn.fig.common.utils.AbstractResponse;

@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_EMPTY)
public class CancelRequest  extends AbstractResponse{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2479714481993971858L;
	private String reason;
	private boolean push;
	private String bank;
	public CancelRequest(String code, String message) {
		super(code, message);
	}

}
