package sn.adiya.notification.otp;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class OtpDto {

	
   @NotBlank(message = "Veuillez renseigner l object")
	private String object;
   @NotBlank(message = "Veuillez renseigner le telephone")
   private String phone;
   @NotBlank(message = "Veuillez renseigner indicatif du pays")
	private String countryCode;
   @NotBlank(message = "Veuillez renseigner id requete")
	private String requestId;
	
	private String senderId;
	
	private String id;
	private String otp;
}
