/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import eu.webtoolkit.jwt.servlet.WebRequest;

/**
 * An environment for testing purposes
 * <p>
 * 
 * This environment is useful for use in unit tests: you may configure its
 * properties and pass it to the constructor of an application.
 * <p>
 * This is useful for (unit) test-cases: <code>
 void test() { <br> 
   Test::WTestEnvironment environment; <br> 
   MyApplication app(environment); <br> 
   ... <br> 
 }
</code>
 * <p>
 * 
 * @see WEnvironment
 * @see WApplication#WApplication(WEnvironment env)
 */
public class WTestEnvironment extends WEnvironment {
	/**
	 * Default constructor.
	 * <p>
	 * Constructs a test environment that resembles FireFox 3.0 with default
	 * settings.
	 * <p>
	 * The <i>applicationPath</i> is the simulated deployment path. The
	 * configuration file points to a wt_config.xml configuration file. The
	 * <i>type</i> indicates the application type.
	 * <p>
	 * After construction, but before passing it to the constructor of a
	 * {@link WApplication}, you can change any of the environment properties
	 * using the setter methods.
	 */
	public WTestEnvironment(Configuration configuration, ApplicationType type) {
		super();
		List<String> dummy = new ArrayList<String>();
		this.configuration_ = configuration;
		this.controller_ = new TestController(configuration);
		this.init(type);
	}

	/**
	 * Default constructor.
	 * <p>
	 * Calls
	 * {@link #WTestEnvironment(Configuration configuration, ApplicationType type)
	 * this(configuration, ApplicationType.Application)}
	 */
	public WTestEnvironment(Configuration configuration) {
		this(configuration, ApplicationType.Application);
	}

	/**
	 * Destructor.
	 * <p>
	 * !
	 */
	public void destroy() {
		/* delete WebSession.Handler.getInstance() */;
		/* delete this.session_ */;
		/* delete this.controller_ */;
		/* delete this.configuration_ */;
	}

	/**
	 * Sets parameters to the application.
	 * <p>
	 * The default value is an empty map.
	 * <p>
	 * 
	 * @see WEnvironment#getParameterMap()
	 */
	public void setParameterMap(Map<String, List<String>> parameters) {
		this.parameters_ = parameters;
	}

	/**
	 * Sets HTTP cookies.
	 * <p>
	 * The default value is an empty map.
	 * <p>
	 * 
	 * @see WEnvironment#getCookies()
	 */
	public void setCookies(Map<String, String> cookies) {
		this.cookies_ = cookies;
	}

	/**
	 * Sets a HTTP header value.
	 * <p>
	 * The default value is no headers.
	 * <p>
	 * 
	 * @see WEnvironment#getHeaderValue(String name)
	 */
	public void setHeaderValue(String value) {
	}

	/**
	 * Sets whether cookies are supported.
	 * <p>
	 * The default value is <i>true</i>.
	 * <p>
	 * 
	 * @see WEnvironment#supportsCookies()
	 */
	public void setSupportsCookies(boolean enabled) {
		this.doesCookies_ = enabled;
	}

	/**
	 * Sets whether AJAX is supported.
	 * <p>
	 * The default value is <i>true</i>.
	 * <p>
	 * 
	 * @see WEnvironment#hasAjax()
	 */
	public void setAjax(boolean enabled) {
		this.doesAjax_ = enabled;
	}

	/**
	 * Sets the display&apos;s DPI scale.
	 * <p>
	 * The default value is 1.
	 * <p>
	 * 
	 * @see WEnvironment#getDpiScale()
	 */
	public void setDpiScale(double dpiScale) {
		this.dpiScale_ = dpiScale;
	}

	/**
	 * Sets the locale.
	 * <p>
	 * The default value is &quot;en&quot;.
	 * <p>
	 * 
	 * @see WEnvironment#getLocale()
	 */
	public void setLocale(Locale locale) {
		this.locale_ = locale;
	}

	/**
	 * Sets the host name.
	 * <p>
	 * The default value is &quot;localhost&quot;.
	 * <p>
	 * 
	 * @see WEnvironment#getHostName()
	 */
	public void setHostName(String hostName) {
		this.host_ = hostName;
	}

	/**
	 * Sets the URL scheme.
	 * <p>
	 * The default value is &quot;http&quot;.
	 * <p>
	 * 
	 * @see WEnvironment#getUrlScheme()
	 */
	public void setUrlScheme(String scheme) {
		this.urlScheme_ = scheme;
	}

	/**
	 * Sets the user agent.
	 * <p>
	 * The default value is no &quot;Mozilla/5.0 (X11; U; Linux x86_64; en-US;
	 * rv:1.9.0.11) Gecko/2009060309 Ubuntu/9.04 (jaunty) Firefox/3.0.11&quot;.
	 * <p>
	 * 
	 * @see WEnvironment#getUserAgent()
	 */
	public void setUserAgent(String userAgent) {
		super.setUserAgent(userAgent);
	}

	/**
	 * Sets the referer.
	 * <p>
	 * The default value is &quot;&quot;.
	 * <p>
	 * 
	 * @see WEnvironment#getReferer()
	 */
	public void setReferer(String referer) {
		this.referer_ = referer;
	}

	/**
	 * Sets the accept header.
	 * <p>
	 * The default value is
	 * &quot;text/html,application/xhtml+xml,application/xml;q=0.9,*
	 * /*;q=0.8&quot;.
	 * <p>
	 * 
	 * @see WEnvironment#getAccept()
	 */
	public void setAccept(String accept) {
		this.accept_ = accept;
	}

	/**
	 * Sets the server signature.
	 * <p>
	 * The default value is &quot;None (WTestEnvironment)&quot;.
	 * <p>
	 * 
	 * @see WEnvironment#getServerSignature()
	 */
	public void setServerSignature(String signature) {
		this.serverSignature_ = signature;
	}

	/**
	 * Sets the server software.
	 * <p>
	 * The default value is &quot;None (WTestEnvironment)&quot;.
	 * <p>
	 * 
	 * @see WEnvironment#getServerSoftware()
	 */
	public void setServerSoftware(String software) {
		this.serverSignature_ = software;
	}

	/**
	 * Sets the server admin.
	 * <p>
	 * The default value is &quot;your@onyourown.here&quot;.
	 * <p>
	 * 
	 * @see WEnvironment#getServerAdmin()
	 */
	public void setServerAdmin(String serverAdmin) {
		this.serverAdmin_ = serverAdmin;
	}

	/**
	 * Sets the client address.
	 * <p>
	 * The default value is &quot;127.0.0.1&quot;.
	 * <p>
	 * 
	 * @see WEnvironment#getClientAddress()
	 */
	public void setClientAddress(String clientAddress) {
		this.clientAddress_ = clientAddress;
	}

	/**
	 * Sets the initial internal path.
	 * <p>
	 * The default value is &quot;&quot;.
	 * <p>
	 * 
	 * @see WEnvironment#getInternalPath()
	 */
	public void setInternalPath(String internalPath) {
		super.setInternalPath(internalPath);
	}

	/**
	 * Sets the content type.
	 * <p>
	 * The default value is XHTML1.
	 * <p>
	 * 
	 * @see WEnvironment#getContentType()
	 */
	public void setContentType(WEnvironment.ContentType contentType) {
		this.contentType_ = contentType;
	}

	private Configuration configuration_;
	private WebSession session;
	private WtServlet controller_;

	private void init(ApplicationType type) {
		this.session_ = new WebSession(this.controller_, "testwtd", type, "",
				(WebRequest) null, this);
		new WebSession.Handler(this.session_, true);
		this.doesAjax_ = true;
		this.doesCookies_ = true;
		this.dpiScale_ = 1;
		this.contentType_ = WEnvironment.ContentType.XHTML1;
		this.urlScheme_ = "http";
		this.referer_ = "";
		this.accept_ = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
		this.serverSignature_ = "None (WTestEnvironment)";
		this.serverSoftware_ = this.serverSignature_;
		this.serverAdmin_ = "your@onyourown.here";
		this.pathInfo_ = "";
		this
				.setUserAgent("Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.0.11) Gecko/2009060309 Ubuntu/9.04 (jaunty) Firefox/3.0.11");
		this.host_ = "localhost";
		this.clientAddress_ = "127.0.0.1";
		this.locale_ = new Locale("en");
	}
}