package sn.adiya.common.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import sn.fig.common.config.entities.ParametresGeneraux;
import sn.fig.common.entities.GroupeUtilisateur;
import sn.fig.common.entities.Utilisateur;
import sn.fig.common.exception.ErrorResponse;
import sn.fig.common.utils.APGAuthResponse;
import sn.fig.common.utils.APGCommonRequest;
import sn.fig.common.utils.APIUtilVue;
import sn.fig.common.utils.BEConstantes;
import sn.fig.common.utils.BeanLocator;
import sn.fig.common.utils.ChannelResponse;
import sn.fig.common.utils.StringOperation;
import sn.fig.entities.Partner;
import sn.fig.entities.PartnerUtilisateur;
import sn.fig.entities.Wallet;
import sn.fig.mdb.EmailMessage;
import sn.fig.session.AdministrationSession;
import sn.fig.session.AdministrationSessionBean;
import sn.fig.session.MessageSenderService;
import sn.fig.session.MessageSenderServiceBean;
import sn.fig.session.OperationSession;
import sn.fig.session.OperationSessionBean;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.dto.APGCountry;
import sn.adiya.common.dto.Bank;
import sn.adiya.common.dto.Biller;
import sn.adiya.common.dto.Billers;
import sn.adiya.common.dto.CCurrency;
import sn.adiya.common.dto.Channel;
import sn.adiya.common.dto.Operator;
import sn.adiya.common.dto.POS;
import sn.adiya.common.dto.Periodicite;
import sn.adiya.common.dto.WOperator;

public class APGUtilVue {
	final static String TAG = APGUtilVue.class+"";
	public final static Logger LOG = Logger.getLogger(APGUtilVue.class);
	private volatile static APGUtilVue uniqueInstance;

	private APGUtilVue() {
	}

	public static APGUtilVue getInstance() {
		if (uniqueInstance == null) {
			synchronized (APGUtilVue.class) {
				if (uniqueInstance == null) {
					uniqueInstance = new APGUtilVue();
				}
			}
		}
		return uniqueInstance;
	}

	public String dayName(){
		Calendar cal = Calendar.getInstance();
		int day = cal.get(Calendar.DAY_OF_WEEK);
		if(day == 1)
			return "DIMANCHE";
		else if(day == 2)
			return "LUNDI";
		else if(day == 3)
			return "MARDI";
		else if(day == 4)
			return "MERCREDI";
		else if(day == 5)
			return "JEUDI";
		else if(day == 6)
			return "VENDREDI";
		else if(day == 7)
			return "SAMEDI";
		else
			return null;
	}

	public String inverse(String chaine) {
		StringBuilder result = new StringBuilder();
		for (int i = chaine.length() - 1; i >= 0; i--) {
			result.append(chaine.charAt(i));
		}
		return result.toString();
	}

	public String realFormat(String[] tAnnee) {
		return tAnnee[2]+"-"+tAnnee[1]+"-"+tAnnee[0];
	}

	public String md5ApacheCommonsCodec(String content) {
		return DigestUtils.md5Hex(content);
	}

	public void executeCommande(String commande){
		try {
			String[] cmd = {""+commande+""};
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public String SwitchRate(String from, String to, BigDecimal amount, Partner partner){
		AdministrationSession administrationSession = (AdministrationSession) BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());
		Float val = 0f;
		BigDecimal bd = APIUtilVue.getInstance().getFinalAmount(new BigDecimal(administrationSession.findCourDevise(from, to, amount, partner)));  
		val = bd.floatValue();
		/*	if(partner == null || !Boolean.TRUE.equals(partner.getIsB2B())) {
			BigDecimal bd = APIUtilVue.getInstance().getFinalAmount(new BigDecimal(administrationSession.findCourDevise(from, to, amount, partner)));  
			val = bd.floatValue();
		}else {
			BigDecimal bd = APIUtilVue.getInstance().getFinalAmount(new BigDecimal(administrationSession.findCourDeviseB2B(from, to, amount,partner)));  
			val = bd.floatValue();
		}*/

		if(from.equals(to))
			val = amount.floatValue();

		return val+"";
	}

	public List<Operator> SwitchOperator(String from){
		List<Operator> lOperators = new ArrayList<Operator>();
		if(from.equals("SN")){
			lOperators.add(new Operator("Orange","60801"));
			lOperators.add(new Operator("Free","60802"));
			lOperators.add(new Operator("Expresso","60803"));
			return lOperators;
		}else if(from.equals("CI")){
			lOperators.add(new Operator("Orange CI","600"));
			lOperators.add(new Operator("MTN","600"));
			lOperators.add(new Operator("Moov","600"));
			lOperators.add(new Operator("Koz","600"));
			return lOperators;
		}else{
			lOperators.add(new Operator("Empty Operator","000"));
			return lOperators;
		}
	}

	public String SwitchOperatorAirtime(String countryIsoCode){
		if(countryIsoCode.equals("SN")){
			LOG.info("======= >>>>>>>>>> START SwitchOperatorAirtime FROM SN >>>>>>>");
			JSONObject jo = new JSONObject(); 
			jo.put("code", ErrorResponse.REPONSE_SUCCESS.getCode());
			jo.put("message", ErrorResponse.REPONSE_SUCCESS.getMessage(""));

			JSONArray ja = new JSONArray(); 
			JSONObject jo0 = new JSONObject();
			jo0.put("countryIsoCode", "SN");
			jo0.put("operatorName", "ORANGE");
			jo0.put("mnc", "60801");
			jo0.put("minAmount", "0");
			jo0.put("maxAmount", "100000");
			ja.put(0, jo0); 

			JSONObject jo1= new JSONObject();
			jo1.put("countryIsoCode", "SN");
			jo1.put("operatorName", "FREE");
			jo1.put("mnc", "60802");
			jo1.put("minAmount", "0");
			jo1.put("maxAmount", "100000");
			ja.put(1, jo1); 

			JSONObject jo2= new JSONObject();
			jo2.put("countryIsoCode", "SN");
			jo2.put("operatorName", "EXPRESSO");
			jo2.put("mnc", "60803");
			jo2.put("minAmount", "0");
			jo2.put("maxAmount", "100000");
			ja.put(2, jo2); 

			JSONObject jo3= new JSONObject();
			jo3.put("countryIsoCode", "SN");
			jo3.put("operatorName", "PROMOBILE");
			jo3.put("mnc", "60804");
			jo3.put("minAmount", "0");
			jo3.put("maxAmount", "100000");
			ja.put(3, jo3);

			jo.putOpt("lOperators", ja);			
			return jo.toString();
		}else if(countryIsoCode.equals("NE")){
			LOG.info("======= >>>>>>>>>> START SwitchOperatorAirtime FROM NE >>>>>>>");
			JSONObject jo = new JSONObject(); 
			jo.put("code", ErrorResponse.REPONSE_SUCCESS.getCode());
			jo.put("message", ErrorResponse.REPONSE_SUCCESS.getMessage(""));

			JSONArray ja = new JSONArray(); 
			JSONObject jo0 = new JSONObject();
			jo0.put("countryIsoCode", "NE");
			jo0.put("operatorName", "Airtel Niger");
			jo0.put("mnc", "640");
			jo0.put("denominationType", "RANGE");
			jo0.put("minAmount", "118,14");
			jo0.put("maxAmount", "55786.95");
			jo0.put("trChannel", "AIR000");
			jo0.put("prefixe", "AN");
			jo0.put("partnerCode", BEConstantes.CODE_RELOADLY);
			ja.put(0, jo0); 

			JSONObject jo1= new JSONObject();
			jo1.put("countryIsoCode", "NE");
			jo1.put("operatorName", "Moov Niger");
			jo1.put("mnc", "339");
			jo1.put("denominationType", "FIXED");
			jo1.put("fixedAmounts", "670.39,1340.78,2676.03,6687.29,13374.59, 26754.72,66878.49");
			jo0.put("trChannel", "AIR000");
			jo1.put("prefixe", "MO");
			jo1.put("partnerCode", BEConstantes.CODE_RELOADLY);
			ja.put(1, jo1); 

			JSONObject jo2= new JSONObject();
			jo2.put("countryIsoCode", "NE");
			jo2.put("operatorName", "Orange Niger");
			jo2.put("mnc", "338");
			jo2.put("denominationType", "FIXED");
			jo2.put("fixedAmounts", "2543.06,6415.81,12826.09,25657.71");
			jo2.put("partnerCode", BEConstantes.CODE_RELOADLY);
			jo0.put("trChannel", "AIR000");
			jo2.put("prefixe", "ON");
			ja.put(2, jo2);   

			jo.putOpt("lOperators", ja);			
			return jo.toString();
		}else{
			JSONObject jo0 = new JSONObject();
			return jo0.toString();
		}
	}

	public List<Biller> SwitchBiller(String from){
		List<Biller> lBillers = new ArrayList<Biller>();
		if(from.equals("SN")){   
			lBillers.add(new Biller("Senelec","0001"));
			lBillers.add(new Biller("Woyofal","0002"));
			lBillers.add(new Biller("SDE","0003"));
			lBillers.add(new Biller("Rapido","0004"));
			return lBillers;
		}else if(from.equals("GN")){
			lBillers.add(new Biller("EDG",""));
			lBillers.add(new Biller("SEG",""));
			lBillers.add(new Biller("GUINEE GAMES",""));
			return lBillers;
		}else{
			lBillers.add(new Biller("Empty Biller","0000"));
			return lBillers;
		}
	}

	public List<Billers> SwitchBillers(String from){
		List<Billers> lBillers = new ArrayList<Billers>();
		if(from.equals("SN")){
			lBillers.add(new Billers("Energy","1","SENELEC","22110"));
			lBillers.add(new Billers("Energy","1","WOYOFAL","22111"));
			lBillers.add(new Billers("Water","2","SDE","22120"));
			lBillers.add(new Billers("TV","3","Canal +","22130"));
			lBillers.add(new Billers("TV","3","Excaf Telecom","22131"));
			return lBillers;
		}else if(from.equals("GN")){
			lBillers.add(new Billers("EDG","","",""));
			lBillers.add(new Billers("SEG","","",""));
			lBillers.add(new Billers("GUINEE GAMES","","",""));
			return lBillers;
		}else{
			lBillers.add(new Billers("Empty Biller","","","0000"));
			return lBillers;
		}
	}

	public List<Periodicite> listPeriodicite(){
		List<Periodicite> lPeriodicite = new ArrayList<>();
		lPeriodicite.add(new Periodicite("J","QUOTIDIEN"));
		lPeriodicite.add(new Periodicite("H","HEBDOMADAIRE"));
		lPeriodicite.add(new Periodicite("M","MENSUEL"));
		lPeriodicite.add(new Periodicite("T","TRIMESTRIEL"));
		lPeriodicite.add(new Periodicite("S","SEMESTRIEL"));
		lPeriodicite.add(new Periodicite("A","ANNUEL"));
		return lPeriodicite;
	}

	public List<WOperator> SwitchCountryWallet(String toCountryIsoCode){
		List<WOperator> lWOperators = new ArrayList<WOperator>();
		if(toCountryIsoCode.equals("SN")){
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("OD","Orange Money Distributeur"));
			lWOperators.add(new WOperator("FM","Free Money"));
			lWOperators.add(new WOperator("WV","Wave"));
			lWOperators.add(new WOperator("CP","Cinet Pay OM"));
			lWOperators.add(new WOperator("EM","E-Money"));
			lWOperators.add(new WOperator("YUP","YUP"));
			lWOperators.add(new WOperator("WZL","Wizall"));
			lWOperators.add(new WOperator("APG","APG"));
			lWOperators.add(new WOperator("OPT","Optima"));
			lWOperators.add(new WOperator("UCP","UCP"));
			lWOperators.add(new WOperator("CFP","CFP"));
			lWOperators.add(new WOperator("UNA","UNA"));
			lWOperators.add(new WOperator("PEC","PEC"));
			lWOperators.add(new WOperator("LMP","LIMO PAY"));
			lWOperators.add(new WOperator("PTF","LAPOSTE"));

			return lWOperators;
		}else if(toCountryIsoCode.equals("CI")){
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("MN","MTN")); // -> CINET PAY
			lWOperators.add(new WOperator("UCP","Wallet Unacoopec"));
			lWOperators.add(new WOperator("CFP","Wallet CFP"));
			lWOperators.add(new WOperator("MM","Moov Money"));

			return lWOperators;
		}else if(toCountryIsoCode.equals("BJ")){
			/*	lWOperators.add(new WOperator("MN","MTN"));
			lWOperators.add(new WOperator("FZ","Flooz"));	*/
			return lWOperators;
		}else if(toCountryIsoCode.equals("ZM")){
			lWOperators.add(new WOperator("MN","MTN"));
			lWOperators.add(new WOperator("AT","AIRTEL"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("TG")){
			lWOperators.add(new WOperator("FZ","Flooz"));
			lWOperators.add(new WOperator("TM","TMoney"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("ML")){  
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("MM","Moov Money"));// -> CINET PAY MC
			return lWOperators;
		}else if(toCountryIsoCode.equals("CM")){
			lWOperators.add(new WOperator("O1","OM CMR1"));
			lWOperators.add(new WOperator("O2","OM CMR2"));
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("MN","MTN")); 
			lWOperators.add(new WOperator("EU","Express Union"));
			lWOperators.add(new WOperator("UP","UP"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("GH")){ // -> MFS
			lWOperators.add(new WOperator("MN","MTN"));
			lWOperators.add(new WOperator("AT","AIRTEL"));
			lWOperators.add(new WOperator("TC","TIGO"));
			lWOperators.add(new WOperator("VF","VODAFONE"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("GN")){ // -> MFS
			lWOperators.add(new WOperator("MN","MTN"));
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("UB","UBA"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("GA")){
			lWOperators.add(new WOperator("EU","Express Union"));
			lWOperators.add(new WOperator("FQC","Wallet Frequence"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("CG")){
			lWOperators.add(new WOperator("EU","Express Union"));
			lWOperators.add(new WOperator("AT","AIRTEL"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("CF")){
			lWOperators.add(new WOperator("EU","Express Union"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("TD")){  
			lWOperators.add(new WOperator("EU","Express Union"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("MW")){
			lWOperators.add(new WOperator("XX","AIRTEL"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("RW")){ // -> MFS
			lWOperators.add(new WOperator("MN","MTN"));
			lWOperators.add(new WOperator("AT","AIRTEL"));
			lWOperators.add(new WOperator("TC","TIGO"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("KE")){   
			lWOperators.add(new WOperator("XX","Safaricom"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("GM")){   
			lWOperators.add(new WOperator("RFS","Reliance"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("UG")){ 
			lWOperators.add(new WOperator("AT","AIRTEL"));
			lWOperators.add(new WOperator("MN","MTN"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("BF")){ 
			lWOperators.add(new WOperator("MM","Moov Money"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("NG")){    
			lWOperators.add(new WOperator("XX","MTN NG"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("TZ")){ 
			lWOperators.add(new WOperator("XX","XX"));
			lWOperators.add(new WOperator("AT","AIRTEL"));
			lWOperators.add(new WOperator("TC","TIGO"));
			lWOperators.add(new WOperator("VC","VODACOM"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("CD")){ 
			lWOperators.add(new WOperator("XX","Wallet"));
			lWOperators.add(new WOperator("EP","E-MPATE"));
			lWOperators.add(new WOperator("MS","MPSEA"));
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("AT","Airtel Money"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("SL")){ 
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("AM","AfriMoney"));
			lWOperators.add(new WOperator("UB","UBA"));

			return lWOperators;
		}else if(toCountryIsoCode.equals("LR")){ 
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("AM","AfriMoney"));
			lWOperators.add(new WOperator("UB","UBA"));
			lWOperators.add(new WOperator("MN","MTN"));
			return lWOperators;
		}
		else{
			lWOperators.add(new WOperator("00","Empty Wallet"));
			return lWOperators;
		}
	}

	public List<WOperator> SwitchCountryWallet(String toCountryIsoCode, Partner partnerPayer){
		//	if(partnerPayer != null)codePartnerPayer += partnerPayer.getCode();
		List<WOperator> lWOperators = new ArrayList<WOperator>();
		if(toCountryIsoCode.equals("SN")){
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("OD","Orange Money Distributeur"));
			lWOperators.add(new WOperator("FM","Free Money"));
			lWOperators.add(new WOperator("WV","Wave"));
			lWOperators.add(new WOperator("CP","Cinet Pay OM"));
			lWOperators.add(new WOperator("EM","E-Money"));
			lWOperators.add(new WOperator("YUP","YUP"));
			lWOperators.add(new WOperator("WZL","Wizall"));
			lWOperators.add(new WOperator("APG","APG"));
			lWOperators.add(new WOperator("OPT","Optima"));  
			lWOperators.add(new WOperator("UCP","UCP"));
			lWOperators.add(new WOperator("CFP","CFP"));
			lWOperators.add(new WOperator("UNA","UNA"));
			lWOperators.add(new WOperator("PEC","PEC"));
			lWOperators.add(new WOperator("LMP","LIMO PAY"));
			lWOperators.add(new WOperator("PTF","LAPOSTE"));

			return lWOperators;
		}else if(toCountryIsoCode.equals("CI")){
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("MN","MTN")); // -> CINET PAY
			lWOperators.add(new WOperator("UCP","Wallet Unacoopec"));
			lWOperators.add(new WOperator("CFP","Wallet CFP"));

			return lWOperators;
		}else if(toCountryIsoCode.equals("ML")){    
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("MM","Moov Money"));// -> CINET PAY MC
			return lWOperators;
		}else if(toCountryIsoCode.equals("CM")){
			lWOperators.add(new WOperator("O1","OM CMR1"));
			lWOperators.add(new WOperator("O2","OM CMR2"));
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("MN","MTN")); 
			lWOperators.add(new WOperator("EU","Express Union"));
			lWOperators.add(new WOperator("UP","UP"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("GH")){ // -> MFS
			lWOperators.add(new WOperator("MN","MTN"));
			lWOperators.add(new WOperator("AT","AIRTEL"));
			lWOperators.add(new WOperator("TC","TIGO"));
			lWOperators.add(new WOperator("VF","VODAFONE"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("GN")){ // -> MFS
			lWOperators.add(new WOperator("MN","MTN"));
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("UB","UBA"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("GA")){
			lWOperators.add(new WOperator("EU","Express Union"));
			lWOperators.add(new WOperator("FQC","Wallet Frequence"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("CG")){
			lWOperators.add(new WOperator("EU","Express Union"));
			lWOperators.add(new WOperator("AT","AIRTEL"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("CF")){
			lWOperators.add(new WOperator("EU","Express Union"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("TD")){
			lWOperators.add(new WOperator("EU","Express Union"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("MW")){ 
			lWOperators.add(new WOperator("XX","AIRTEL"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("RW")){ // -> MFS
			lWOperators.add(new WOperator("MN","MTN"));
			lWOperators.add(new WOperator("AT","AIRTEL"));
			lWOperators.add(new WOperator("TC","TIGO"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("KE")){   
			lWOperators.add(new WOperator("XX","Safaricom"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("GM")){   
			lWOperators.add(new WOperator("RFS","Reliance"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("NG")){   
			lWOperators.add(new WOperator("XX","Paga"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("UG")){ 
			lWOperators.add(new WOperator("AT","AIRTEL"));
			lWOperators.add(new WOperator("MN","MTN"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("BF")){   
			lWOperators.add(new WOperator("MM","Moov Money"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("TZ")){ 
			lWOperators.add(new WOperator("XX","Wallet"));
			lWOperators.add(new WOperator("AT","AIRTEL"));
			lWOperators.add(new WOperator("TC","TIGO"));
			lWOperators.add(new WOperator("VC","VODACOM"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("BJ")){
			lWOperators.add(new WOperator("XX","Wallet"));
			/*	lWOperators.add(new WOperator("MN","MTN"));
			lWOperators.add(new WOperator("FZ","Flooz"));	*/
			return lWOperators;
		}else if(toCountryIsoCode.equals("ZM")){
			lWOperators.add(new WOperator("MN","MTN"));
			lWOperators.add(new WOperator("AT","AIRTEL"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("TG")){
			lWOperators.add(new WOperator("FZ","Flooz"));
			lWOperators.add(new WOperator("TM","TMoney"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("CD")){ 
			lWOperators.add(new WOperator("XX","Wallet"));
			lWOperators.add(new WOperator("EP","E-MPATE"));
			lWOperators.add(new WOperator("MS","MPSEA"));
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("AT","Airtel Money"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("SL")){ 
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("AM","AfriMoney"));
			lWOperators.add(new WOperator("UB","UBA"));
			return lWOperators;
		}else if(toCountryIsoCode.equals("LR")){ 
			lWOperators.add(new WOperator("OM","Orange Money"));
			lWOperators.add(new WOperator("AM","AfriMoney"));
			lWOperators.add(new WOperator("UB","UBA"));
			lWOperators.add(new WOperator("MN","MTN"));
			return lWOperators;
		}else{
			lWOperators.add(new WOperator("00","Empty Wallet"));
			return lWOperators;
		}
	}

	public List<CCurrency> SwitchCountryCurrency(String from){
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

		List<CCurrency> lCCurrencies = new ArrayList<CCurrency>();
		ParametresGeneraux parametresGeneraux = (ParametresGeneraux) session.findObjectById(ParametresGeneraux.class, null,from);
		if(parametresGeneraux != null){
			LOG.info("RESULT >>>>>>>>> "+parametresGeneraux.getLibelle());
			lCCurrencies.add(new CCurrency(parametresGeneraux.getLibelle()));
			return lCCurrencies;
		}else 
			return lCCurrencies;
	}

	public String SwitchFees(String operationType, String amount){
		if(operationType.equals("XOF")){
			return "0.0015";
		}else if(operationType.equals("XOF")){
			return "0.0018";
		}else if(operationType.equals("EUR")){
			return "656.43";
		}else if(operationType.equals("EUR")){
			return "1.16";
		}else if(operationType.equals("USD")){
			return "564.31";
		}else if(operationType.equals("USD")){
			return "0.86";
		}else return "";
	}

	public String SwitchCountry(String continent){
		List<APGCountry> lCountry = new ArrayList<APGCountry>();
		if(continent.equals("AF")){
			APGCountry cSenegal = new APGCountry();
			cSenegal.setCode("SN");
			cSenegal.setName("Senegal");
			lCountry.add(cSenegal);
			APGCountry cMali = new APGCountry();
			cMali.setCode("ML");
			cMali.setName("Mali");
			lCountry.add(cMali);
			APGCountry cGambie = new APGCountry();
			cGambie.setCode("GM");
			cGambie.setName("Gambie");
			lCountry.add(cGambie);
			APGCountry cGuinee = new APGCountry();
			cGuinee.setCode("GN");
			cGuinee.setName("Guinee");
			lCountry.add(cGuinee);
			APGCountry cCI = new APGCountry();
			cCI.setCode("CI");
			cCI.setName("Cote d ivoire");
			lCountry.add(cCI);
			return lCountry.toString();
		}else if(continent.equals("Europe")){
			APGCountry cFrance = new APGCountry();
			cFrance.setCode("FR");
			cFrance.setName("France");
			lCountry.add(cFrance);
			APGCountry cRoumanie = new APGCountry();
			cRoumanie.setCode("RO");
			cRoumanie.setName("Roumanie");
			lCountry.add(cRoumanie);
			return lCountry.toString();
		}else if(continent.equals("Amerique")){
			APGCountry cBresil = new APGCountry();
			cBresil.setCode("BR");
			cBresil.setName("Bresil");
			lCountry.add(cBresil);
			APGCountry cUSA = new APGCountry();
			cUSA.setCode("US");
			cUSA.setName("USA");
			lCountry.add(cUSA);
			return lCountry.toString();
		}else if(continent.equals("Asie")){
			APGCountry cChine = new APGCountry();
			cChine.setCode("CN");
			cChine.setName("Chine");
			lCountry.add(cChine);
			APGCountry cJapon = new APGCountry();
			cJapon.setCode("JP");
			cJapon.setName("Japon");
			lCountry.add(cJapon);
			return lCountry.toString();
		}else if(continent.equals("Oceanie")){
			APGCountry cAustralie = new APGCountry();
			cAustralie.setCode("AU");
			cAustralie.setName("Australie");
			lCountry.add(cAustralie);
			return lCountry.toString();
		}else return "";
	}

	public List<Channel> getListChannel(){
		List<Channel> lChannels = new ArrayList<Channel>();
		List<ChannelResponse> lChannelResponses = new ArrayList<ChannelResponse>(EnumSet.allOf(ChannelResponse.class));
		for(ChannelResponse cr:lChannelResponses) {
			Channel c = new Channel();
			c.setCode(cr.getCode());
			c.setName(cr.getMessage());
			lChannels.add(c);
		}
		return lChannels;
	}

	/*	public Boolean VerifyCredentiel(Utilisateur user){
		boolean isExpired=false;
		if(user != null)
			if(user.getNbPassword() != null && user.getNbPassword()>=Long.parseLong(Constantes.MAX_TENTATIVE_PASSWORD) || 
			user.getNbToken() != null &&  user.getNbToken()>=Long.parseLong(Constantes.MAX_TENTATIVE_TOKEN)){
				desableUser(user);
				isExpired=true;
			}
		return isExpired;
	}

	private void desableUser(Utilisateur user){
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

		user.setNbPassword(Long.parseLong(Constantes.MAX_TENTATIVE_PASSWORD)+1);
		user.setNbToken(Long.parseLong(Constantes.MAX_TENTATIVE_TOKEN)+1);

		session.updateObject(user);
	}*/

	public void CheckLogin(String login){
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		AdministrationSession administrationSession = (AdministrationSession) BeanLocator.lookUp(AdministrationSessionBean.class.getSimpleName());
		Utilisateur utilisateur = administrationSession.findUtilisateurByLogin(login);
		if(utilisateur != null){
			Long nbPassword = 1L;
			Long nbToken=null;
			if(utilisateur != null){
				if(utilisateur.getNbPassword() != null)
					nbPassword=utilisateur.getNbPassword()+1;
				if(utilisateur.getNbToken() != null)
					nbToken=utilisateur.getNbToken();
			}
			utilisateur.setNbPassword(nbPassword);
			utilisateur.setNbToken(nbToken);

			session.updateObject(utilisateur);
		}

	}

	public String generateToken(int length)
	{		
		return APIUtilVue.getInstance().getSeconde()+generate(length, Constantes.TOKEN_DATA);
	}

	private String generate(int length, String keyList){
		String code = new String();
		for(int i = 0; i < length; i++)
			code += keyList.charAt(Double.valueOf((Math.floor(Math.random() * keyList.length()))).intValue());

		return code;
	}

	public String getHeure(){
		Calendar calendrier = Calendar.getInstance();
		calendrier.setTime(new Date());

		return String.format("%d", calendrier.get(Calendar.HOUR));
	}

	public String getMinute(){
		Calendar calendrier = Calendar.getInstance();
		calendrier.setTime(new Date());

		return String.format("%02d", calendrier.get(Calendar.MINUTE)+1);
	}

	public String getSeconde(){
		Calendar calendrier = Calendar.getInstance();
		calendrier.setTime(new Date());

		return String.format("%02d", calendrier.get(Calendar.SECOND));
	}

	public String getDelay(int counter){
		String delay = null;
		if(counter == 1)
			delay = Constantes.DELAY_TOKEN_H;
		else if(counter == 2)
			delay = Constantes.DELAY_TOKEN_M;
		else if(counter == 3)
			delay = Constantes.DELAY_TOKEN_S;

		return delay;
	}

	public Date DateSetYear(Date uneDate,int uneAnnee) {
		if (uneDate != null) {
			Calendar calendrier = Calendar.getInstance();
			calendrier.setTime(uneDate);
			calendrier.set(Calendar.YEAR, uneAnnee);

			return calendrier.getTime();
		}
		else 
			return null;
	}

	public String FormatBalance(BigDecimal balance) {
		try {
			//NumberFormat formatter = new DecimalFormat("#0.000");     
			NumberFormat formatter = new DecimalFormat("#0");     
			return formatter.format(balance);
		} catch (Exception e) {
			return "";
		}
	}

	public List<Object> SwitchCPS(String toCountry, String servicesType){
		List<Object> lObjects = new ArrayList<Object>(); 
		if(toCountry.equals("SN")){   
			if(servicesType.equals("BANKS")){   
				lObjects.add(new Bank("BANK OF AFRICA", "AFRIBJB1BWB"));
				lObjects.add(new Bank("BANQUE ATLANTIQUE", "ATBJBJBJ"));
				lObjects.add(new Bank("BHS", "LHSESNDA"));
				lObjects.add(new Bank("BIMAO", "BIMUSNDA"));
				lObjects.add(new Bank("BANQUE ISLAMIQUE", "ISSNSNDA"));
				lObjects.add(new Bank("BRM", "BRMXSNDA"));
				lObjects.add(new Bank("BSIC-SENEGAL", "BSAHSNDA"));
			}else if(servicesType.equals("WALLETS")){
				lObjects.add(new Wallet("Orange Money", "XOF"));
				lObjects.add(new Wallet("Tigo Cash", "XOF"));
				lObjects.add(new Wallet("E-Money", "XOF"));
				lObjects.add(new Wallet("Wizall", "XOF"));
			}else if(servicesType.equals("POS")){
				lObjects.add(new POS("Wizall"));
				lObjects.add(new POS("ATPS"));
			}
			return lObjects;
		}else if(toCountry.equals("IC")){
			if(servicesType.equals("BANKS")){   
				lObjects.add(new Bank("BICICI", "BICICIAB"));
				lObjects.add(new Bank("NSIA BANQUE", "BIAOCIAB"));
				lObjects.add(new Bank("SIB", "SIVBCIAB"));
				lObjects.add(new Bank("SGBCI", "SGCICIAB"));
				lObjects.add(new Bank("CITIBANK", "CITICIAX"));
				lObjects.add(new Bank("BANK OF AFRICA", "AFRICIAB"));
			}else if(servicesType.equals("WALLETS")){
				lObjects.add(new Wallet("Orange Money", "XOF"));
				lObjects.add(new Wallet("MTN", "XOF"));
				lObjects.add(new Wallet("MOOV", "XOF"));
			}else if(servicesType.equals("POS")){
				lObjects.add(new POS("Ivoiry Central Pay"));
				lObjects.add(new POS("Grand Bassam"));
				lObjects.add(new POS("Petit Bassam"));
			}
			return lObjects;
		}
		else if(toCountry.equals("BG")){
			if(servicesType.equals("BANKS")){   
				lObjects.add(new Bank("BANCO DA AFRICA OCIDENTAL (BAO)", "BAOBGWGW"));
				lObjects.add(new Bank("BANCO DA UNIAO (BDU)", "BDUGGWGW"));
				lObjects.add(new Bank("ECOBANK", "ECOCGWGW"));
				lObjects.add(new Bank("ORABANK", "ORBKGWGW"));
				lObjects.add(new Bank("BANQUE ATLANTIQUE", "ATGWGWGW"));
			}else if(servicesType.equals("WALLETS")){
				lObjects.add(new Wallet("Orange Money", "XOF"));
				lObjects.add(new Wallet("MTN", "XOF"));
			}else if(servicesType.equals("POS")){
				lObjects.add(new POS("Ivoiry Central Pay"));
				lObjects.add(new POS("Grand Bassam"));
				lObjects.add(new POS("Petit Bassam"));
			}
			return lObjects;
		}else return lObjects;
	}

	public APGAuthResponse checkAuth(String auth) throws UnsupportedEncodingException {
		OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());
		if(auth == null || auth.equals("")){
			return new APGAuthResponse(ErrorResponse.AUTHENTICATION_ERRORS_1702.getCode(), ErrorResponse.AUTHENTICATION_ERRORS_1702.getMessage(Constantes.AUTH_NAME),null);
		}
		Partner partner = operationSession.findHmacKey(APIUtilVue.getInstance().apgCrypt(auth));
		if(partner == null){
			return new APGAuthResponse(ErrorResponse.AUTHENTICATION_ERRORS_1702.getCode(), ErrorResponse.AUTHENTICATION_ERRORS_1702.getMessage(Constantes.ACCOUNT_NAME),null);			
		}
		LOG.info("### >>>>>>> Partner : "+partner.getName());
		return new APGAuthResponse(ErrorResponse.REPONSE_SUCCESS.getCode(), ErrorResponse.REPONSE_SUCCESS.getMessage(Constantes.PARTNER_NAME),partner.getIdPartner());
	}

	public void addSuperAdminPartner(Partner partner, String loginContact, String email, String prenomContact, String nomContact, Boolean notifyUser, APGCommonRequest apgCommonRequest ) throws Exception {
		if(partner.getPType().equals(BEConstantes.PARTNER_AGREGATOR) || 
				partner.getPType().equals(BEConstantes.PARTNER_ACCEPTEUR) || 
				partner.getPType().equals(BEConstantes.PARTNER_SENDER) || 
				partner.getPType().equals(BEConstantes.PARTNER_SENDER_PAYER) || 
				partner.getPType().equals(BEConstantes.PARTNER_PROVIDER) || 
				partner.getPType().equals(BEConstantes.PARTNER_MONETIC) || 
				partner.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR) || 
				partner.getPType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)|| 
				partner.getPType().equals(BEConstantes.PARTNER_AGENCE)|| 
				partner.getPType().equals(BEConstantes.PARTNER_EMETTEUR)|| 
				partner.getPType().equals(BEConstantes.PARTNER_CAISSE) 
				) {
			MessageSenderService messageSenderService = (MessageSenderService) BeanLocator.lookUp(MessageSenderServiceBean.class.getSimpleName());
			OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());
			Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());

			APIUtilVue utilVue = APIUtilVue.getInstance();

			if(partner.getPType().equals(BEConstantes.PARTNER_PROVIDER)) {
				partner.setPrenomContact(prenomContact);
				partner.setNomContact(nomContact);
			}

			Utilisateur utilisateur = new Utilisateur();
			utilisateur.setDate(new Date());
			utilisateur.setEmail(email);
			utilisateur.setEntreprise("APG");
			utilisateur.setGenre("MR");
			utilisateur.setAdresse(partner.getAdresse());
			utilisateur.setValeur(apgCommonRequest.getValue());
			utilisateur.setDocumentNumber(apgCommonRequest.getDocumentNumber());
			utilisateur.setExpirationDocDate(utilVue.commonStringToDate(apgCommonRequest.getDateExpiration(), BEConstantes.FORMAT_DATE_DAY_MM_YYYY));

			String groupe=null;
			if(partner.getPType().equals(BEConstantes.PARTNER_AGREGATOR)) groupe=BEConstantes.GROUPE_ADMIN;
			else if(partner.getPType().equals(BEConstantes.PARTNER_SENDER_PAYER)) groupe=BEConstantes.GROUPE_SUPER_ADMIN_EP;
			else if(partner.getPType().equals(BEConstantes.PARTNER_ACCEPTEUR)) groupe=BEConstantes.GROUPE_PARTENAIRE_ECOMMERCE;
			else if(partner.getPType().equals(BEConstantes.PARTNER_EMETTEUR)) groupe=BEConstantes.GROUPE_CARD_PROGRAM_MANAGER;
			else if(partner.getPType().equals(BEConstantes.PARTNER_MONETIC)) {
				if(apgCommonRequest.getProfil() != null && apgCommonRequest.getProfil().equalsIgnoreCase("PMC"))
					groupe=BEConstantes.GROUPE_PROGAM_MANAGER_CONTROLER;
				else 
					groupe=BEConstantes.GROUPE_PROGAM_MANAGER_MASTER;
			}
			else if(partner.getPType().equals(BEConstantes.PARTNER_PROVIDER) && Boolean.TRUE.equals(partner.getSuperAdmin())) groupe=BEConstantes.GROUPE_SUPER_ADMIN_P;
			else if(partner.getPType().equals(BEConstantes.PARTNER_PROVIDER) && !Boolean.TRUE.equals(partner.getSuperAdmin())) addMakerCheckerPartner(partner, loginContact, email, prenomContact, nomContact);
			else if(partner.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR) && "AG".equals(partner.getFilsDistributeur())) groupe=BEConstantes.GROUPE_SUPER_ADMIN_DA;
			//	else if(partner.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR) && "AG".equals(partner.getFilsDistributeur()) && !Boolean.TRUE.equals(operationSession.partnerUser(partner).getSuperAdmin())) addMakerCheckerPartner(partner, loginContact, email, prenomContact, nomContact);
			else if(partner.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR) && "SD".equals(partner.getFilsDistributeur())) groupe=BEConstantes.GROUPE_SUPER_ADMIN_SD;
			//	else if(partner.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR) && "SD".equals(partner.getFilsDistributeur()) && !Boolean.TRUE.equals(operationSession.partnerUser(partner).getSuperAdmin())) addMakerCheckerPartner(partner, loginContact, email, prenomContact, nomContact);
			else if(partner.getPType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)) groupe=BEConstantes.GROUPE_SUPERVISEUR_SD;
			else if(partner.getPType().equals(BEConstantes.PARTNER_CAISSE)) groupe=BEConstantes.GROUPE_CAISSIER_P;
			//	else if(partner.getPType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR) && !Boolean.TRUE.equals(operationSession.partnerUser(partner).getSuperAdmin())) addMakerCheckerPartner(partner, loginContact, email, prenomContact, nomContact);
			else if(partner.getPType().equals(BEConstantes.PARTNER_SENDER)  && Boolean.FALSE.equals(partner.getIsB2B())) groupe=BEConstantes.GROUPE_PARTNER;
			else if(partner.getPType().equals(BEConstantes.PARTNER_SENDER) && Boolean.TRUE.equals(partner.getIsB2B())) groupe=BEConstantes.GROUPE_ADMIN_B;
			else if(partner.getPType().equals(BEConstantes.PARTNER_PAYER)) groupe=BEConstantes.GROUPE_ADMIN;
			else if(partner.getPType().equals(BEConstantes.PARTNER_AGENCE) ) groupe=BEConstantes.GROUPE_SUPERVISEUR_P;

			if(partner.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR) && "AG".equals(partner.getFilsDistributeur()) && (partner.getParent().getLogo().equals("logo-reliance"))) groupe = BEConstantes.GROUPE_CONTROLEUR_DA;
			else if(partner.getPType().equals(BEConstantes.PARTNER_DISTRIBUTEUR) && "SD".equals(partner.getFilsDistributeur()) && (partner.getParent().getLogo().equals("logo-reliance"))) groupe = BEConstantes.GROUPE_CONTROLEUR_DSD;

			if(groupe != null && (groupe.equals(BEConstantes.GROUPE_SUPER_ADMIN_DA) || groupe.equals(BEConstantes.GROUPE_SUPER_ADMIN_SD)) && (loginContact.contains(";"))) {
				addUsersForDistributeur(partner, loginContact, email, prenomContact, nomContact, groupe);
			}else if(groupe != null){
				Partner parent = null;
				if(partner.getPType().equals(BEConstantes.PARTNER_SOUS_DISTRIBUTEUR)) {
					LOG.info("Fils : "+partner.getName());				
					parent = partner.getParent().getParent();
					LOG.info("Grand pere : "+partner.getName());
					if(Boolean.TRUE.equals(apgCommonRequest.getIsMasterDealer()))
						utilisateur.setIsMasterDealer(true);
					else
						utilisateur.setIsMasterDealer(false);
				}else
					parent = partner.getParent();

				GroupeUtilisateur profil =  session.findObjectById(GroupeUtilisateur.class, null, groupe);
				utilisateur.setGroupeUtilisateur(profil);
				utilisateur.setIdGroupeUtilisateur(profil.getIdGroupeUtilisateur());
				utilisateur.setLibelleGroupeUtilisateur(profil.getLibelle());
				utilisateur.setIsInit(false);
				utilisateur.setIsActive(false);
				if(Boolean.TRUE.equals(notifyUser))
					utilisateur.setIsActive(true);
				LOG.info("### --- --- - Partner "+partner.getPType()+" GROUPE "+profil.getIdGroupeUtilisateur());
				utilisateur.setNom(nomContact);
				utilisateur.setMatricule(partner.getConsumerId());
				utilisateur.setPartner(operationSession.partnerUser(partner));
				utilisateur.setLogin(loginContact);
				String smsPassword = StringOperation.getPasswwd();
				utilisateur.setPassword(APIUtilVue.getInstance().apgSha(smsPassword));
				String phone = partner.getTelephoneContact();
				if(phone != null) {
					LOG.info("PARTNER >>>>>> "+partner.getIdPartner()+" >>>> "+partner.getName());
					LOG.info("INDICATIF >>>>>> "+partner.getCountryIndicatif());
					if(phone.startsWith(partner.getCountryIndicatif())) {
						phone = phone.substring(partner.getCountryIndicatif().length(), phone.length());
					}else if(phone.startsWith("+"+partner.getCountryIndicatif())) {
						phone = phone.substring(partner.getCountryIndicatif().length()+1, phone.length());
					}else if(phone.startsWith("00"+partner.getCountryIndicatif())) {
						phone = phone.substring(partner.getCountryIndicatif().length()+2, phone.length());
					}
				}
				utilisateur.setPhone(phone);
				utilisateur.setPrenom(prenomContact);
				utilisateur.setFirst(true);
				utilisateur.setType(BEConstantes.PIECE_1);
				utilisateur.setPartnerId(operationSession.partnerUser(partner).getIdPartner()+"");
				utilisateur.setPartnerName(operationSession.partnerUser(partner).getName());
				utilisateur.setPartnerCode(operationSession.partnerUser(partner).getCode());
				String ussdEntete = "", ussdContent = "";
				if(groupe.equals(BEConstantes.GROUPE_CAISSIER_P)) {
					if(Boolean.TRUE.equals(apgCommonRequest.getHasUssd())) {
						utilisateur.setUssdLogin(apgCommonRequest.getUssdLogin());
						utilisateur.setUssdPassword(utilVue.apgSha(apgCommonRequest.getUssdLogin()+apgCommonRequest.getUssdPassword()));
						ussdEntete = "<th bgcolor=\"black\">Numero USSD</th>\n" + 
								"<th bgcolor=\"black\">Pin</th>\n" ;

						ussdContent = "<th bgcolor=\"white\"> "+apgCommonRequest.getUssdLogin()+" </th>\n" + 
								"<th bgcolor=\"white\"> "+apgCommonRequest.getUssdPassword()+" </th>\n" ;
					}
				}

				utilisateur = (Utilisateur) session.saveObject(utilisateur);
				if(parent != null) { 
					session.saveObject(new PartnerUtilisateur(utilisateur, partner, true));
				}
				if(Boolean.TRUE.equals(notifyUser)) {
					String msg="Bonjour, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profil : "+utilisateur.getLibelleGroupeUtilisateur()+" Votre identifiant : "+utilisateur.getLogin()+" votre mot de passe : "+smsPassword;
					if(utilisateur.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) || utilisateur.getIdGroupeUtilisateur().equals(BEConstantes.GROUPE_PARTENAIRE_ECOMMERCE)) {
						msg = "Bonjour, vos parametres de connexion Optima Business sont : Identifiant / "+utilisateur.getLogin()+", Nouveau Mot de passe / "+smsPassword+". "
								+ "Vous pouvez telecharger l'application Optima Business en cliquant sur https://bit.ly/3tSxaZU";					}
					if(Boolean.TRUE.equals(apgCommonRequest.getHasUssd())) {
						msg += " . Votre Identifiant USSD "+apgCommonRequest.getUssdLogin()+", Votre Pin "+apgCommonRequest.getUssdPassword(); 
					}
					if(utilisateur.getPartner().getLogo() != null && utilisateur.getPartner().getLogo().equals("logo-reliance"))
						msg="Hello, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profile: "+utilisateur.getGroupeUtilisateur().getLibelle()+" Your username: "+utilisateur.getLogin()+" your password: "+smsPassword+"";
					utilVue.notifyUtilisateur(utilisateur, msg);
					String opt = utilisateur.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) ? "\"<tr style=\"color:white;\">\n" + 
							"<th colspan=\"2\" bgcolor=\"black\">Merci de vous connecter sur https://pms.optima.world</th>\n" + 
							"</tr>\n" : "";
					msg = BEConstantes.MESSAGE_HTML+"<table rules=\"all\" cellpadding=\"5px\" style=\"width:500px; border:solid 1px black; font-family:verdana; font-size:12px;\">\n" + 
							"   <tr>\n" + 
							"      <th bgcolor=\"white\" style=\"font-size:large\" colspan=\"2\" align=\"center\">Vos parametres de connexion</th>\n" + 
							"   </tr>\n" + 
							"   <tr style=\"color:white;\">\n" + 
							"      <th bgcolor=\"black\">Votre identifiant</th>\n" + 
							"      <th bgcolor=\"black\">Votre mot de passe</th>\n" + 
							opt +
							ussdEntete +
							"   </tr>\n" + 
							"   <tr style=\"color:black;\">\n" + 
							"      <th bgcolor=\"white\"> "+utilisateur.getLogin()+" </th>\n" + 
							"      <th bgcolor=\"white\"> "+smsPassword+" </th>\n" + 
							ussdContent +
							"   </tr>\n" + 
							"</table> ";
					String subject = utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profil : "+utilisateur.getLibelleGroupeUtilisateur()+" votre compte "+partner.getPType(),
							header = utilisateur.getEmail();
					String datas = "";

					if(utilisateur.getPartner().getLogo() != null && utilisateur.getPartner().getLogo().equals("logo-reliance")) {
						String subjectR = utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profile : "+utilisateur.getLibelleGroupeUtilisateur()+" Your account "+partner.getPType();
						String headerR = utilisateur.getEmail();
						String datasR = "";
						String msgR = BEConstantes.MESSAGE_HTML+"<table rules=\"all\" cellpadding=\"5px\" style=\"width:500px; border:solid 1px black; font-family:verdana; font-size:12px;\">\n" + 
								"   <tr>\n" + 
								"      <th bgcolor=\"white\" style=\"font-size:large\" colspan=\"2\" align=\"center\">Your access settings</th>\n" + 
								"   </tr>\n" + 
								"   <tr style=\"color:white;\">\n" + 
								"      <th bgcolor=\"black\">Your username</th>\n" + 
								"      <th bgcolor=\"black\">Your password</th>\n" + 
								"   </tr>\n" + 
								"   <tr style=\"color:black;\">\n" + 
								"      <th bgcolor=\"white\"> "+utilisateur.getLogin()+" </th>\n" + 
								"      <th bgcolor=\"white\"> "+smsPassword+" </th>\n" + 
								"   </tr>\n" + 
								"</table> ";
						messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null,headerR, subjectR, msgR, datasR));
					}
					else
						messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null,header, subject, msg, datas));
				}
			}

		}
	}

	private void addMakerCheckerPartner(Partner partner, String loginContact, String email, String prenomContact, String nomContact) throws Exception {
		OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		MessageSenderService messageSenderService = (MessageSenderService) BeanLocator.lookUp(MessageSenderServiceBean.class.getSimpleName());

		LOG.info(" ######## ----- ADD MAKER CHECKER ------ #######");
		LOG.info(" ######## ----- ADD MAKER CHECKER ------ #######");
		APIUtilVue utilVue = APIUtilVue.getInstance();
		utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("loginContact Maker"),loginContact.split(";")[0]);
		utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("email Maker"),email.split(";")[0]);
		utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("prenomContact Maker"),prenomContact.split(";")[0]);
		utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("nomContact Maker"),nomContact.split(";")[0]);

		utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("loginContact Checker"),loginContact.split(";")[1]);
		utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("email Checker"),email.split(";")[1]);
		utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("prenomContact Checker"),prenomContact.split(";")[1]);
		utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("nomContact Checker"),nomContact.split(";")[1]);

		if(partner.getPType().equals(BEConstantes.PARTNER_PROVIDER)) {
			partner.setPrenomContact(prenomContact);
			partner.setNomContact(nomContact);
			partner.setSuperAdmin(false);
		}
		Utilisateur utilisateur = new Utilisateur();
		utilisateur.setDate(new Date());
		utilisateur.setEntreprise("APG");
		utilisateur.setFlashCode("flash");
		utilisateur.setGenre("MR");
		utilisateur.setAdresse(partner.getAdresse());

		/*
		 * CREATION MAKER
		 */

		GroupeUtilisateur maker =  session.findObjectById(GroupeUtilisateur.class, null, BEConstantes.GROUPE_ADMIN_MAKER_P);
		GroupeUtilisateur checker =  session.findObjectById(GroupeUtilisateur.class, null, BEConstantes.GROUPE_ADMIN_CHECKER_P);
		utilisateur.setGroupeUtilisateur(maker);
		utilisateur.setIdGroupeUtilisateur(maker.getIdGroupeUtilisateur());
		utilisateur.setLibelleGroupeUtilisateur(maker.getLibelle());
		utilisateur.setIsInit(false);
		utilisateur.setIsActive(false);
		if(partner.getPType().equals(BEConstantes.PARTNER_PROVIDER))
			utilisateur.setIsActive(true);
		LOG.info("### --- --- - Partner "+partner.getPType()+" GROUPE "+maker.getIdGroupeUtilisateur());
		utilisateur.setNom(nomContact.split(";")[0]);
		utilisateur.setMatricule(partner.getConsumerId());
		utilisateur.setPartner(operationSession.partnerUser(partner));
		utilisateur.setLogin(loginContact.split(";")[0]);
		utilisateur.setEmail(email.split(";")[0]);
		String smsPassword = StringOperation.getPasswwd();
		utilisateur.setPassword(APIUtilVue.getInstance().apgSha(smsPassword));
		String phone = partner.getTelephoneContact();
		if(phone != null) {
			if(partner.getTelephoneContact().contains(";") && partner.getTelephoneContact().split(";")[0] != null)
				phone = partner.getTelephoneContact().split(";")[0];
			if(phone.startsWith(partner.getCountryIndicatif())) {
				phone = phone.substring(partner.getCountryIndicatif().length(), phone.length());
			}else if(phone.startsWith("+"+partner.getCountryIndicatif())) {
				phone = phone.substring(partner.getCountryIndicatif().length()+1, phone.length());
			}else if(phone.startsWith("00"+partner.getCountryIndicatif())) {
				phone = phone.substring(partner.getCountryIndicatif().length()+2, phone.length());
			}
		}
		utilisateur.setPhone(phone);
		utilisateur.setPrenom(prenomContact.split(";")[0]);
		utilisateur.setFirst(true);
		utilisateur.setType(BEConstantes.PIECE_1);
		utilisateur.setPartnerId(operationSession.partnerUser(partner).getIdPartner()+"");
		utilisateur.setPartnerName(operationSession.partnerUser(partner).getName());
		utilisateur.setPartnerCode(operationSession.partnerUser(partner).getCode());

		utilisateur = (Utilisateur) session.saveObject(utilisateur);

		if(partner.getPType().equals(BEConstantes.PARTNER_PROVIDER)) {
			String msg="Bonjour, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profil : "+utilisateur.getLibelleGroupeUtilisateur()+" Votre identifiant : "+utilisateur.getLogin()+" votre mot de passe : "+smsPassword;
			if(utilisateur.getPartner().getLogo() != null && utilisateur.getPartner().getLogo().equals("logo-reliance"))
				msg="Hello, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profile: "+utilisateur.getGroupeUtilisateur().getLibelle()+" Your username: "+utilisateur.getLogin()+" your password: "+smsPassword+"";
			APIUtilVue.getInstance().notifyUtilisateur(utilisateur, msg);
			String opt = utilisateur.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) ? "\"<tr style=\"color:white;\">\n" + 
					"<th colspan=\"2\" bgcolor=\"black\">Merci de vous connecter sur https://pms.optima.world</th>\n" + 
					"</tr>\n" : "";
			msg = BEConstantes.MESSAGE_HTML+"<table rules=\"all\" cellpadding=\"5px\" style=\"width:500px; border:solid 1px black; font-family:verdana; font-size:12px;\">\n" + 
					"   <tr>\n" + 
					"      <th bgcolor=\"white\" style=\"font-size:large\" colspan=\"2\" align=\"center\">Vos parametres de connexion</th>\n" + 
					"   </tr>\n" + 
					"   <tr style=\"color:white;\">\n" + 
					"      <th bgcolor=\"black\">Votre identifiant</th>\n" + 
					"      <th bgcolor=\"black\">Votre mot de passe</th>\n" + 
					opt + 
					"   </tr>\n" + 
					"   <tr style=\"color:black;\">\n" + 
					"      <th bgcolor=\"white\"> "+utilisateur.getLogin()+" </th>\n" + 
					"      <th bgcolor=\"white\"> "+smsPassword+" </th>\n" + 
					"   </tr>\n" + 
					"</table> ";
			String subject = utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profil : "+utilisateur.getLibelleGroupeUtilisateur()+" votre compte "+utilisateur.getPartner().getPType(),
					header = utilisateur.getEmail();
			String datas = "";
			messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null,header, subject, msg, datas));	
		}


		/*
		 * CREATION CHECKER
		 */

		utilisateur.setGroupeUtilisateur(checker);
		utilisateur.setIdGroupeUtilisateur(checker.getIdGroupeUtilisateur());
		utilisateur.setLibelleGroupeUtilisateur(checker.getLibelle());
		utilisateur.setIsInit(false);
		utilisateur.setIsActive(false);
		if(partner.getPType().equals(BEConstantes.PARTNER_PROVIDER))
			utilisateur.setIsActive(true);
		LOG.info("### --- --- - Partner "+partner.getPType()+" GROUPE "+checker.getIdGroupeUtilisateur());
		utilisateur.setNom(nomContact.split(";")[1]);
		utilisateur.setMatricule(partner.getConsumerId());
		utilisateur.setPartner(operationSession.partnerUser(partner));
		utilisateur.setLogin(loginContact.split(";")[1]);
		utilisateur.setEmail(email.split(";")[1]);
		smsPassword = StringOperation.getPasswwd();
		utilisateur.setPassword(APIUtilVue.getInstance().apgSha(smsPassword));
		phone = partner.getTelephoneContact();
		if(phone != null) {
			if(partner.getTelephoneContact().contains(";") && partner.getTelephoneContact().split(";")[1] != null)
				phone = partner.getTelephoneContact().split(";")[1];
			if(phone.startsWith(partner.getCountryIndicatif())) {
				phone = phone.substring(partner.getCountryIndicatif().length(), phone.length());
			}else if(phone.startsWith("+"+partner.getCountryIndicatif())) {
				phone = phone.substring(partner.getCountryIndicatif().length()+1, phone.length());
			}else if(phone.startsWith("00"+partner.getCountryIndicatif())) {
				phone = phone.substring(partner.getCountryIndicatif().length()+2, phone.length());
			}
		}
		utilisateur.setPhone(phone);
		utilisateur.setPrenom(prenomContact.split(";")[1]);
		utilisateur.setFirst(true);
		utilisateur.setType(BEConstantes.PIECE_1);
		utilisateur.setPartnerId(operationSession.partnerUser(partner).getIdPartner()+"");
		utilisateur.setPartnerName(operationSession.partnerUser(partner).getName());
		utilisateur.setPartnerCode(operationSession.partnerUser(partner).getCode());

		utilisateur = (Utilisateur) session.saveObject(utilisateur);

		if(partner.getPType().equals(BEConstantes.PARTNER_PROVIDER)) {
			String msg="Bonjour, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profil : "+utilisateur.getLibelleGroupeUtilisateur()+" Votre identifiant : "+utilisateur.getLogin()+" votre mot de passe : "+smsPassword;
			if(utilisateur.getPartner().getLogo() != null && utilisateur.getPartner().getLogo().equals("logo-reliance"))
				msg="Hello, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profile: "+utilisateur.getGroupeUtilisateur().getLibelle()+" Your username: "+utilisateur.getLogin()+" your password: "+smsPassword+"";
			APIUtilVue.getInstance().notifyUtilisateur(utilisateur, msg);
			String opt = utilisateur.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) ? "\"<tr style=\"color:white;\">\n" + 
					"<th colspan=\"2\" bgcolor=\"black\">Merci de vous connecter sur https://pms.optima.world</th>\n" + 
					"</tr>\n" : "";
			msg = BEConstantes.MESSAGE_HTML+"<table rules=\"all\" cellpadding=\"5px\" style=\"width:500px; border:solid 1px black; font-family:verdana; font-size:12px;\">\n" + 
					"   <tr>\n" + 
					"      <th bgcolor=\"white\" style=\"font-size:large\" colspan=\"2\" align=\"center\">Vos parametres de connexion</th>\n" + 
					"   </tr>\n" + 
					"   <tr style=\"color:white;\">\n" + 
					"      <th bgcolor=\"black\">Votre identifiant</th>\n" + 
					"      <th bgcolor=\"black\">Votre mot de passe</th>\n" + 
					opt +
					"   </tr>\n" + 
					"   <tr style=\"color:black;\">\n" + 
					"      <th bgcolor=\"white\"> "+utilisateur.getLogin()+" </th>\n" + 
					"      <th bgcolor=\"white\"> "+smsPassword+" </th>\n" + 
					"   </tr>\n" + 
					"</table> ";
			String subject = utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profil : "+utilisateur.getLibelleGroupeUtilisateur()+" votre compte "+utilisateur.getPartner().getPType(),
					header = utilisateur.getEmail();
			String datas = "";
			messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null,header, subject, msg, datas));	
		}
	} 

	private void addUsersForDistributeur(Partner partner, String loginContact, String email, String prenomContact, String nomContact, String groupe) throws Exception {
		OperationSession operationSession = (OperationSession) BeanLocator.lookUp(OperationSessionBean.class.getSimpleName());
		Session session = (Session) BeanLocator.lookUp(SessionBean.class.getSimpleName());
		MessageSenderService messageSenderService = (MessageSenderService) BeanLocator.lookUp(MessageSenderServiceBean.class.getSimpleName());

		LOG.info(" ######## ----- ADD SUPER_ADMIN ------ #######");
		LOG.info(" ######## ----- ADD SUPER_ADMIN ------ #######");

		GroupeUtilisateur profilSuperAdmin = (GroupeUtilisateur) session.findObjectById(GroupeUtilisateur.class, null, groupe);
		if(groupe.equals(BEConstantes.GROUPE_SUPER_ADMIN_DA) || groupe.equals(BEConstantes.GROUPE_SUPER_ADMIN_SD)) {
			GroupeUtilisateur profilControleur = null;
			if(groupe.equals(BEConstantes.GROUPE_SUPER_ADMIN_DA)) 
				profilControleur = (GroupeUtilisateur) session.findObjectById(GroupeUtilisateur.class, null, BEConstantes.GROUPE_CONTROLEUR_DA); 
			else if(groupe.equals(BEConstantes.GROUPE_SUPER_ADMIN_SD)) 
				profilControleur = (GroupeUtilisateur) session.findObjectById(GroupeUtilisateur.class, null, BEConstantes.GROUPE_CONTROLEUR_DSD); 

			APIUtilVue utilVue = APIUtilVue.getInstance();
			if(loginContact.split(";").length < 2) 
				utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("fichier incomplet verifiez superAdmin et controleur"),null);

			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("loginContact superAdmin"),loginContact.split(";")[0]);
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("email superAdmin"),email.split(";")[0]);
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("prenomContact superAdmin"),prenomContact.split(";")[0]);
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("nomContact superAdmin"),nomContact.split(";")[0]);

			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("loginContact controleur"),loginContact.split(";")[1]);
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("email controleur"),email.split(";")[1]);
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("prenomContact controleur"),prenomContact.split(";")[1]);
			utilVue.CommonLabel(null,TAG,ErrorResponse.SYNTAXE_ERRORS_1802.getCode(),ErrorResponse.SYNTAXE_ERRORS_1802.getMessage("nomContact controleur"),nomContact.split(";")[1]);
			Utilisateur utilisateur = new Utilisateur();
			utilisateur.setDate(new Date());
			utilisateur.setEntreprise("APG");
			utilisateur.setFlashCode("flash");
			utilisateur.setGenre("MR");
			utilisateur.setAdresse(partner.getAdresse());

			/*
			 * CREATION MAKER
			 */

			utilisateur.setGroupeUtilisateur(profilSuperAdmin);
			utilisateur.setIdGroupeUtilisateur(profilSuperAdmin.getIdGroupeUtilisateur());
			utilisateur.setLibelleGroupeUtilisateur(profilSuperAdmin.getLibelle());
			utilisateur.setIsInit(false);
			utilisateur.setIsActive(true);
			LOG.info("### --- --- - Partner "+partner.getPType()+" GROUPE "+profilSuperAdmin.getIdGroupeUtilisateur());
			utilisateur.setNom(nomContact.split(";")[0]);
			utilisateur.setMatricule(partner.getConsumerId());
			utilisateur.setPartner(operationSession.partnerUser(partner));
			utilisateur.setLogin(loginContact.split(";")[0]);
			utilisateur.setEmail(email.split(";")[0]);
			String smsPassword = StringOperation.getPasswwd();
			utilisateur.setPassword(APIUtilVue.getInstance().apgSha(smsPassword));
			String phone = partner.getTelephoneContact();
			if(phone != null) {
				if(partner.getTelephoneContact().contains(";") && partner.getTelephoneContact().split(";")[0] != null)
					phone = partner.getTelephoneContact().split(";")[0];
				if(phone.startsWith(partner.getCountryIndicatif())) {
					phone = phone.substring(partner.getCountryIndicatif().length(), phone.length());
				}else if(phone.startsWith("+"+partner.getCountryIndicatif())) {
					phone = phone.substring(partner.getCountryIndicatif().length()+1, phone.length());
				}else if(phone.startsWith("00"+partner.getCountryIndicatif())) {
					phone = phone.substring(partner.getCountryIndicatif().length()+2, phone.length());
				}
			}
			partner.setPrenomContact(prenomContact.split(";")[0]);
			partner.setNomContact(nomContact.split(";")[0]);
			partner.setTelephoneContact(phone.split(";")[0]);
			partner.setEmailContact(email.split(";")[0]);
			session.updateObject(partner);

			utilisateur.setPhone(phone);
			utilisateur.setPrenom(prenomContact.split(";")[0]);
			utilisateur.setFirst(true);
			utilisateur.setType(BEConstantes.PIECE_1);
			utilisateur.setPartnerId(operationSession.partnerUser(partner).getIdPartner()+"");
			utilisateur.setPartnerName(operationSession.partnerUser(partner).getName());
			utilisateur.setPartnerCode(operationSession.partnerUser(partner).getCode());

			utilisateur = (Utilisateur) session.saveObject(utilisateur);
			PartnerUtilisateur pu = new PartnerUtilisateur(utilisateur, partner, true);
			session.saveObject(pu);

			String msg="Bonjour, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profil : "+utilisateur.getLibelleGroupeUtilisateur()+" Votre identifiant : "+utilisateur.getLogin()+" votre mot de passe : "+smsPassword;
			if(utilisateur.getPartner().getLogo() != null && utilisateur.getPartner().getLogo().equals("logo-reliance"))
				msg="Hello, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profile: "+utilisateur.getGroupeUtilisateur().getLibelle()+" Your username: "+utilisateur.getLogin()+" your password: "+smsPassword+"";
			APIUtilVue.getInstance().notifyUtilisateur(utilisateur, msg);
			String opt = utilisateur.getPartner().getCode().equals(BEConstantes.CODE_OPTIMA_PROVIDER) ? "\"<tr style=\"color:white;\">\n" + 
					"<th colspan=\"2\" bgcolor=\"black\">Merci de vous connecter sur https://pms.optima.world</th>\n" + 
					"</tr>\n" : "";
			msg = BEConstantes.MESSAGE_HTML+"<table rules=\"all\" cellpadding=\"5px\" style=\"width:500px; border:solid 1px black; font-family:verdana; font-size:12px;\">\n" + 
					"   <tr>\n" + 
					"      <th bgcolor=\"white\" style=\"font-size:large\" colspan=\"2\" align=\"center\">Vos parametres de connexion</th>\n" + 
					"   </tr>\n" + 
					"   <tr style=\"color:white;\">\n" + 
					"      <th bgcolor=\"black\">Votre identifiant</th>\n" + 
					"      <th bgcolor=\"black\">Votre mot de passe</th>\n" + 
					opt +
					"   </tr>\n" + 
					"   <tr style=\"color:black;\">\n" + 
					"      <th bgcolor=\"white\"> "+utilisateur.getLogin()+" </th>\n" + 
					"      <th bgcolor=\"white\"> "+smsPassword+" </th>\n" + 
					"   </tr>\n" + 
					"</table> ";
			String subject = utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profil : "+utilisateur.getLibelleGroupeUtilisateur()+" votre compte "+utilisateur.getPartner().getPType(),
					header = utilisateur.getEmail();
			String datas = "";
			messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null,header, subject, msg, datas));	

			/*
			 * CREATION CHECKER
			 */
			utilisateur = new Utilisateur();
			utilisateur.setGroupeUtilisateur(profilControleur);
			utilisateur.setIdGroupeUtilisateur(profilControleur.getIdGroupeUtilisateur());
			utilisateur.setLibelleGroupeUtilisateur(profilControleur.getLibelle());
			utilisateur.setIsInit(false);
			utilisateur.setIsActive(true);
			LOG.info("### --- --- - Partner "+partner.getPType()+" GROUPE "+profilControleur.getIdGroupeUtilisateur());
			utilisateur.setNom(nomContact.split(";")[1]);
			utilisateur.setMatricule(partner.getConsumerId());
			utilisateur.setPartner(operationSession.partnerUser(partner));
			utilisateur.setLogin(loginContact.split(";")[1]);
			utilisateur.setEmail(email.split(";")[1]);
			smsPassword = StringOperation.getPasswwd();
			utilisateur.setPassword(APIUtilVue.getInstance().apgSha(smsPassword));
			phone = partner.getTelephoneContact();
			if(phone != null) {
				if(partner.getTelephoneContact().contains(";") && partner.getTelephoneContact().split(";")[1] != null)
					phone = partner.getTelephoneContact().split(";")[1];
				if(phone.startsWith(partner.getCountryIndicatif())) {
					phone = phone.substring(partner.getCountryIndicatif().length(), phone.length());
				}else if(phone.startsWith("+"+partner.getCountryIndicatif())) {
					phone = phone.substring(partner.getCountryIndicatif().length()+1, phone.length());
				}else if(phone.startsWith("00"+partner.getCountryIndicatif())) {
					phone = phone.substring(partner.getCountryIndicatif().length()+2, phone.length());
				}
			}
			utilisateur.setPhone(phone);
			utilisateur.setPrenom(prenomContact.split(";")[1]);
			utilisateur.setFirst(true);
			utilisateur.setType(BEConstantes.PIECE_1);
			utilisateur.setPartnerId(operationSession.partnerUser(partner).getIdPartner()+"");
			utilisateur.setPartnerName(operationSession.partnerUser(partner).getName());
			utilisateur.setPartnerCode(operationSession.partnerUser(partner).getCode());

			utilisateur = (Utilisateur) session.saveObject(utilisateur);
			pu = new PartnerUtilisateur(utilisateur, partner, true);
			session.saveObject(pu);

			msg="Bonjour, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profil : "+utilisateur.getLibelleGroupeUtilisateur()+" Votre identifiant : "+utilisateur.getLogin()+" votre mot de passe : "+smsPassword;
			if(utilisateur.getPartner().getLogo() != null && utilisateur.getPartner().getLogo().equals("logo-reliance"))
				msg="Hello, "+utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profile: "+utilisateur.getGroupeUtilisateur().getLibelle()+" Your username: "+utilisateur.getLogin()+" your password: "+smsPassword+"";
			APIUtilVue.getInstance().notifyUtilisateur(utilisateur, msg);
			msg = BEConstantes.MESSAGE_HTML+"<table rules=\"all\" cellpadding=\"5px\" style=\"width:500px; border:solid 1px black; font-family:verdana; font-size:12px;\">\n" + 
					"   <tr>\n" + 
					"      <th bgcolor=\"white\" style=\"font-size:large\" colspan=\"2\" align=\"center\">Vos parametres de connexion</th>\n" + 
					"   </tr>\n" + 
					"   <tr style=\"color:white;\">\n" + 
					"      <th bgcolor=\"black\">Votre identifiant</th>\n" + 
					"      <th bgcolor=\"black\">Votre mot de passe</th>\n" + 
					opt +
					"   </tr>\n" + 
					"   <tr style=\"color:black;\">\n" + 
					"      <th bgcolor=\"white\"> "+utilisateur.getLogin()+" </th>\n" + 
					"      <th bgcolor=\"white\"> "+smsPassword+" </th>\n" + 
					"   </tr>\n" + 
					"</table> ";
			subject = utilisateur.getPrenom()+" "+utilisateur.getNom()+" Profil : "+utilisateur.getLibelleGroupeUtilisateur()+" votre compte "+utilisateur.getPartner().getPType();
			header = utilisateur.getEmail();
			messageSenderService.enqueue("queue/EmailQueue", new EmailMessage(null,header, subject, msg, datas));	
		} 
	}
}
