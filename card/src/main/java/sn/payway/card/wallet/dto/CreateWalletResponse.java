package sn.payway.card.wallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import sn.apiapg.common.utils.AbstractResponse;

@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateWalletResponse extends AbstractResponse{/**
	 * 
	 */
	private static final long serialVersionUID = 8482516889355721682L;
	
	
	private String walletId;

}
