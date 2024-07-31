package sn.payway.common.hsm;

import lombok.Data;

@Data
public class CheckPinDto {

	private String pan;
	private String pin;
	private String pinBlock;
	private String panSeq;
	private String timestamp;
	private String transactionId;
	private String zpk;
}
