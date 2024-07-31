package sn.adiya.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Stateless;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.jboss.logging.Logger;

@Stateless
public class AdiyaCommonTools {

	private static final Logger LOG =Logger.getLogger(AdiyaCommonTools.class);
	private Properties props;
	
	@PostConstruct
	public void init() { 
		try {
		try(InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties")){
		 props = new Properties();
		 if(is!=null) {
			 
				props.load(is);
		 }
			 Integer port =(Integer)ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName("jboss.as:socket-binding-group=standard-sockets,socket-binding=http"), "port");
			 LOG.info(port);	  
			  Integer off =(Integer)ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName("jboss.as:socket-binding-group=standard-sockets"), "port-offset");
			 LOG.info(off);
			Integer finalPort = port+off;
			String base ="http://127.0.0.1:"+finalPort;
			props.setProperty("wildfly.port", finalPort.toString());
			props.setProperty(Constantes.WILDFLY_LOCAL, base);
		} 
		} catch (IOException |MBeanException|InstanceNotFoundException |AttributeNotFoundException |MalformedObjectNameException |ReflectionException e) {
			LOG.error("error",e);
		} 
		
	}
	
	@Lock(LockType.READ)
	public String getProperty(String propertyName) {
		return props.getProperty(propertyName);
	}
	
	
}
