package sn.adiya.security.apikey;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Executors;

import javax.ejb.Stateless;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

import lombok.extern.jbosslog.JBossLog;
import sn.fig.common.utils.BeanLocator;
import sn.fig.common.utils.MailUtils;
import sn.fig.entities.Partner;
import sn.fig.entities.PartnerApiKey;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;

@Stateless
@JBossLog
public class ApiKeyService {

	
	public String generate(Long idPartner) {
		log.info("generate new api key");
		Session sess =(Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		Partner partner = sess.findObjectById(Partner.class, idPartner, null);
		log.info(partner.getName());
		String key = RandomStringUtils.randomAlphanumeric(30);
		String hashedKey = DigestUtils.sha256Hex(key);
		PartnerApiKey pApiKey = sess.executeNamedQuerySingle(PartnerApiKey.class, "apikey.findByPartner", new String[] {"partner"},new Partner[] {partner});
		Instant date = Instant.now(Clock.system(ZoneId.of("GMT")));
		if(pApiKey ==null) {
			pApiKey = new PartnerApiKey();
			pApiKey.setApiKey(hashedKey);
			pApiKey.setDateWriting(date);
			pApiKey.setPartner(partner);
			sess.saveObject(pApiKey);
		}else {
			pApiKey.setApiKey(hashedKey);
			pApiKey.setDateWriting(date);
			sess.updateObject(pApiKey);
		}
		Executors.newSingleThreadExecutor().execute(()->{
			
			String message = "Bonjour,\n. Merci dutliser cette cle pour acceder a nos apis. "+key;
			MailUtils.sendEmail(partner.getEmailContact(),"API KEY", message, false, null, null);
		});
		
		return key;
	}
	
	public Partner getPartnerByKey(String key) {
		Session sess =(Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
		
		String hashedKey = DigestUtils.sha256Hex(key.replace(" ", ""));
		
		PartnerApiKey pApiKey = sess.executeNamedQuerySingle(PartnerApiKey.class, "apikey.findByKey", new String[] {"key"},new String[] {hashedKey});
		
		return pApiKey==null?null:pApiKey.getPartner();
	}
}
