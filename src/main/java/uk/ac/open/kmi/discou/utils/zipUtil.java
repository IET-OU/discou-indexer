package uk.ac.open.kmi.discou.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;


public class zipUtil {

	 public  String gzipStringEncode(String str) throws Exception{
	        //Stream to obtain the byte encoded
	        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	        //Stream per a compressione legato al ByteStream per ottenere il risultato
	        GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);
	        //Scrittura dei byte della stringa nello stream per la compressione
	        gzipStream.write(str.getBytes());
	        //Chiusura stream
	        byteStream.close();
	        gzipStream.close();
	        //Restituzione byte compressi codificati in Base64 per essere spediti
	        return Base64.encodeBase64String(byteStream.toByteArray());
	    }
	 
	 
	    public  String gzipStringDecode(String str) throws Exception{
	        //Stream per l'immissione dei byte compressi
	        ByteArrayInputStream byteStream = new ByteArrayInputStream(Base64.decodeBase64(str));
	        //Stream per a decompressione legato al ByteStream per ottenere il risultato
	        GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
	        int nread;
	        String result = "";
	        byte[] bytes = new byte[1024];
	        //Si cicla finch� ci sono byte disponibili
	        while(gzipStream.available()>0){
	            //Lettura dei byte con inserimento nel buffer
	            nread = gzipStream.read(bytes);
	            //Se sono stati letti dei byte
	            if( nread > 0 ){
	                //Concatenazione dei byte convertiti in stringa al risultato
	                result += new String(bytes,0,nread);
	            }
	        }
	        //Restituzione risultato
	        return result;
	    }
	
}
