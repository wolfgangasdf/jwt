/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import eu.webtoolkit.jwt.servlet.WebResponse;
import eu.webtoolkit.jwt.utils.MathUtils;
import eu.webtoolkit.jwt.utils.OrderedMultiMap;

class WebRenderer implements SlotLearnerInterface {
	public WebRenderer(WebSession session) {
		super();
		this.session_ = session;
		this.visibleOnly_ = true;
		this.rendered_ = false;
		this.twoPhaseThreshold_ = 5000;
		this.expectedAckId_ = 0;
		this.cookiesToSet_ = new ArrayList<WebRenderer.Cookie>();
		this.currentFormObjects_ = new HashMap<String, WObject>();
		this.currentFormObjectsList_ = "";
		this.collectedJS1_ = new StringWriter();
		this.collectedJS2_ = new StringWriter();
		this.invisibleJS_ = new StringWriter();
		this.statelessJS_ = new StringWriter();
		this.beforeLoadJS_ = new StringWriter();
		this.updateMap_ = new HashSet<WWidget>();
		this.learning_ = false;
	}

	public void setTwoPhaseThreshold(int bytes) {
		this.twoPhaseThreshold_ = bytes;
	}

	public boolean isVisibleOnly() {
		return this.visibleOnly_;
	}

	public void setVisibleOnly(boolean how) {
		this.visibleOnly_ = how;
	}

	public void needUpdate(WWidget w, boolean laterOnly) {
		this.updateMap_.add(w);
		if (!laterOnly) {
			this.moreUpdates_ = true;
		}
	}

	public void doneUpdate(WWidget w) {
		this.updateMap_.remove(w);
	}

	public void updateFormObjects(WWebWidget source, boolean checkDescendants) {
		this.formObjectsChanged_ = true;
	}

	enum ResponseType {
		Page, Script, Update;

		/**
		 * Returns the numerical representation of this enum.
		 */
		public int getValue() {
			return ordinal();
		}
	}

	public void updateFormObjectsList(WApplication app) {
		if (this.formObjectsChanged_) {
			this.currentFormObjects_.clear();
			app.domRoot_.getFormObjects(this.currentFormObjects_);
			if (app.domRoot2_ != null) {
				app.domRoot2_.getFormObjects(this.currentFormObjects_);
			}
		}
	}

	public Map<String, WObject> getFormObjects() {
		return this.currentFormObjects_;
	}

	public void saveChanges() throws IOException {
		this.collectJS(this.collectedJS1_);
	}

	public void discardChanges() throws IOException {
		this.collectJS((Writer) null);
	}

	public void letReloadJS(WebResponse response, boolean newSession,
			boolean embedded) throws IOException {
		if (!embedded) {
			this.setHeaders(response, "text/javascript; charset=UTF-8");
		}
		response.out().append("window.location.reload(true);");
	}

	public final void letReloadJS(WebResponse response, boolean newSession)
			throws IOException {
		letReloadJS(response, newSession, false);
	}

	public void letReloadHTML(WebResponse response, boolean newSession)
			throws IOException {
		this.setHeaders(response, "text/html; charset=UTF-8");
		response.out().append("<html><script type=\"text/javascript\">");
		this.letReloadJS(response, newSession, true);
		response.out().append("</script><body></body></html>");
	}

	public boolean isDirty() {
		return !this.updateMap_.isEmpty()
				|| this.collectedJS1_.getBuffer().length() > 0
				|| this.collectedJS2_.getBuffer().length() > 0
				|| this.invisibleJS_.getBuffer().length() > 0;
	}

	public void serveResponse(WebResponse response,
			WebRenderer.ResponseType responseType) throws IOException {
		switch (responseType) {
		case Update:
			this.serveJavaScriptUpdate(response);
			break;
		case Page:
			if (this.session_.getApp() != null) {
				this.serveMainpage(response);
			} else {
				this.serveBootstrap(response);
			}
			break;
		case Script:
			this.serveMainscript(response);
			break;
		}
	}

	// public void serveError(WebResponse request, RuntimeException error,
	// WebRenderer.ResponseType responseType) ;
	public void serveError(WebResponse response, String message,
			WebRenderer.ResponseType responseType) throws IOException {
		boolean js = responseType == WebRenderer.ResponseType.Update
				|| responseType == WebRenderer.ResponseType.Script;
		WApplication app = this.session_.getApp();
		if (!js || !(app != null)) {
			response.setContentType("text/html");
			response.out().append("<title>Error occurred.</title>").append(
					"<h2>Error occurred.</h2>").append(
					WWebWidget.escapeText(new WString(message), true)
							.toString()).append('\n');
		} else {
			response
					.out()
					.append(app.getJavaScriptClass())
					.append(
							"._p_.quit();document.title = 'Error occurred.';document.body.innerHtml='<h2>Error occurred.</h2>' +");
			DomElement.jsStringLiteral(response.out(), message, '\'');
			response.out().append(";");
		}
	}

	public void setCookie(final String name, final String value, int maxAge,
			final String domain, final String path) {
		this.cookiesToSet_.add(new WebRenderer.Cookie(name, value, path,
				domain, maxAge));
	}

	public boolean isPreLearning() {
		return this.learning_;
	}

	public void learningIncomplete() {
		this.learningIncomplete_ = true;
	}

	public void ackUpdate(int updateId) {
		if (updateId == this.expectedAckId_) {
			this.setJSSynced(false);
			++this.expectedAckId_;
		}
	}

	public void streamRedirectJS(Writer out, String redirect)
			throws IOException {
		if (this.session_.getApp() != null) {
			out.append("if (window.").append(
					this.session_.getApp().getJavaScriptClass()).append(") ")
					.append(this.session_.getApp().getJavaScriptClass())
					.append("._p_.setHash('").append(
							this.session_.getApp().getInternalPath()).append(
							".');\n");
		}
		out.append("if (window.location.replace) window.location.replace('")
				.append(redirect).append("');else window.location.href='")
				.append(redirect).append("';\n");
	}

	static class Cookie {
		public String name;
		public String value;
		public String path;
		public String domain;
		public int maxAge;

		public Cookie(String n, String v, String p, String d, int m) {
			this.name = n;
			this.value = v;
			this.path = p;
			this.domain = d;
			this.maxAge = m;
		}
	}

	private WebSession session_;
	private boolean visibleOnly_;
	private boolean rendered_;
	private int twoPhaseThreshold_;
	private int expectedAckId_;
	private List<WebRenderer.Cookie> cookiesToSet_;
	private Map<String, WObject> currentFormObjects_;
	private String currentFormObjectsList_;
	private boolean formObjectsChanged_;

	private void setHeaders(WebResponse response, final String mimeType) {
		for (int i = 0; i < this.cookiesToSet_.size(); ++i) {
			String cookies = "";
			String value = this.cookiesToSet_.get(i).value;
			cookies += DomElement.urlEncodeS(this.cookiesToSet_.get(i).name)
					+ "=" + DomElement.urlEncodeS(value) + "; Version=1;";
			if (this.cookiesToSet_.get(i).maxAge != -1) {
				cookies += " Max-Age="
						+ String.valueOf(this.cookiesToSet_.get(i).maxAge)
						+ ";";
			}
			if (this.cookiesToSet_.get(i).domain.length() != 0) {
				cookies += " Domain=" + this.cookiesToSet_.get(i).domain + ";";
			}
			if (this.cookiesToSet_.get(i).path.length() != 0) {
				cookies += " Path=" + this.cookiesToSet_.get(i).path + ";";
			}
			if (cookies.length() != 0) {
				response.addHeader("Set-Cookie", cookies);
			}
		}
		this.cookiesToSet_.clear();
		response.setContentType(mimeType);
	}

	private void serveJavaScriptUpdate(WebResponse response) throws IOException {
		this.rendered_ = true;
		this.setHeaders(response, "text/javascript; charset=UTF-8");
		this.collectJavaScript();
		response.out().append(this.collectedJS1_.toString()).append(
				this.collectedJS2_.toString()).append(
				this.session_.getApp().getJavaScriptClass()).append(
				"._p_.response(").append(String.valueOf(this.expectedAckId_))
				.append(");");
		if (response.isWebSocketRequest() || response.isWebSocketMessage()) {
			this.setJSSynced(false);
		}
	}

	private void serveMainscript(WebResponse response) throws IOException {
		Configuration conf = this.session_.getController().getConfiguration();
		boolean widgetset = this.session_.getType() == EntryPointType.WidgetSet;
		this.setHeaders(response, "text/javascript; charset=UTF-8");
		if (!widgetset) {
			String redirect = this.session_.getRedirect();
			if (redirect.length() != 0) {
				this.streamRedirectJS(response.out(), redirect);
				return;
			}
		}
		WApplication app = this.session_.getApp();
		final boolean xhtml = app.getEnvironment().getContentType() == WEnvironment.ContentType.XHTML1;
		final boolean innerHtml = !xhtml || app.getEnvironment().agentIsGecko();
		this.formObjectsChanged_ = true;
		this.currentFormObjectsList_ = this.createFormObjectsList(app);
		FileServe jquery = new FileServe(WtServlet.JQuery_js);
		jquery.stream(response.out());
		FileServe script = new FileServe(WtServlet.Wt_js);
		script.setCondition("DEBUG", conf.isDebug());
		script.setCondition("DYNAMIC_JS", false);
		script.setVar("WT_CLASS", "Wt3_1_7");
		script.setVar("APP_CLASS", app.getJavaScriptClass());
		script.setVar("AUTO_JAVASCRIPT", "(function(){" + app.autoJavaScript_
				+ "})");
		script.setCondition("STRICTLY_SERIALIZED_EVENTS", conf
				.isSerializedEvents());
		script.setCondition("WEB_SOCKETS", conf.isWebSockets());
		script.setVar("INNER_HTML", innerHtml);
		script.setVar("FORM_OBJECTS", '[' + this.currentFormObjectsList_ + ']');
		script.setVar("RELATIVE_URL", WWebWidget.jsStringLiteral(this.session_
				.getBootstrapUrl(response,
						WebSession.BootstrapOption.ClearInternalPath)));
		script.setVar("KEEP_ALIVE", String
				.valueOf(conf.getSessionTimeout() / 2));
		script.setVar("INITIAL_HASH", WWebWidget.jsStringLiteral(app
				.getInternalPath()));
		script.setVar("INDICATOR_TIMEOUT", conf.getIndicatorTimeout());
		script
				.setVar("SERVER_PUSH_TIMEOUT",
						conf.getServerPushTimeout() * 1000);
		script.setVar("ONLOAD", "(function() {"
				+ (widgetset ? "" : "window.loadWidgetTree();") + "})");
		script.stream(response.out());
		app.autoJavaScriptChanged_ = false;
		this.streamCommJs(app, response.out());
		if (!this.rendered_) {
			this.serveMainAjax(response);
		} else {
			if (app.enableAjax_) {
				this.collectedJS1_.append(this.beforeLoadJS_.toString());
				this.beforeLoadJS_ = new StringWriter();
				this.collectedJS1_
						.append("var domRoot = ")
						.append(app.domRoot_.getJsRef())
						.append(
								";var form = Wt3_1_7.getElement('Wt-form');domRoot.style.display = form.style.display;document.body.replaceChild(domRoot, form);");
				this.loadScriptLibraries(this.collectedJS1_, app, true);
				this.collectedJS1_.append(app.getNewBeforeLoadJavaScript());
				this.collectedJS2_.append(
						"domRoot.style.visibility = 'visible';").append(
						app.getJavaScriptClass()).append(
						"._p_.autoJavaScript();");
				this.loadScriptLibraries(this.collectedJS2_, app, false);
				app.enableAjax_ = false;
			}
			response.out().append("window.loadWidgetTree = function(){\n");
			this.visibleOnly_ = false;
			this.collectJavaScript();
			this.updateLoadIndicator(this.collectedJS1_, app, true);
			response.out().append(this.collectedJS1_.toString()).append(
					app.getJavaScriptClass()).append("._p_.response(").append(
					String.valueOf(this.expectedAckId_)).append(");");
			response.out().append(app.getJavaScriptClass()).append(
					"._p_.update(null, 'load', null, false);").append(
					this.collectedJS2_.toString()).append("};").append(
					app.getJavaScriptClass()).append("._p_.setServerPush(")
					.append(app.isUpdatesEnabled() ? "true" : "false").append(
							");").append("window.WtScriptLoaded = true;")
					.append("if (window.isLoaded) onLoad();\n");
		}
	}

	private void serveBootstrap(WebResponse response) throws IOException {
		boolean xhtml = this.session_.getEnv().getContentType() == WEnvironment.ContentType.XHTML1;
		Configuration conf = this.session_.getController().getConfiguration();
		FileServe boot = new FileServe(WtServlet.Boot_html);
		this.setPageVars(boot);
		this.setBootVars(response, boot);
		StringWriter noJsRedirectUrl = new StringWriter();
		DomElement.htmlAttributeValue(noJsRedirectUrl, this.session_
				.getBootstrapUrl(response,
						WebSession.BootstrapOption.KeepInternalPath)
				+ "&js=no");
		if (xhtml) {
			boot.setVar("AUTO_REDIRECT", "");
			boot.setVar("NOSCRIPT_TEXT", conf.getRedirectMessage());
		} else {
			boot.setVar("AUTO_REDIRECT",
					"<noscript><meta http-equiv=\"refresh\" content=\"0;url="
							+ noJsRedirectUrl.toString() + "\"></noscript>");
			boot.setVar("NOSCRIPT_TEXT", conf.getRedirectMessage());
		}
		boot.setVar("REDIRECT_URL", noJsRedirectUrl.toString());
		response.addHeader("Cache-Control", "no-cache, no-store");
		response.addHeader("Expires", "-1");
		String contentType = xhtml ? "application/xhtml+xml" : "text/html";
		contentType += "; charset=UTF-8";
		this.setHeaders(response, contentType);
		boot.stream(response.out());
		this.rendered_ = false;
	}

	private void serveMainpage(WebResponse response) throws IOException {
		Configuration conf = this.session_.getController().getConfiguration();
		WApplication app = this.session_.getApp();
		if (!app.getEnvironment().hasAjax()
				&& (app.internalPathIsChanged_ && !app.oldInternalPath_
						.equals(app.newInternalPath_))) {
			app.oldInternalPath_ = app.newInternalPath_;
			this.session_.redirect(app.getBookmarkUrl(app.newInternalPath_));
		}
		String redirect = this.session_.getRedirect();
		if (redirect.length() != 0) {
			response.setStatus(302);
			response.sendRedirect(redirect);
			return;
		}
		WWebWidget mainWebWidget = app.domRoot_;
		this.visibleOnly_ = true;
		DomElement mainElement = mainWebWidget.createSDomElement(app);
		this.rendered_ = true;
		this.setJSSynced(true);
		final boolean xhtml = app.getEnvironment().getContentType() == WEnvironment.ContentType.XHTML1;
		String styleSheets = "";
		if (app.getCssTheme().length() != 0) {
			styleSheets += "<link href=\"" + WApplication.getResourcesUrl()
					+ "/themes/" + app.getCssTheme()
					+ "/wt.css\" rel=\"stylesheet\" type=\"text/css\""
					+ (xhtml ? "/>" : ">") + "\n";
			if (app.getEnvironment().agentIsIE()) {
				styleSheets += "<link href=\"" + WApplication.getResourcesUrl()
						+ "/themes/" + app.getCssTheme()
						+ "/wt_ie.css\" rel=\"stylesheet\" type=\"text/css\""
						+ (xhtml ? "/>" : ">") + "\n";
			}
			if (app.getEnvironment().getAgent() == WEnvironment.UserAgent.IE6) {
				styleSheets += "<link href=\"" + WApplication.getResourcesUrl()
						+ "/themes/" + app.getCssTheme()
						+ "/wt_ie6.css\" rel=\"stylesheet\" type=\"text/css\""
						+ (xhtml ? "/>" : ">") + "\n";
			}
		}
		for (int i = 0; i < app.styleSheets_.size(); ++i) {
			String url = app.styleSheets_.get(i).uri;
			url = StringUtils.replace(url, '&', "&amp;");
			styleSheets += "<link href=\"" + app.fixRelativeUrl(url)
					+ "\" rel=\"stylesheet\" type=\"text/css\"";
			if (app.styleSheets_.get(i).media.length() != 0
					&& !app.styleSheets_.get(i).media.equals("all")) {
				styleSheets += " media=\"" + app.styleSheets_.get(i).media
						+ '"';
			}
			styleSheets += xhtml ? "/>" : ">";
			styleSheets += "\n";
		}
		app.styleSheetsAdded_ = 0;
		this.beforeLoadJS_ = new StringWriter();
		for (int i = 0; i < app.scriptLibraries_.size(); ++i) {
			String url = app.scriptLibraries_.get(i).uri;
			url = StringUtils.replace(url, '&', "&amp;");
			styleSheets += "<script src='" + app.fixRelativeUrl(url)
					+ "'></script>\n";
			this.beforeLoadJS_.append(app.scriptLibraries_.get(i).beforeLoadJS);
		}
		app.scriptLibrariesAdded_ = 0;
		app.newBeforeLoadJavaScript_ = app.beforeLoadJavaScript_;
		boolean hybridPage = this.session_.isProgressiveBoot()
				|| this.session_.getEnv().hasAjax();
		FileServe page = new FileServe(hybridPage ? WtServlet.Hybrid_html
				: WtServlet.Plain_html);
		this.setPageVars(page);
		page.setVar("SESSION_ID", this.session_.getSessionId());
		if (hybridPage) {
			this.setBootVars(response, page);
			page.setVar("INTERNAL_PATH", this.safeJsStringLiteral(app
					.getInternalPath()));
		}
		String url = app.getEnvironment().agentIsSpiderBot()
				|| conf.getSessionTracking() == Configuration.SessionTracking.CookiesURL
				&& this.session_.getEnv().supportsCookies() ? this.session_
				.getBookmarkUrl(app.newInternalPath_) : this.session_
				.getMostRelativeUrl(app.newInternalPath_);
		url = app.fixRelativeUrl(url);
		url = StringUtils.replace(url, '&', "&amp;");
		page.setVar("RELATIVE_URL", url);
		if (conf.isInlineCss()) {
			page.setVar("STYLESHEET", app.getStyleSheet().getCssText(true));
		} else {
			page.setVar("STYLESHEET", "");
		}
		page.setVar("STYLESHEETS", styleSheets);
		page.setVar("TITLE", WWebWidget.escapeText(app.getTitle()).toString());
		app.titleChanged_ = false;
		if (hybridPage) {
			response.addHeader("Cache-Control", "no-cache, no-store");
			response.addHeader("Expires", "-1");
		}
		String contentType = xhtml ? "application/xhtml+xml" : "text/html";
		contentType += "; charset=UTF-8";
		this.setHeaders(response, contentType);
		this.formObjectsChanged_ = true;
		this.currentFormObjectsList_ = this.createFormObjectsList(app);
		page.streamUntil(response.out(), "HTML");
		List<DomElement.TimeoutEvent> timeouts = new ArrayList<DomElement.TimeoutEvent>();
		{
			EscapeOStream js = new EscapeOStream();
			EscapeOStream out = new EscapeOStream(response.out());
			mainElement.asHTML(out, js, timeouts);
			app.doJavaScript(js.toString());
			;
		}
		int refresh;
		if (app.getEnvironment().hasAjax()) {
			StringWriter str = new StringWriter();
			DomElement.createTimeoutJs(str, timeouts, app);
			app.doJavaScript(str.toString());
			refresh = 1000000;
		} else {
			if (app.isQuited()) {
				refresh = 1000000;
			} else {
				refresh = conf.getSessionTimeout() / 3;
				for (int i = 0; i < timeouts.size(); ++i) {
					refresh = Math
							.min(refresh, 1 + timeouts.get(i).msec / 1000);
				}
			}
		}
		page.setVar("REFRESH", String.valueOf(refresh));
		page.stream(response.out());
		app.internalPathIsChanged_ = false;
	}

	private void serveMainAjax(WebResponse response) throws IOException {
		Configuration conf = this.session_.getController().getConfiguration();
		boolean widgetset = this.session_.getType() == EntryPointType.WidgetSet;
		WApplication app = this.session_.getApp();
		WWebWidget mainWebWidget = app.domRoot_;
		this.visibleOnly_ = true;
		app.loadingIndicatorWidget_.show();
		DomElement mainElement = mainWebWidget.createSDomElement(app);
		app.loadingIndicatorWidget_.hide();
		if (conf.isInlineCss()) {
			app.getStyleSheet().javaScriptUpdate(app, response.out(), true);
		}
		if (app.getCssTheme().length() != 0) {
			response.out().append("Wt3_1_7").append(".addStyleSheet('").append(
					WApplication.getResourcesUrl()).append("/themes/").append(
					app.getCssTheme()).append("/wt.css', 'all');");
			if (app.getEnvironment().agentIsIE()) {
				response.out().append("Wt3_1_7").append(".addStyleSheet('")
						.append(WApplication.getResourcesUrl()).append(
								"/themes/").append(app.getCssTheme()).append(
								"/wt_ie.css', 'all');");
			}
			if (app.getEnvironment().getAgent() == WEnvironment.UserAgent.IE6) {
				response.out().append("Wt3_1_7").append(".addStyleSheet('")
						.append(WApplication.getResourcesUrl()).append(
								"/themes/").append(app.getCssTheme()).append(
								"/wt_ie6.css', 'all');");
			}
		}
		app.styleSheetsAdded_ = app.styleSheets_.size();
		this.loadStyleSheets(response.out(), app);
		app.scriptLibrariesAdded_ = app.scriptLibraries_.size();
		this.loadScriptLibraries(response.out(), app, true);
		response.out().append('\n').append(app.getBeforeLoadJavaScript());
		if (!widgetset) {
			response.out().append("window.loadWidgetTree = function(){\n");
		}
		if (app.bodyHtmlClassChanged_) {
			response.out().append("document.body.parentNode.className='")
					.append(app.htmlClass_).append("';").append(
							"document.body.className='").append(app.bodyClass_)
					.append("';");
			app.bodyHtmlClassChanged_ = false;
		}
		Writer s = response.out();
		mainElement.addToParent(s, "document.body", widgetset ? 0 : -1, app);
		;
		if (app.isQuited()) {
			s.append(app.getJavaScriptClass()).append("._p_.quit();");
		}
		if (widgetset) {
			app.domRoot2_.rootAsJavaScript(app, s, true);
		}
		this.setJSSynced(true);
		this.preLearnStateless(app, this.collectedJS1_);
		response.out().append(this.collectedJS1_.toString());
		this.collectedJS1_ = new StringWriter();
		this.updateLoadIndicator(response.out(), app, true);
		if (widgetset) {
			String historyE = app.getEnvironment().getParameter("Wt-history");
			if (historyE != null) {
				response.out().append("Wt3_1_7")
						.append(".history.initialize('").append(
								historyE.charAt(0)).append("-field', '")
						.append(historyE.charAt(0)).append("-iframe');\n");
			}
		}
		response.out().append(app.getAfterLoadJavaScript()).append(
				"{var o=null,e=null;").append(
				app.hideLoadingIndicator_.getJavaScript()).append("}");
		if (widgetset) {
			response.out().append(app.getJavaScriptClass()).append(
					"._p_.load();\n");
		}
		if (!app.isQuited()) {
			response.out().append(this.session_.getApp().getJavaScriptClass())
					.append("._p_.update(null, 'load', null, false);\n");
		}
		if (!widgetset) {
			response.out().append("};\n");
			response.out().append(app.getJavaScriptClass()).append(
					"._p_.setServerPush(").append(
					app.isUpdatesEnabled() ? "true" : "false").append(");");
			response
					.out()
					.append(
							"window.WtScriptLoaded = true;if (window.isLoaded) onLoad();\n");
		}
		this.loadScriptLibraries(response.out(), app, false);
	}

	// private void serveWidgetSet(WebResponse request) ;
	private void streamCommJs(WApplication app, Writer out) throws IOException {
		Configuration conf = this.session_.getController().getConfiguration();
		FileServe js = new FileServe(
				app.getAjaxMethod() == WApplication.AjaxMethod.XMLHttpRequest ? WtServlet.CommAjax_js
						: WtServlet.CommScript_js);
		js.setVar("APP_CLASS", app.getJavaScriptClass());
		js.setVar("WT_CLASS", "Wt3_1_7");
		js
				.setVar(
						"CLOSE_CONNECTION",
						conf.getServerType() == Configuration.ServerType.WtHttpdServer
								&& this.session_.getEnv().agentIsGecko()
								&& this.session_.getEnv().getAgent().getValue() < WEnvironment.UserAgent.Firefox3_0
										.getValue());
		js.stream(out);
	}

	private void collectJavaScript() throws IOException {
		WApplication app = this.session_.getApp();
		Configuration conf = this.session_.getController().getConfiguration();
		this.collectedJS1_.append(this.invisibleJS_.toString());
		this.invisibleJS_ = new StringWriter();
		if (conf.isInlineCss()) {
			app.getStyleSheet()
					.javaScriptUpdate(app, this.collectedJS1_, false);
		}
		this.loadStyleSheets(this.collectedJS1_, app);
		if (app.bodyHtmlClassChanged_) {
			this.collectedJS1_.append("document.body.parentNode.className='")
					.append(app.htmlClass_).append("';").append(
							"document.body.className='").append(app.bodyClass_)
					.append("';");
			app.bodyHtmlClassChanged_ = false;
		}
		this.loadScriptLibraries(this.collectedJS1_, app, true);
		this.loadScriptLibraries(this.collectedJS2_, app, false);
		this.collectedJS1_.append(app.getNewBeforeLoadJavaScript());
		if (app.domRoot2_ != null) {
			app.domRoot2_.rootAsJavaScript(app, this.collectedJS1_, false);
		}
		this.collectJavaScriptUpdate(this.collectedJS1_);
		if (this.visibleOnly_) {
			boolean needFetchInvisible = false;
			if (!this.updateMap_.isEmpty()) {
				needFetchInvisible = true;
				if (this.twoPhaseThreshold_ > 0) {
					this.visibleOnly_ = false;
					this.collectJavaScriptUpdate(this.invisibleJS_);
					if (this.invisibleJS_.getBuffer().length() < (int) this.twoPhaseThreshold_) {
						this.collectedJS1_.append(this.invisibleJS_.toString());
						this.invisibleJS_ = new StringWriter();
						needFetchInvisible = false;
					}
					this.visibleOnly_ = true;
				}
			}
			if (needFetchInvisible) {
				this.collectedJS1_.append(app.getJavaScriptClass()).append(
						"._p_.update(null, 'none', null, false);");
			}
		}
		if (app.autoJavaScriptChanged_) {
			this.collectedJS1_.append(app.getJavaScriptClass()).append(
					"._p_.autoJavaScript=function(){").append(
					app.autoJavaScript_).append("};");
			app.autoJavaScriptChanged_ = false;
		}
		this.visibleOnly_ = true;
		app.domRoot_.doneRerender();
		if (app.domRoot2_ != null) {
			app.domRoot2_.doneRerender();
		}
		String redirect = this.session_.getRedirect();
		if (redirect.length() != 0) {
			this.streamRedirectJS(this.collectedJS1_, redirect);
		}
	}

	private void collectChanges(List<DomElement> changes) {
		WApplication app = this.session_.getApp();
		do {
			this.moreUpdates_ = false;
			OrderedMultiMap<Integer, WWidget> depthOrder = new OrderedMultiMap<Integer, WWidget>();
			for (Iterator<WWidget> i_it = this.updateMap_.iterator(); i_it
					.hasNext();) {
				WWidget i = i_it.next();
				int depth = 1;
				WWidget ww = i;
				WWidget w = ww;
				for (; w.getParent() != null; ++depth) {
					w = w.getParent();
				}
				if (w != app.domRoot_ && w != app.domRoot2_) {
					depth = 0;
				}
				depthOrder.put(depth, ww);
			}
			for (Iterator<Map.Entry<Integer, WWidget>> i_it = depthOrder
					.entrySet().iterator(); i_it.hasNext();) {
				Map.Entry<Integer, WWidget> i = i_it.next();
				boolean j = this.updateMap_.contains(i.getValue());
				if (j != false) {
					WWidget w = i.getValue();
					if (i.getKey() == 0) {
						w.getWebWidget().propagateRenderOk();
						continue;
					}
					if (!this.learning_ && this.visibleOnly_) {
						if (w.isRendered()) {
							w.getSDomChanges(changes, app);
						}
					} else {
						w.getSDomChanges(changes, app);
					}
				}
			}
		} while (!this.learning_ && this.moreUpdates_);
	}

	private void collectJavaScriptUpdate(Writer out) throws IOException {
		WApplication app = this.session_.getApp();
		out.append('{');
		this.collectJS(out);
		this.preLearnStateless(app, out);
		if (this.formObjectsChanged_) {
			String formObjectsList = this.createFormObjectsList(app);
			if (!formObjectsList.equals(this.currentFormObjectsList_)) {
				this.currentFormObjectsList_ = formObjectsList;
				out.append(app.getJavaScriptClass()).append(
						"._p_.setFormObjects([").append(
						this.currentFormObjectsList_).append("]);");
			}
		}
		out.append(app.getAfterLoadJavaScript());
		if (app.isQuited()) {
			out.append(app.getJavaScriptClass()).append("._p_.quit();");
		}
		this.updateLoadIndicator(out, app, false);
		out.append('}');
	}

	private void loadStyleSheets(Writer out, WApplication app)
			throws IOException {
		int first = app.styleSheets_.size() - app.styleSheetsAdded_;
		for (int i = first; i < app.styleSheets_.size(); ++i) {
			out.append("Wt3_1_7").append(".addStyleSheet('").append(
					app.fixRelativeUrl(app.styleSheets_.get(i).uri)).append(
					"', '").append(app.styleSheets_.get(i).media).append(
					"');\n");
		}
		app.styleSheetsAdded_ = 0;
	}

	private void loadScriptLibraries(Writer out, WApplication app, boolean start)
			throws IOException {
		int first = app.scriptLibraries_.size() - app.scriptLibrariesAdded_;
		if (start) {
			for (int i = first; i < app.scriptLibraries_.size(); ++i) {
				String uri = app
						.fixRelativeUrl(app.scriptLibraries_.get(i).uri);
				out.append(app.scriptLibraries_.get(i).beforeLoadJS).append(
						app.getJavaScriptClass()).append("._p_.loadScript('")
						.append(uri).append("',");
				DomElement.jsStringLiteral(out,
						app.scriptLibraries_.get(i).symbol, '\'');
				out.append(");\n");
				out.append(app.getJavaScriptClass()).append("._p_.onJsLoad(\"")
						.append(uri).append("\",function() {\n");
			}
		} else {
			if (app.scriptLibraries_.size() - first > 0) {
				out.append(app.getJavaScriptClass()).append(
						"._p_.autoJavaScript();");
			}
			for (int i = first; i < app.scriptLibraries_.size(); ++i) {
				out.append("});");
			}
			app.scriptLibrariesAdded_ = 0;
		}
	}

	private void updateLoadIndicator(Writer out, WApplication app, boolean all)
			throws IOException {
		if (app.showLoadingIndicator_.needsUpdate(all)) {
			out.append(
					"showLoadingIndicator = function() {var o=null,e=null;\n")
					.append(app.showLoadingIndicator_.getJavaScript()).append(
							"};\n");
			app.showLoadingIndicator_.updateOk();
		}
		if (app.hideLoadingIndicator_.needsUpdate(all)) {
			out.append(
					"hideLoadingIndicator = function() {var o=null,e=null;\n")
					.append(app.hideLoadingIndicator_.getJavaScript()).append(
							"};\n");
			app.hideLoadingIndicator_.updateOk();
		}
	}

	private void setJSSynced(boolean invisibleToo) {
		this.collectedJS1_ = new StringWriter();
		this.collectedJS2_ = new StringWriter();
		if (!invisibleToo) {
			this.collectedJS1_.append(this.invisibleJS_.toString());
		}
		this.invisibleJS_ = new StringWriter();
	}

	private String createFormObjectsList(WApplication app) {
		this.updateFormObjectsList(app);
		String result = "";
		for (Iterator<Map.Entry<String, WObject>> i_it = this.currentFormObjects_
				.entrySet().iterator(); i_it.hasNext();) {
			Map.Entry<String, WObject> i = i_it.next();
			if (result.length() != 0) {
				result += ',';
			}
			result += "'" + i.getKey() + "'";
		}
		this.formObjectsChanged_ = false;
		return result;
	}

	private void preLearnStateless(WApplication app, Writer out)
			throws IOException {
		boolean isIEMobile = app.getEnvironment().agentIsIEMobile();
		if (isIEMobile || !this.session_.getEnv().hasAjax()) {
			return;
		}
		this.collectJS(out);
		Map<String, WeakReference<AbstractEventSignal>> ss = this.session_
				.getApp().exposedSignals();
		for (Iterator<Map.Entry<String, WeakReference<AbstractEventSignal>>> i_it = ss
				.entrySet().iterator(); i_it.hasNext();) {
			Map.Entry<String, WeakReference<AbstractEventSignal>> i = i_it
					.next();
			AbstractEventSignal s = i.getValue().get();
			if (!(s != null)) {
				i_it.remove();
				continue;
			}
			if (s.getSender() == app) {
				s.processPreLearnStateless(this);
			}
			WWidget ww = ((s.getSender()) instanceof WWidget ? (WWidget) (s
					.getSender()) : null);
			if (ww != null && ww.isRendered()) {
				s.processPreLearnStateless(this);
			}
		}
		out.append(this.statelessJS_.toString());
		this.statelessJS_ = new StringWriter();
	}

	private StringWriter collectedJS1_;
	private StringWriter collectedJS2_;
	private StringWriter invisibleJS_;
	private StringWriter statelessJS_;
	StringWriter beforeLoadJS_;

	private void collectJS(Writer js) throws IOException {
		List<DomElement> changes = new ArrayList<DomElement>();
		this.collectChanges(changes);
		WApplication app = this.session_.getApp();
		if (js != null) {
			js.append(app.getNewBeforeLoadJavaScript());
			EscapeOStream sout = new EscapeOStream(js);
			for (int i = 0; i < changes.size(); ++i) {
				changes.get(i).asJavaScript(sout, DomElement.Priority.Delete);
			}
			for (int i = 0; i < changes.size(); ++i) {
				changes.get(i).asJavaScript(sout, DomElement.Priority.Update);
				;
			}
		} else {
			for (int i = 0; i < changes.size(); ++i) {
				;
			}
		}
		if (js != null) {
			if (app.titleChanged_) {
				js.append(app.getJavaScriptClass()).append("._p_.setTitle(")
						.append(
								WString.toWString(app.getTitle())
										.getJsStringLiteral()).append(");\n");
			}
			if (app.closeMessageChanged_) {
				js.append(app.getJavaScriptClass()).append(
						"._p_.setCloseMessage(").append(
						WString.toWString(app.getCloseMessage())
								.getJsStringLiteral()).append(");\n");
			}
		}
		app.titleChanged_ = false;
		app.closeMessageChanged_ = false;
		if (js != null) {
			if (app.internalPathIsChanged_) {
				js.append(app.getJavaScriptClass()).append("._p_.setHash('")
						.append(app.newInternalPath_).append("');\n");
			}
			js.append(app.getAfterLoadJavaScript());
		} else {
			app.getAfterLoadJavaScript();
		}
		app.internalPathIsChanged_ = false;
	}

	private void setPageVars(FileServe page) {
		boolean xhtml = this.session_.getEnv().getContentType() == WEnvironment.ContentType.XHTML1;
		WApplication app = this.session_.getApp();
		page.setVar("DOCTYPE", this.session_.getDocType());
		String htmlAttr = "";
		if (app != null && app.htmlClass_.length() != 0) {
			htmlAttr = " class=\"" + app.htmlClass_ + "\"";
		}
		if (xhtml) {
			page.setVar("HTMLATTRIBUTES",
					"xmlns=\"http://www.w3.org/1999/xhtml\"" + htmlAttr);
			page.setVar("METACLOSE", "/>");
		} else {
			if (this.session_.getEnv().agentIsIE()) {
				page.setVar("HTMLATTRIBUTES",
						"xmlns:v=\"urn:schemas-microsoft-com:vml\" lang=\"en\" dir=\"ltr\""
								+ htmlAttr);
			} else {
				page.setVar("HTMLATTRIBUTES", "lang=\"en\" dir=\"ltr\""
						+ htmlAttr);
			}
			page.setVar("METACLOSE", ">");
		}
		page.setVar("BODYATTRIBUTES", !(app != null)
				|| app.bodyClass_.length() == 0 ? "" : " class=\""
				+ app.bodyClass_ + "\"");
		page.setVar("HEADDECLARATIONS", this.getHeadDeclarations());
		page.setCondition("FORM", !this.session_.getEnv().agentIsSpiderBot()
				&& !this.session_.getEnv().hasAjax());
	}

	private void setBootVars(WebResponse response, FileServe boot) {
		Configuration conf = this.session_.getController().getConfiguration();
		boot.setVar("BLANK_HTML", this.session_.getBootstrapUrl(response,
				WebSession.BootstrapOption.ClearInternalPath)
				+ "&amp;request=resource&amp;resource=blank");
		boot.setVar("SELF_URL", this.safeJsStringLiteral(this.session_
				.getBootstrapUrl(response,
						WebSession.BootstrapOption.KeepInternalPath)));
		boot.setVar("SESSION_ID", this.session_.getSessionId());
		boot.setVar("RANDOMSEED", String.valueOf(MathUtils.randomInt() + 0));
		boot.setVar("RELOAD_IS_NEWSESSION", conf.isReloadIsNewSession());
		boot
				.setVar(
						"USE_COOKIES",
						conf.getSessionTracking() == Configuration.SessionTracking.CookiesURL);
		boot.setVar("AJAX_CANONICAL_URL", this
				.safeJsStringLiteral(this.session_.ajaxCanonicalUrl(response)));
	}

	private String getHeadDeclarations() {
		final boolean xhtml = this.session_.getEnv().getContentType() == WEnvironment.ContentType.XHTML1;
		EscapeOStream result = new EscapeOStream();
		if (this.session_.getApp() != null) {
			for (int i = 0; i < this.session_.getApp().metaHeaders_.size(); ++i) {
				WApplication.MetaHeader m = this.session_.getApp().metaHeaders_
						.get(i);
				result.append("<meta");
				if (m.name.length() != 0) {
					if (m.type == MetaHeaderType.MetaName) {
						result.append(" name=\"");
					} else {
						result.append(" http-equiv=\"");
					}
					result.pushEscape(EscapeOStream.RuleSet.HtmlAttribute);
					result.append(m.name);
					result.popEscape();
					result.append('"');
				}
				if (m.lang.length() != 0) {
					result.append(" lang=\"");
					result.pushEscape(EscapeOStream.RuleSet.HtmlAttribute);
					result.append(m.lang);
					result.popEscape();
					result.append('"');
				}
				result.append(" content=\"");
				result.pushEscape(EscapeOStream.RuleSet.HtmlAttribute);
				result.append(m.content.toString());
				result.popEscape();
				result.append(xhtml ? "\"/>" : "\">");
			}
		} else {
			if (this.session_.getEnv().agentIsIE()
					&& this.session_.getEnv().getAgent().getValue() <= WEnvironment.UserAgent.IE8
							.getValue()) {
				result.append(
						"<meta http-equiv=\"X-UA-Compatible\" content=\"IE=7")
						.append(xhtml ? "\"/>" : "\">").append('\n');
			}
		}
		if (this.session_.getFavicon().length() != 0) {
			result
					.append(
							"<link rel=\"icon\" type=\"image/vnd.microsoft.icon\" href=\"")
					.append(this.session_.getFavicon()).append(
							xhtml ? "\"/>" : "\">");
		}
		return result.toString();
	}

	private Set<WWidget> updateMap_;
	private boolean learning_;
	private boolean learningIncomplete_;
	private boolean moreUpdates_;

	private String safeJsStringLiteral(String value) {
		String s = WWebWidget.jsStringLiteral(value);
		return StringUtils.replace(s, "<", "<'+'");
	}

	public String learn(AbstractEventSignal.LearningListener slot)
			throws IOException {
		if (slot.getType() == SlotType.PreLearnStateless) {
			this.learning_ = true;
		}
		this.learningIncomplete_ = false;
		slot.trigger();
		StringWriter js = new StringWriter();
		this.collectJS(js);
		String result = js.toString();
		if (slot.getType() == SlotType.PreLearnStateless) {
			slot.undoTrigger();
			this.collectJS((Writer) null);
			this.learning_ = false;
		} else {
			this.statelessJS_.append(result);
		}
		if (!this.learningIncomplete_) {
			slot.setJavaScript(result);
		}
		this.collectJS(this.statelessJS_);
		return result;
	}
}
