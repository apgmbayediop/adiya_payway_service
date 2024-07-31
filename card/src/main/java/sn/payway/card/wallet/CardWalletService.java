package sn.payway.card.wallet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sn.apiapg.common.exception.ErrorResponse;
import sn.apiapg.entities.Card;
import sn.apiapg.entities.Partner;
import sn.payway.card.wallet.dto.CreateWalletResponse;
import sn.payway.common.utils.Constantes;
import sn.payway.common.utils.PaywayCommonTools;

@Stateless
public class CardWalletService {

	public static final Logger LOG = Logger.getLogger(CardWalletService.class);
	@Inject
	private PaywayCommonTools tools;
	
	public CreateWalletResponse createWallet(Card card,Partner partner) {
		CreateWalletResponse resp;
		try {
			//String notSet = "NON RENSEIGNE";
			ObjectNode node = new ObjectMapper().createObjectNode();
			String timestamp = Long.toString(System.currentTimeMillis());
			node.put("fromWalletId", card.getRegister().getPhonenumber());
			node.put("prefixeWalletId", partner.getPrefixeWallet());
			node.put("walletId", card.getRegister().getPhonenumber());
			node.put("partnerName", partner.getName());
			node.put("partnerId", partner.getIdPartner());
			node.put("customerIndicatif", partner.getCountryIndicatif());
			node.put("timestamp", timestamp);
			node.put("customerFirstName", card.getRegister().getFirstname());
			node.put("customerLastName",card.getRegister().getLastname());
			node.put("fromCountry", partner.getCountryIsoCode());
			
			try (CloseableHttpClient http = HttpClients.createDefault()) {
				String url = tools.getProperty(Constantes.WILDFLY_LOCAL) + "/api/gateway/operations/v1/createWallet";
				LOG.info(url);
				HttpPost post = new HttpPost(url);
				post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

				ObjectMapper mapper = new ObjectMapper();
				String json = node.toString();
				StringEntity entity = new StringEntity(json);
				post.setEntity(entity);
				try (CloseableHttpResponse response = http.execute(post)) {
					LOG.info(json);
					LOG.info(response.getStatusLine().getStatusCode());
					String textRsp = new String(response.getEntity().getContent().readAllBytes(),
							StandardCharsets.UTF_8);
					LOG.info(textRsp);
					resp = mapper.readValue(textRsp, CreateWalletResponse.class);

				}
			}
		} catch (IOException e) {
			LOG.error("createWallet",e);
			resp = new CreateWalletResponse();
			resp.setCode(ErrorResponse.UNKNOWN_ERROR.getCode());
			resp.setMessage(ErrorResponse.UNKNOWN_ERROR.getMessage(""));
		}
		return resp;
	}
	
	
}
