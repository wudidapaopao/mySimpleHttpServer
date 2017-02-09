package com.yxz.myHttpServer.http;

/**
* @author Yu 
* cookie首部包装类
*/
public class HttpCookie extends HttpHeader{

	//cookie作用范围的域名
	private static final String DOMAIN = "domain";
	//cookie作用范围的url路径
	private static final String PATH = "path";
	//cookie的过期时间
	private static final String EXPIRE = "exprie";
	//cookie是否只在ssl下传输
	private static final String SECURE = "secure";
	
	public HttpCookie(String name, String value, String domain, String path, String expire, boolean secure) {
		buildCookie(name, value, domain, path, expire, secure);
	}

	private void buildCookie(String name, String value, String domain, String path, String expire, boolean secure) {
		setName(HttpHeader.SET_COOKIE);
		StringBuilder sb = new StringBuilder();
		sb.append(name + HttpConstants.EQUALS + value);
		addSeperate(sb);
		if(domain != null && domain.length() > 0) {
			sb.append(DOMAIN + HttpConstants.EQUALS + domain);
			addSeperate(sb);
		}
		if(path != null && path.length() > 0) {
			sb.append(PATH + HttpConstants.EQUALS + path);
			addSeperate(sb);
		}
		if(expire != null && expire.length() > 0) {
			sb.append(EXPIRE + HttpConstants.EQUALS + expire);
			addSeperate(sb);
		}
		if(secure) {
			sb.append(SECURE);
		}
		setValue(sb.toString());
	}

	private void addSeperate(StringBuilder sb) {
		sb.append(HttpConstants.SEMICOLON);
		sb.append(HttpConstants.SP);
	}	
	
}
