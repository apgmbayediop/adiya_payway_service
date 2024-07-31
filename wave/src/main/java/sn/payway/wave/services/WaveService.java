package sn.payway.wave.services;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import lombok.extern.jbosslog.JBossLog;
import sn.apiapg.common.config.entities.ToolsPartner;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.BEConstantes;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.entities.Partner;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.utils.Constantes;
import sn.payway.common.utils.PaywayCommonTools;
import sn.payway.wave.dto.WaveRequest;

@Stateless
@JBossLog
public class WaveService {

	public static final String WAVE_CODE = "94422";
	public static final String PROCESSING_RESPONSE = "processing";
	public static final String PAY_STATUS_SUCCESS = "succeeded";
	public static final String PAY_STATUS_FAIL = "cancelled";
	public static final String PAY_STATUS_EXPIRED = "expired";

	public static final String KEY_DEBIT = "WAVE_KEY_DEBIT";
	public static final String BASE_URL = "WAVE_BASE_URL_DEBIT";
	// private static final String WAVE_CALLBACK_URL =
	// "/payline/v1/payment/hooks/wave";
	public static final String SUCCESS_URL = "/payline/operation/txn/success/";
	public static final String ERROR_URL = "/payline/operation/txn/failure/";
	private String baseUrl;

	@Inject
	private PaywayCommonTools tools;

	@PostConstruct
	public void init(){
		Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		ToolsPartner tBaseUrl = sess.findObjectById(ToolsPartner.class, null, WaveService.BASE_URL);
		try {
			baseUrl = APIUtilVue.getInstance().apgDeCrypt(tBaseUrl.getValue());
		} catch (UnsupportedEncodingException e) {
			log.error("initWave",e);
		}
	}

	public WaveRequest payment(WaveRequest req,String key) {
		WaveRequest wavResp;
		try {
			log.info("create payment wave");

			try (CloseableHttpClient http = HttpClientBuilder.create().build()) {
				HttpPost post = new HttpPost(baseUrl + "/v1/checkout/sessions");
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				post.addHeader(HttpHeaders.AUTHORIZATION, key);
				JsonMapper mapper = new JsonMapper();
				String body = mapper.writeValueAsString(req);
				StringEntity entity = new StringEntity(body);
				post.setEntity(entity);
				log.info(body);
				try (CloseableHttpResponse resp = http.execute(post)) {
					String strResp = EntityUtils.toString(resp.getEntity());
					log.info(strResp);
					wavResp = new ObjectMapper().readValue(strResp, WaveRequest.class);
					String code = PROCESSING_RESPONSE.equals(wavResp.getPaymentStatus())?Constantes.ISO_PENDING_STATUS:Constantes.ISO_UNKNOWN_ERROR_STATUS;
					wavResp.setCode(code);
					wavResp.setMessage(strResp);
				}
			}
		} catch (Exception e) {
			log.error("WVerror", e);
			wavResp = new WaveRequest();
			wavResp.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
		}
		return wavResp;
	}

	public WaveRequest callback(String object) {
		log.info("waveCallback");
		log.info(object);
		return new WaveRequest();
	}

	public WaveRequest checkTransaction(String id,String key) {
		WaveRequest response;
		try {

			if(id.startsWith("cos-")) {
			response = checkStatus(key, id);
			
			}else {
				response = new WaveRequest();
				response.setPaymentStatus(BEConstantes.STATUS_TRANSACTION_PENDING);
			}

		} catch (Exception e) {
			log.error("updWave", e);
			response = new WaveRequest();
			response.setPaymentStatus(BEConstantes.STATUS_TRANSACTION_PENDING);

		}
		return response;
	}

	public WaveRequest refund(String id,String key) {
		WaveRequest response = new WaveRequest();
		try {

			try (CloseableHttpClient http = HttpClientBuilder.create().build()) {
				HttpPost post = new HttpPost(baseUrl + "/v1/checkout/sessions/" + id + "/refund");
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				post.addHeader(HttpHeaders.AUTHORIZATION, key);
				try (CloseableHttpResponse resp = http.execute(post)) {

					log.info("response = " + resp.getStatusLine().getStatusCode());
					if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
					} else {
						response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
					}

				}
			}

		} catch (Exception e) {
			log.error("refundWave", e);
			response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());

		}
		return response;
	}

	private WaveRequest checkStatus(String key, String tokentransaction) {
		WaveRequest waveResponse;
		try (CloseableHttpClient http = HttpClientBuilder.create().build()) {
			HttpGet post = new HttpGet(baseUrl + "/v1/checkout/sessions/" + tokentransaction);
			post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
			post.addHeader(HttpHeaders.AUTHORIZATION, key);

			try (CloseableHttpResponse resp = http.execute(post)) {
				String strResp = EntityUtils.toString(resp.getEntity());
				// log.info(strResp);
				waveResponse = new ObjectMapper().readValue(strResp, WaveRequest.class);
				String responsePayer = String.join("//", waveResponse.getPaymentStatus(), waveResponse.getId(),
						waveResponse.getCheckoutStatus(), waveResponse.getWhenCompleted(),
						waveResponse.getWhenCreated());
				waveResponse.setMessage(responsePayer);
				waveResponse.setCode(Constantes.ISO_PENDING_STATUS);
				if (PAY_STATUS_SUCCESS.equals(waveResponse.getPaymentStatus())) {
					waveResponse.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
				} else if (PAY_STATUS_FAIL.equals(waveResponse.getPaymentStatus())) {
					waveResponse.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
				} else if (PAY_STATUS_EXPIRED.equals(waveResponse.getCheckoutStatus())) {
					waveResponse.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
					waveResponse.setPaymentStatus(PAY_STATUS_FAIL);
				}

			}
		} catch (Exception e) {
			log.error("checkStatus", e);
			waveResponse = new WaveRequest();
			waveResponse.setPaymentStatus(BEConstantes.STATUS_TRANSACTION_PENDING);
		}
		return waveResponse;
	}

	private WaveRequest createMerchant(String key, WaveRequest request) {
		WaveRequest waveResponse;
		try {
			try (CloseableHttpClient http = HttpClientBuilder.create().build()) {
				HttpPost post = new HttpPost(baseUrl + "/v1/aggregated_merchants");
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				post.addHeader(HttpHeaders.AUTHORIZATION, key);
				JsonMapper mapper = new JsonMapper();
				String body = mapper.writeValueAsString(request);
				StringEntity entity = new StringEntity(body);
				post.setEntity(entity);
				try (CloseableHttpResponse resp = http.execute(post)) {
					String strResp = EntityUtils.toString(resp.getEntity());
					log.info(strResp);
					waveResponse = new ObjectMapper().readValue(strResp, WaveRequest.class);
					waveResponse.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
					if (waveResponse.getId() == null) {
						waveResponse.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());

					}
				}
			}

		} catch (Exception e) {
			waveResponse = new WaveRequest();
			waveResponse.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
		}
		return waveResponse;
	}

	public WaveRequest getMerchant(String key, Partner partner) throws UnsupportedEncodingException {
		String codeMerchant = "WAVE_AGGR_MERCHANT";
		if (Constantes.PREPROD_ENV.equals(tools.getProperty(Constantes.ENV_PRPOPERTY))
				|| Constantes.PROD_ENV.equals(tools.getProperty(Constantes.ENV_PRPOPERTY))) {
			codeMerchant = codeMerchant + "_" + partner.getIdPartner();
		}
		Session sess = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		ToolsPartner code = sess.findObjectById(ToolsPartner.class, null, codeMerchant);
		WaveRequest merch = new WaveRequest();
		merch.setName(partner.getName());
		merch.setBusinessRegistrationIdentifier(partner.getIdPartner().toString());
		merch.setBusinessType("other");
		if (code == null) {
			WaveRequest resp = createMerchant(key, merch);
			if (ErrorResponse.REPONSE_SUCCESS.getCode().equals(resp.getCode())) {
				merch.setId(resp.getId());
				code = new ToolsPartner();
				code.setCode(codeMerchant);
				code.setDate(new Date());
				code.setDescription("sous marchand wave");
				code.setKey(codeMerchant);
				code.setPartner(partner);
				code.setValue(APIUtilVue.getInstance().encod(resp.getId()));
				sess.saveObject(code);
			} else {
				throw new UnsupportedEncodingException();
			}
		} else {
			merch.setId(APIUtilVue.getInstance().apgDeCrypt(code.getValue()));
		}
		return merch;
	}
	
}
