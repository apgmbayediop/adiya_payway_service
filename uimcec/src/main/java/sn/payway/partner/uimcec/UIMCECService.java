package sn.payway.partner.uimcec;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sn.apiapg.common.config.entities.ToolsPartner;
import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.common.utils.APIUtilVue;
import sn.apiapg.common.utils.BeanLocator;
import sn.apiapg.common.utils.ChannelResponse;
import sn.apiapg.entities.Card;
import sn.apiapg.entities.Partner;
import sn.apiapg.session.Session;
import sn.apiapg.session.SessionBean;
import sn.payway.common.utils.Constantes;


@Stateless
public class UIMCECService {

	private static final Logger LOG = Logger.getLogger(UIMCECService.class);
	public static final String UIMCEC_URL = "UIMCEC_URL";
	public static final String UIMCEC_USERNAME = "UIMCEC_USERNAME";
	public static final String UIMCEC_PASSWORD = "UIMCEC_PASSWORD";
	public static final String CODE_UIMCEC = "122783";
	public static final String UIMCEC_AUTH = "UIMCEC_AUTH";
	private static final String PRM_METHODE="methode";
	private static final String PRM_REFERENCE_OP="referenceope";
	private static final String PRM_TOKEN="token";
	private static final String MESSAGE_TYPE="messagetype";
	private static final String CARD_CIN ="cardCin";
	public static final String BIN_UIMCEC="6089670010";
	private static final String PAN="pan";
	private static final String PRM_METHOD_ISO8583="iso8583";
	
	

	private String baseUrl;
	private String username;
	private String password;
	private Partner partner;
	
	//private PropertTools config;

	@PostConstruct
	public void init() {

		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

		List<ToolsPartner> params = session.executeNamedQueryList(ToolsPartner.class, "findTPByPCode",
				new String[] { "code" }, new String[] { CODE_UIMCEC });
		try {
			partner = params.get(0).getPartner();
			for (ToolsPartner tool : params) {
				if (UIMCEC_URL.equals(tool.getCode())) {
					baseUrl = APIUtilVue.getInstance().apgDeCrypt(tool.getValue());
				}
				if (UIMCEC_USERNAME.equals(tool.getCode())) {
					username = APIUtilVue.getInstance().apgDeCrypt(tool.getValue());
				}
				if (UIMCEC_PASSWORD.equals(tool.getCode())) {
					password = APIUtilVue.getInstance().apgDeCrypt(tool.getValue());
				}
			}
		} catch (UnsupportedEncodingException e) {
			LOG.error("initCagecfi",e);
		}
	}

	private UimcecPayDto authentication() {
		UimcecPayDto response = new UimcecPayDto();
		response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
		response.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		try {
			LOG.info("CAGEFI token");
			try(CloseableHttpClient http = HttpClients.custom()
					.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
			        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
			        .build() ){
			HttpPost post = new HttpPost(baseUrl);
			post.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.createObjectNode();
			((ObjectNode) node).put(PRM_METHODE, "token");
			((ObjectNode) node).put(PRM_REFERENCE_OP, Long.toString(System.currentTimeMillis()));
			((ObjectNode) node).put("agentcode", username);
			((ObjectNode) node).put("agentpasse", password);
			String json = mapper.writeValueAsString(node);
			StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
			LOG.info(EntityUtils.toString(entity));
			post.setEntity(entity);
			String respStr;
			try(CloseableHttpResponse resp = http.execute(post)){
			LOG.info(resp.getStatusLine().getStatusCode());
		
			respStr = EntityUtils.toString(resp.getEntity());
			LOG.info("response auth uimcec "+respStr);
			response = mapper.readValue(respStr, UimcecPayDto.class);
			LOG.info(response.getCodeMessage());
			LOG.info(response.getReferencePayer());
			if ("100000".equals(response.getCodeMessage())) {
				response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
				Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
				ToolsPartner tool = session.executeNamedQuerySingle(ToolsPartner.class, "findToolsPById",
						new String[] { "code" }, new String[] { UIMCEC_AUTH });
				String value = APIUtilVue.getInstance().encod(response.getReferencePayer());
				if (tool == null) {
					LOG.info("saving token");
					tool = new ToolsPartner();
					tool.setDate(new Date());
					tool.setPartner(partner);
					tool.setCode(UIMCEC_AUTH);
					tool.setKey(UIMCEC_AUTH);
					tool.setValue(value);
					session.saveObject(tool);
				} else {
					tool.setValue(value);
					tool.setDate(new Date());
					session.updateObject(tool);
				}
			} else {
				response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			}
			}
			} catch (KeyManagementException e) {
				LOG.error("KeyManErrore",e);
			} catch (NoSuchAlgorithmException e) {
				LOG.error("AlgoError",e);
			} catch (KeyStoreException e) {
				LOG.error("KeyStoreError",e);
			}
		} catch (IOException e) {
		     LOG.error("errorAuth",e);
		}
		return response;
		
	}
	
	public BigDecimal balance(UimcecRequest request) {
		BigDecimal balance = BigDecimal.ZERO;
		try {
			LOG.info("CAGEFI balance");
			try(CloseableHttpClient http =HttpClients.custom()
					.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
			        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
			        .build()){
			String url = baseUrl;
			HttpPost post = new HttpPost(url);
			post.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

			String token =token();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.createObjectNode();
			((ObjectNode) node).put(PRM_METHODE, PRM_METHOD_ISO8583);
			((ObjectNode) node).put(PRM_TOKEN, token);
			((ObjectNode) node).put(PRM_REFERENCE_OP, System.currentTimeMillis());
			((ObjectNode) node).put(MESSAGE_TYPE, "1200");
			((ObjectNode) node).put("acquereurbanque", "505500");
			((ObjectNode) node).put("acquereurcode", request.getPosNumber());
			((ObjectNode) node).put("acquereurlocation", request.getAddress());
			((ObjectNode) node).put(PAN, request.getCardCin());
			((ObjectNode) node).put("processingcode", "3120");
			((ObjectNode) node).put(CARD_CIN, request.getCardCin());
			((ObjectNode) node).put("montant", BigDecimal.ZERO);
			((ObjectNode) node).put("fraisoperation", BigDecimal.ZERO);
			((ObjectNode) node).put("cardcintransfert", "");
			((ObjectNode) node).put("devise", "952");
			String json = mapper.writeValueAsString(node);
			StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
			LOG.info(EntityUtils.toString(entity));
			post.setEntity(entity);
			try(CloseableHttpResponse resp = http.execute(post)){
			LOG.info(resp.getStatusLine().getStatusCode());
			String respStr = EntityUtils.toString(resp.getEntity());
			UimcecPayDto response = mapper.readValue(respStr, UimcecPayDto.class);
			LOG.info(respStr);
			if (Constantes.ISO_SUCCESS_STATUS.equals(response.getCodeMessage())) {
			        balance = response.getBalance();	
			} 
			}
			}
			
		
	} catch (KeyManagementException e) {
		LOG.error("KeyManE",e);
	} catch (NoSuchAlgorithmException e) {
		LOG.error("NoSuchAlg",e);
	} catch (KeyStoreException e) {
		LOG.error("KeyStore",e);
	}
		catch (IOException e) {
			LOG.error("error",e);
		}
		return balance;
	}
	public UimcecPayDto authorization(UimcecRequest request) {
		UimcecPayDto response = new UimcecPayDto();
		response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
		response.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		try {
			LOG.info("CAGEFI authorization");
			try(CloseableHttpClient http = HttpClients.custom()
					.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
			        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
			        .build()){
			String url = baseUrl;
			HttpPost post = new HttpPost(url);
			post.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

			String token =token();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.createObjectNode();
			String processCode = ChannelResponse.CARD2PAY.getCode().equals(request.getChannelType())?"0120":"1720";
			String reference = Long.toString(request.getId()).concat(request.getReference());
			((ObjectNode) node).put(PRM_METHODE, PRM_METHOD_ISO8583);
			((ObjectNode) node).put(PRM_TOKEN, token);
			((ObjectNode) node).put(PRM_REFERENCE_OP, reference);
			((ObjectNode) node).put(MESSAGE_TYPE, "1100");
			((ObjectNode) node).put("acquereurbanque", request.getAcquerreurBanque());
			((ObjectNode) node).put("acquereurcode", request.getAcquerreurCode());
			((ObjectNode) node).put("acquereurlocation", request.getAcquerreurName());
			((ObjectNode) node).put(PAN, request.getCardCin());
			((ObjectNode) node).put("processingcode",processCode);
			((ObjectNode) node).put("cardCin", request.getCardCin());
			((ObjectNode) node).put("montant", request.getAmount().intValue());
			((ObjectNode) node).put("fraisoperation", request.getFees().intValue());
			((ObjectNode) node).put("cardcintransfert", "");
			((ObjectNode) node).put("devise", "952");
			String json = mapper.writeValueAsString(node);
			StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
			LOG.info(EntityUtils.toString(entity));
			post.setEntity(entity);
			try(CloseableHttpResponse resp = http.execute(post)){
			LOG.info(resp.getStatusLine().getStatusCode());
			String respStr = EntityUtils.toString(resp.getEntity());
			 response = mapper.readValue(respStr, UimcecPayDto.class);
			String content =String.join("|","1100",request.getAcquerreurBanque(),
					request.getAcquerreurCode(),reference,processCode,response.getReferencePayer());
			response.setResponsePayer(content);
			LOG.info(respStr);
			if (Constantes.ISO_SUCCESS_STATUS.equals(response.getCodeMessage())) {
				response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
				
			} else {
				response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			}
			}
			}
		}
		 catch (KeyManagementException e) {
				LOG.error("KeyMa",e);
			} catch (NoSuchAlgorithmException e) {
				LOG.error("NoSuchAlg",e);
			} catch (KeyStoreException e) {
				LOG.error("KeyStore",e);
			}
		catch (IOException e) {
			LOG.error("errorAuth",e);
		}
		return response;
		
	}
	
	public UimcecPayDto cancel(UimcecRequest req) {
		UimcecPayDto response = new UimcecPayDto();
		response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
		response.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));

		try {
			LOG.info("uimcec cancel");
			try(CloseableHttpClient http =HttpClients.custom()
					.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
			        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
			        .build()){
			String url = baseUrl;
			HttpPost post = new HttpPost(url);
			post.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
			String token =token();
			String processCode = ChannelResponse.CARD2PAY.getCode().equals(req.getChannelType())?"0120":"1720";
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.createObjectNode();
			BigDecimal amount =req.getAmount();
			BigDecimal fees = req.getFees();
			((ObjectNode) node).put(PRM_METHODE, PRM_METHOD_ISO8583);
			((ObjectNode) node).put(PRM_TOKEN, token);
			((ObjectNode) node).put(PRM_REFERENCE_OP, req.getReference());
			((ObjectNode) node).put(MESSAGE_TYPE, "1420");
			((ObjectNode) node).put("acquereurbanque", req.getAcquerreurBanque());
			((ObjectNode) node).put("acquereurcode", req.getAcquerreurCode());
			((ObjectNode) node).put("acquereurlocation", req.getAcquerreurName());
			((ObjectNode) node).put(PAN, req.getCardCin());
			((ObjectNode) node).put("processingcode",processCode);
			((ObjectNode) node).put("cardCin", req.getCardCin());
			((ObjectNode) node).put("montant", amount.intValue());
			((ObjectNode) node).put("fraisoperation",fees.intValue());
			((ObjectNode) node).put("cardcintransfert", "");
			((ObjectNode) node).put("devise", "952");
			final String json = mapper.writeValueAsString(node);
			StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
			post.setEntity(entity);
			String respStr;
			try(CloseableHttpResponse resp = http.execute(post)){
			LOG.info(resp.getStatusLine().getStatusCode());
			respStr = EntityUtils.toString(resp.getEntity());
			}
			response = mapper.readValue(respStr, UimcecPayDto.class);
			LOG.info(respStr);
			if ("000".equals(response.getCodeMessage())) {
				response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
				
			} else {
				response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			}
			}
		}
		 catch (KeyManagementException e) {
				LOG.error("KeyMan",e);
			} catch (NoSuchAlgorithmException e) {
				LOG.error("NoSuchAlg",e);
			} catch (KeyStoreException e) {
				LOG.error("KeyStore",e);
			}
		catch (IOException e) {
			LOG.error("errorCancel",e);
			}
		return response;
	}

	public String token() throws UnsupportedEncodingException {
		LOG.info("find token");
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		ToolsPartner tool = session.executeNamedQuerySingle(ToolsPartner.class, "findToolsPById", new String[] { "code" },
				new String[] { UIMCEC_AUTH });
		UimcecPayDto resp ;
		String token;
		if (tool == null) {
			resp = authentication();
			token = resp.getReferencePayer();
		}else {
		Date date = tool.getDate();
		Instant time = date.toInstant();
		Instant now = Instant.now();
		Long diff = ChronoUnit.MINUTES.between(time, now);
		LOG.info("diff "+diff);
	
		long limit = 55L;
		if (diff >= limit) {
			resp = authentication();
			token = resp.getReferencePayer();
		}else {
		token = APIUtilVue.getInstance().apgDeCrypt(tool.getValue());
		}
	}
		return token;
	}

	public boolean isCagefi(Card card) {
		String pan = card.getPan();
		return pan.startsWith(BIN_UIMCEC);
	}
	
	public UimcecUserDto findUser(Card card) {
		UimcecUserDto response = new UimcecUserDto();
		response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
		response.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		try {
			LOG.info("CAGECFI findUser");
			try(CloseableHttpClient http = HttpClients.custom()
					.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
			        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
			        .build()){
			String url = baseUrl;
			HttpPost post = new HttpPost(url);
			post.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

			String token =token();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.createObjectNode();
			String reference = DateTimeFormatter.ofPattern("yyyyMMddHHmmss",Locale.FRANCE).withZone(ZoneId.systemDefault()).format(Instant.now());
			((ObjectNode) node).put(PRM_METHODE, PRM_METHOD_ISO8583);
			((ObjectNode) node).put(PRM_TOKEN, token);
			((ObjectNode) node).put(PRM_REFERENCE_OP, reference);
			((ObjectNode) node).put(MESSAGE_TYPE, "signaletique");
			((ObjectNode) node).put(PAN, card.getCin());
			((ObjectNode) node).put(CARD_CIN,card.getCin());
			((ObjectNode) node).put(CARD_CIN, card.getCin());
			String json = mapper.writeValueAsString(node);
			StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
			LOG.info(json);
			post.setEntity(entity);
			try(CloseableHttpResponse resp = http.execute(post)){
			LOG.info(resp.getStatusLine().getStatusCode());
			String respStr = EntityUtils.toString(resp.getEntity());
			response = mapper.readValue(respStr, UimcecUserDto.class);
			LOG.info(response.getCodeMessage());
			if (Constantes.ISO_SUCCESS_STATUS.equals(response.getCodeMessage())) {
				response.setCode(ErrorResponse.REPONSE_SUCCESS.getCode());
				
						
			} else {
				response.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
				response.setMessage(response.getDescription());
			}
			}
			} catch (KeyManagementException e) {
				LOG.error("KeyMn",e);
			} catch (NoSuchAlgorithmException e) {
				LOG.error("NoAlgo",e);
			} catch (KeyStoreException e) {
				LOG.error("keyStore",e);
			}
		} catch (IOException e) {
			LOG.error("errorAuth",e);
		}
		return response;
		
	}

	
}
