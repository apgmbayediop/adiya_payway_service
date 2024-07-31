package sn.payway.notification.otp;

import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import sn.apiapg.common.utils.AbstractResponse;

@Path("otp")
public class OTPController {

	
	 
	private NotifyOtpService otp;

	@POST
	public AbstractResponse generate(OtpDto dto) {
		
		return otp.generate(dto);
	}
	
	@PUT
	public AbstractResponse verify(OtpDto dto) {
		
		return otp.check(dto);
	}
	
}
