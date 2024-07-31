package sn.adiya.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class OAuthDto {

	private String clientId;
	private String clientSecret;
	private String grantType;
}
