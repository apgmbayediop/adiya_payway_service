/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sn.adiya.common.hsm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.ejb.Stateless;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

import sn.fig.common.config.entities.ParametresGeneraux;
import sn.fig.common.utils.BeanLocator;
import sn.fig.entities.Card;
import sn.fig.session.Session;
import sn.fig.session.SessionBean;
import sn.adiya.common.utils.Constantes;

/**
 *
 * @author Cheikhouna DIOP
 */
@Stateless
public class HSMHandler {

    private static final Logger LOG = Logger.getLogger(HSMHandler.class);

    private Socket socket;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    protected String mk;
    protected int port =1500;
    protected String address ="172.16.2.253";
    

    public HSMHandler() {
    	super();
    }
    public static String generateKsn(String terminalSn) {
		String end = "0E00000";
		String start = "FFF987654321" + terminalSn;
		start = start.replaceAll("[A-Z]", "A");
		start = start.substring(start.length() - 9, start.length());
		String ksn = "FFFF" + start + end;
		LOG.info("KSN = " + ksn);
		return ksn;
	}

    public byte[] sendCommand(byte[] command) {
        try {
        	closeHsm();
        	connect();
            if (isConnected()) {
                
                dataOut.write(command);
                dataOut.flush();
                byte [] received = new byte[300];
                int len = dataIn.read(received);
                if(len>0){
                    
                byte [] data = new byte[len];
                System.arraycopy(received, 0, data, 0, len);
                return data;
                }
                else {
                    return new byte[]{0x39,0x30,0x39};
                }
            } else {
            	LOG.info("not connected");
                return new byte[]{0x39,0x30,0x39};
            }
        } catch (IOException e) {
        	LOG.log(Level.FATAL, "errorCmd",e);
            return new byte[]{0x39,0x30,0x39};
        }
        finally {
			closeHsm();
		}

    }

    private void closeHsm() {
    	try {
    		LOG.info("close hsm ");
    	
			if(socket==null) 
				 {
					LOG.info("already close");
				}else{
				dataIn.close();
				dataOut.close();
				socket.close();
			}
			
		} catch (Exception e) {
			LOG.error("errorCloseHsm",e);
		}
    	finally {
			socket = null;
		}
    }
    public void connect() {
        try {
    
                
        	if (!isConnected()) {
        		loadData();
        		LOG.info("address "+address);
                socket = new Socket(address, port);
                socket.setSoTimeout(60000);
                LOG.info(socket.isConnected());
                dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                }

            
        } catch (IOException e) {
        	LOG.log(Level.FATAL, "errorConnect",e);
        }
    }

    @PostConstruct
    public void loadData() {
    
    	if(mk ==null || address == null) {
    		//IP HSM;port;MASTER KEY,CLEARBDK;BDKLMK;ZPK
        	Session session = (Session)BeanLocator.lookUp(SessionBean.class.getSimpleName());
        	ParametresGeneraux param = session.findObjectById(ParametresGeneraux.class, null,"PARAM_HSM");
            if(param==null)
            	 {
                	LOG.info("PARAM_HSM is null");
                }else{
            String []data = param.getLibelle().split(";");
            address = data[0];
            port = Integer.parseInt(data[1]);
            mk = data[2];
            
            }
            
            LOG.info(address);
            LOG.info(port);
    	}
    }
    public boolean isConnected() {

        return socket != null && socket.isConnected();
    }

    public byte[] calculateArpc(String pan, String atc, String unpredic, String arqc, String arc) {
        //ClientHSM
        //0123KQ + 02 + 00 + CLE MK-AC(LMK) + PAN + ATC(TAG 9F36) + UN (TAG 9F37) + ARQC (9F26) + ARC ( TAG 8A)
        try {
            byte[] header = ("0123KQ20"+mk).getBytes(StandardCharsets.US_ASCII);
            byte[] panb = Hex.decodeHex(pan.toCharArray());
            byte[] atcb = Hex.decodeHex(atc.toCharArray());
            byte[] unpredicb = Hex.decodeHex(unpredic.toCharArray());
            byte[] arqcb = Hex.decodeHex(arqc.toCharArray());
            byte[] arcb = Hex.decodeHex(arc.toCharArray());
            int len = header.length + panb.length + atcb.length + unpredicb.length + arqcb.length + arcb.length;
            byte[] com = new byte[len + 2];
            System.arraycopy(header, 0, com, 2, header.length);
            System.arraycopy(panb, 0, com, 2 + header.length, panb.length);
            System.arraycopy(atcb, 0, com, 2 + header.length + panb.length, atcb.length);
            System.arraycopy(unpredicb, 0, com, 2 + header.length + panb.length + atcb.length, unpredicb.length);
            System.arraycopy(arqcb, 0, com, 2 + header.length + panb.length + atcb.length + unpredicb.length, arqcb.length);
            System.arraycopy(arcb, 0, com, 2 + header.length + panb.length + atcb.length + unpredicb.length + arqcb.length, arcb.length);
            com[0] = 0x00;
            com[1] = (byte) len;
            byte [] arpcResp = sendCommand(com);
            int minLen=8;
            if (arpcResp.length>minLen) {
                byte[] arpc = new byte[8];
                System.arraycopy(arpcResp, 10, arpc, 0, 8);
                return arpc;
            } else {
                return new byte[]{0x39,0x39,0x39};
            }
        }  catch (Exception e) {
        	 LOG.log(Level.FATAL, "errorArpc", e);
             return new byte[]{0x39,0x39,0x39};
		}
    }
    
    public byte[] generateBDK() {

        try {
        	connect();
            byte[] header = "0123A00009U".getBytes(StandardCharsets.US_ASCII);
            int len = header.length;
            byte[] com = new byte[len + 2];
            System.arraycopy(header, 0, com, 2, header.length);
            com[0] = 0x00;
            com[1] = (byte) len;
            byte [] resb = sendCommand(com);
            String resp = new String(resb,StandardCharsets.US_ASCII);
            
            if (resp.contains("0123A100U")) {
            	int index = resp.indexOf("0123A100U");
                String bdk = resp.substring(index+9);
            	return hexStringToByteArray(bdk);
            } else {
                return new byte[]{0x39,0x39,0x39};
            }
        } catch (Exception e) {
            LOG.log(Level.FATAL, "errorBdk", e);
            return new byte[]{0x39,0x39,0x39};
        }
    }

    
    public  byte[] generateIPEK(byte[] ksnb,String baseKey){
    	Security.addProvider(new BouncyCastleProvider());
        byte[] ksn = new byte[10];
        for(int i= 0;i<10;i++) {
        	ksn[i] = (byte)0xFF;
        }
        int destPos = ksn.length -ksnb.length;
        byte[] bdk =hexStringToByteArray(baseKey);
        System.arraycopy(ksnb, 0, ksn, destPos, ksnb.length);
        byte[] leftEnc = encrypt3Des(ksn,bdk);
        byte[] diff =  {(byte)0xC0,(byte)0xC0,(byte)0xC0,(byte)0xC0,0x00,0x00,0x00,0x00,
        		(byte)0xC0,(byte)0xC0,(byte)0xC0,(byte)0xC0,0x00,0x00,0x00,0x00};
        byte[] xor = ByteUtils.xor(diff, bdk);
        byte[] rightEnc = encrypt3Des(ksn,xor);
       byte [] result = new byte[16];
        System.arraycopy(leftEnc, 0, result, 0, 8);
        System.arraycopy(rightEnc, 0, result, 8, 8);
        return result;
    }
    
    private  byte [] encrypt3Des(byte[] data, byte[] keyb){
    	 Cipher encrypter;
		try {
			LOG.info("key length "+keyb.length);
			LOG.info(hexToString(keyb));
	        byte[] key =new byte[16];
	        int minLength=8;
	        if(keyb.length == minLength) {
	        	System.arraycopy(keyb, 0, key, 0, 8);
	        	System.arraycopy(keyb, 0, key, 8, 8);
	        }
	        else {
	        	key = keyb;
	        }
			encrypter = Cipher.getInstance("DESede/ECB/PKCS7Padding", "BC");
			 encrypter.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DESede")); 
	         return encrypter.doFinal(data);
		} catch (NoSuchAlgorithmException |NoSuchProviderException |InvalidKeyException |IllegalBlockSizeException | BadPaddingException e) {
			LOG.error("en3Ds",e);
			return new byte[0];
		} catch (NoSuchPaddingException e) {
			LOG.error("en3Ds2",e);
			return new byte[0];
		}
        
    }
    
    public  byte[] decrypt3DES(byte[] data, byte[] keyb) throws GeneralSecurityException {
        LOG.info("key length "+keyb.length);
        byte[] key =new byte[16];
        int minLength=8;
        if(keyb.length == minLength) {
        	System.arraycopy(keyb, 0, key, 0, minLength);
        	System.arraycopy(keyb, 0, key, minLength, minLength);
        }
        else {
        	key = keyb;
        }
        Key skey = new SecretKeySpec(key, "DESede");
        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS7Padding");
        cipher.init(Cipher.DECRYPT_MODE, skey, new IvParameterSpec(new byte[8]));
        return cipher.doFinal(data);
    }
    
    public static String hexToString(byte[] hex) {
        if(hex==null || hex.length== 0) {
            return "00";
        }
        StringBuilder ret = new StringBuilder();
        
        for (byte b : hex) {
            ret.append( Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return ret.toString().toUpperCase(Locale.getDefault());
    }

    public static byte[] hexStringToByteArray(String str) {
        if(str == null || str.length()==0){
           return new byte[0];
        }
        int len = str.length();
        byte[] data = new byte[len / 2];
        for (int index = 0; index < len; index += 2) {
            data[index / 2] = (byte) ((Character.digit(str.charAt(index), 16) << 4)
                    + Character.digit(str.charAt(index + 1), 16));
        }
        return data;
    }
    public boolean verifyPIN(String env,CheckPinDto dto) {
        try {
        	if(Constantes.LOCAL_ENV.equals(env)) {
        		return true;
        	}else {
        	String pinBlockFormat="01";
        	String panSeq = dto.getPan().substring(dto.getPan().length()-13, dto.getPan().length()-1);
        	String cmd = "0123BEU"+dto.getZpk()+dto.getPinBlock()+pinBlockFormat+panSeq+dto.getPin();
            byte[] header = cmd.getBytes(StandardCharsets.US_ASCII);
            int len = header.length;
            byte[] com = new byte[len + 2];
            System.arraycopy(header, 0, com, 2, header.length);
            com[0] = 0x00;
            com[1] = (byte) len;
            byte [] respb = sendCommand(com);
            String resp = new String(respb,StandardCharsets.US_ASCII);
            return resp.contains("0123BF00");
        	}
        } catch (Exception e) {
            LOG.log(Level.FATAL, "errorCheckPin", e);
            return false;
        }
    }
    public byte[] translatePINfromBDKToZPK(String ksn, String pinBlock,String pan,String zpk,String posBdk) {
        try {
        	String ksnDesc ="A05";
        	String pinBlockFormat="01";
        	String panSeq = pan.substring(pan.length()-13, pan.length()-1);
        	String cmd = "0123G0U"+posBdk+"U"+zpk+ksnDesc+ksn+pinBlock+pinBlockFormat+pinBlockFormat+panSeq;
            byte[] header = cmd.getBytes(StandardCharsets.US_ASCII);
            int len = header.length;
            byte[] com = new byte[len + 2];
            System.arraycopy(header, 0, com, 2, header.length);
            com[0] = 0x00;
            com[1] = (byte) len;
            byte [] respb = sendCommand(com);
            String resp = new String(respb,StandardCharsets.US_ASCII);
            String headerResp = "0123G100";
            LOG.info(resp);
            if (resp.contains(headerResp)) {
            	int index = resp.indexOf(headerResp)+headerResp.length()+2;
                String pinB = resp.substring(index,index+16);
            	return hexStringToByteArray(pinB);
            	
            } else {
                return hexStringToByteArray("8092");
            }
        } catch (Exception e) {
            LOG.log(Level.FATAL, "error", e);
            return hexStringToByteArray("8092");
        }
    }
    
    public byte[] generateZMK(String key1,String key2,String key3) {

        try {
        	connect();
            byte[] header = ("0123GY3"+key1+key2+key3).getBytes(StandardCharsets.US_ASCII);
            int len = header.length;
            byte[] com = new byte[len + 2];
            System.arraycopy(header, 0, com, 2, header.length);
            com[0] = 0x00;
            com[1] = (byte) len;
            byte [] resb = sendCommand(com);
            String resp = new String(resb,StandardCharsets.US_ASCII);
            
            if (resp.contains("0123GZ00")) {
            	int index = resp.indexOf("0123GZ00U");
                String key = resp.substring(index+8);
            	return hexStringToByteArray(key);
            } else {
                return new byte[]{0x39,0x39,0x39};
            }
        } catch (Exception e) {
            LOG.log(Level.FATAL, "error", e);
            return new byte[]{0x39,0x39,0x39};
        }
    }
    public boolean verifyPINForId(String env,CheckPinDto req, Card card) {
    	/*boolean resp=true;
    	try {
    		if(Constantes.PROD_ENV.equals(env)||Constantes.PREPROD_ENV.equals(env)) {
    			CheckPinDto reqPin =new CheckPinDto();
    			req.setPin(card.getPin());
    			req.setPanSeq(card.getPan().substring(card.getPan().length()-13, card.getPan().length()-1));
    			String decryptedPIN = decrytPin(reqPin);
    			byte[] pinBlockB =Base64.getDecoder().decode(req.getPinBlock());
    			String pinBlock = new String(pinBlockB,StandardCharsets.UTF_8);
    			String decrypt = hexToString(Base64.getDecoder().decode(req.getTransactionId()));
    			String  encrypt = decryptedPIN+ req.getTimestamp()+decrypt;
    			String encode =DigestUtils.md5Hex(encrypt.getBytes(StandardCharsets.UTF_8));
    			resp = encode.equals(pinBlock)?true:false;
    		}else {
    			resp =true;
    		}
    	}catch (Exception e) {
    		LOG.error("pinForId",e);
	           resp =false;
    	}*/
    	return true;
    }
    public String decrytPin(CheckPinDto req) {
        //ClientHSM
        //0123KQ + 02 + 00 + CLE MK-AC(LMK) + PAN + ATC(TAG 9F36) + UN (TAG 9F37) + ARQC (9F26) + ARC ( TAG 8A)
    	String resp ="";
        try {
            
                byte[] decrypt = ("0123NG" + req.getPanSeq() + req.getPin()).getBytes("UTF-8");
                String dec = new String(sendCommand(decrypt)).substring(4);
                int index = dec.indexOf('N');
                dec = dec.substring(index);
                if (dec.startsWith("NH00")) {
                    resp =dec.substring(4, 8);
                }
            
        } catch (UnsupportedEncodingException e) {
            LOG.error("error to calculate PIN 1",e);
            closeHsm();
        
        } catch (Exception e) {
        	closeHsm();
        	LOG.error("error to calculate PIN ",e);
         }
        return resp;
        
    }
    
    public   byte [] decrypt3DesPKC5(byte[] data, byte[] key){
        try {
            Cipher encrypter;
            encrypter = Cipher.getInstance("TripleDES/CBC/PKCS5Padding");
            encrypter.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key,"TripleDES"));
            return encrypter.doFinal(data);
        } catch (NoSuchAlgorithmException  | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            LOG.error("decr3Ds",e);
            return new byte[0];
        } catch (NoSuchPaddingException e) {
            LOG.error("edecry3Ds2",e);
            return new byte[0];
        }

    }
}
