package sn.payway.notification.sms;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import lombok.extern.jbosslog.JBossLog;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.APIUtilVue;
import sn.payway.notification.dto.SMSRequest;
import sn.payway.notification.dto.SMSResponse;

@Path("sms")
@JBossLog
public class SmsController {

	
	private APIUtilVue utilVue = APIUtilVue.getInstance();
	
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public SMSResponse sendSMS(SMSRequest sms) {
		
		log.info(sms);
		utilVue.sendSMS(sms.getSenderId(), sms.getIndicatif(), sms.getTo(), sms.getMessage());
		SMSResponse response = new SMSResponse();
		response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
		response.setMessage("SUCCESS");
		return response;
	}
	
}
