package sn.adiya.payment.keys;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import lombok.extern.jbosslog.JBossLog;
import sn.fig.common.utils.AbstractResponse;

@Path("/config")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@JBossLog
public class KeyManagementCtl {

	@Inject
	private KeyHandlerService keyHandler;
	
	@Path("/terminalKey")
	@GET
	public AbstractResponse getTerminalKey(@QueryParam("terminalSN") String terminalSn ) 
	{
		log.info("service : getIpek");	
		return keyHandler.getTerminalKey(terminalSn);
	}
}
