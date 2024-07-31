package sn.payway.notification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class SMSRequest {

	
	private String senderId;
	private String to;
	private String message;
	private String indicatif;
}
