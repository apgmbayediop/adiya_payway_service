package sn.adiya.notification.otp;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;

import javax.ejb.Stateless;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.logging.Logger;

import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.AbstractResponse;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.OTP;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;

@Stateless
public class NotifyOtpService {

	private static final Logger LOG = Logger.getLogger(NotifyOtpService.class);

	public AbstractResponse generate(OtpDto dto) {
		AbstractResponse resp = new AbstractResponse();

		resp.setCode(ErrorResponse.AUTHENTICATION_ERRORS_1701.getCode());
		OTP otp = new OTP();
		String code = generateCode(5);
		LOG.info("otp " + code);
		Instant dateWriting = Instant.now(Clock.system(ZoneId.of("GMT")));
		String data = dateWriting.toEpochMilli() + code + dto.getRequestId() + dto.getPhone();
		String encCode = DigestUtils.md5Hex(data);
		otp.setCode(encCode);
		otp.setDateWriting(dateWriting);
		otp.setPhoneNumber(dto.getPhone());
		otp.setRequestId(dto.getRequestId());
		otp.setStatus("AVAILABLE");
		otp.setObject(dto.getObject());
		Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		otp = (OTP) sess.saveObject(otp);
		resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
		resp.setMessage(otp.getId().toString());
		String sender = dto.getSenderId() == null ? "APGSA" : dto.getSenderId();
		String message = "Votre code de validation " + code + ". Ce code est valable  15 minutes";
		Executors.newSingleThreadExecutor()
				.execute(() -> APIUtilVue.getInstance().sendSMS(sender, dto.getCountryCode(), dto.getPhone(), message));

		return resp;
	}

	public AbstractResponse check(OtpDto dto) {
		Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

		OTP otp = sess.findObjectById(OTP.class, Long.parseLong(dto.getId()), null);
		AbstractResponse resp = new AbstractResponse();
		if (otp == null) {
			resp.setCode(ErrorResponse.TRANSACTION_NOT_FOUND.getCode());
		} else {
			Instant now = Instant.now(Clock.system(ZoneId.of("GMT")));
			long diff = ChronoUnit.MINUTES.between(otp.getDateWriting(), now);
			LOG.info("difference " + diff);
			long limitTime = 15L;
			if (diff > limitTime) {
				otp.setStatus("EXPIRED");
				sess.updateObject(otp);
				resp.setCode(ErrorResponse.DELAY_EXPIRED.getCode());
				resp.setMessage("Code invalide");
			} else {
				String data = otp.getDateWriting().toEpochMilli() + dto.getOtp() + otp.getRequestId()
						+ otp.getPhoneNumber();
				String encCode = DigestUtils.md5Hex(data);
				LOG.info(encCode);
				LOG.info(otp.getCode());
				if (encCode.equals(otp.getCode())) {
					otp.setStatus("VERIFIED");
					sess.updateObject(otp);
					resp.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
					resp.setMessage("VERIFIED");
				} else {
					otp.setStatus("FAILED");
					sess.updateObject(otp);
					resp.setCode(ErrorResponse.INVALID_OTP.getCode());
					resp.setMessage("code invalide");
				}
				sess.updateObject(otp);
			}
		}
		return resp;
	}

	private String generateCode(int length) {

		StringBuilder builder = new StringBuilder();
		SecureRandom ran = new SecureRandom();
		for (int i = 0; i < length; i++) {
			builder.append(ran.nextInt(10));
		}
		return builder.toString();
	}
}
