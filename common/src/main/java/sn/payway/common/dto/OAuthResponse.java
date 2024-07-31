package sn.payway.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class OAuthResponse {

	@JsonProperty("expires_in")
	private long expiresIn;
	@JsonProperty("refresh_expires_in")
	private long refresnExpiresIn;
	@JsonProperty("token_type")
	private String tokenType;
	@JsonProperty("access_token")
	private String accessToken;
	@JsonProperty("refresh_token")
	private String refreshToken;
	
	@JsonProperty("scope")
	private String scope;
}
