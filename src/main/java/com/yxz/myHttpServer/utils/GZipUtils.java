package com.yxz.myHttpServer.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
* @author Yu 
* gzip工具类
*/
public class GZipUtils {  
	
    public static void main(String[] args) throws Exception {    
//    	String a = "F:/javaee/myHttpServer/webroot/test.txt";
//    	String b = "F:/javaee/myHttpServer/webroot/test.gzip";
//    	gzipFile(a, b);
//    	gzip(new byte[]{1,2,3,4,5,6});
    }   
    
	public static byte[] gzip(byte[] bytes) throws Exception {  
		ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);  
		GZIPOutputStream gos = null;  
		try {  
			gos = new GZIPOutputStream(bos);  
			gos.write(bytes, 0, bytes.length);  
			gos.finish();  
			gos.flush();  
			bos.flush();  
			bytes = bos.toByteArray();  
		} finally {  
			if (gos != null)  
				gos.close();  
			if (bos != null)  
				bos.close();  
		}  
		return bytes;  
	}  
	    
	public static void gzipFile(String source, String target) throws IOException {  
		FileInputStream fin = null;  
		FileOutputStream fout = null;  
		GZIPOutputStream gzout = null;  
		try {  
			fin = new FileInputStream(source);  
			fout = new FileOutputStream(target);  
			gzout = new GZIPOutputStream(fout);  
			byte[] buf = new byte[1024];  
			int num = 0;  
			while((num = fin.read(buf)) != -1) {  
				gzout.write(buf, 0, num);  
			}  
		}finally {  
			if(gzout != null)  
				gzout.close();  
			if (fout != null)  
				fout.close();  
			if (fin != null)  
				fin.close();  
		}  
	 }  
	
}  