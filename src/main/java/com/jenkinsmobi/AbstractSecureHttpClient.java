package com.jenkinsmobi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;

public abstract class AbstractSecureHttpClient {
  private Logger log = Logger.getInstance();	
  protected DefaultHttpClient wrappedDefaultHttpClient;
  protected String domainName;
  protected int port;
  protected String protocol;
  protected String fullUrl;
  protected String queryPath;
  protected HttpCredentials credentials;
  protected boolean performAuthentication = true;
  protected HttpUriRequest lastRequest;
  protected boolean connectionAborted = false;
  protected String customUserAgentInfo = null;
  private HttpGet get;
  private HttpPost post;
  private HttpResponse response;
  private static final String userAgent = Configuration.getInstance().getUserAgent();

  public AbstractSecureHttpClient(DefaultHttpClient httpClient, String url,
      HttpCredentials credentials, String customUserAgentExtention) {

    url = makeUrl(url);

    this.wrappedDefaultHttpClient = httpClient;
    if (customUserAgentExtention != null) {
      wrappedDefaultHttpClient.getParams().setParameter(
          CoreProtocolPNames.USER_AGENT,
          getDefaultUserAgent(customUserAgentExtention));
    } else {
      wrappedDefaultHttpClient.getParams().setParameter(
          CoreProtocolPNames.USER_AGENT, getDefaultUserAgent());
    }

    this.credentials = credentials;
    this.fullUrl = url;

    this.port = UrlParser.getPort(url);
    this.protocol = UrlParser.getProtocol(url);
    this.domainName = UrlParser.getDomainName(url);
    this.queryPath = UrlParser.getQueryPath(url);

    if (credentials.getPassword() == null
        || credentials.getPassword().length() == 0) {

      performAuthentication = false;
    }
  }

  public static String makeUrl(String url) {

    if(url.startsWith("http://") || url.startsWith("https://")){
      return url;
    }
    
    if (!Configuration.getInstance().getHudsonHostname().endsWith("/")) {
      url = "/" + url;
    }
    url = url.replace(" ", "%20");

    return Configuration.getInstance().getHudsonHostname() + url;
  }

  public static String getDefaultUserAgent() {
    return userAgent + " " + Configuration.getInstance().getProductName() + "/"
        + Configuration.getInstance().getProductVersion() + " Android/"
        + Configuration.getDeviceSoftwareVersion();
  }

  public static String getDefaultUserAgent(String customUserAgentExtention) {
    return userAgent + " " + Configuration.getInstance().getProductName() + "/"
        + customUserAgentExtention + "/"
        + Configuration.getInstance().getProductVersion() + " Android/"
        + Configuration.getDeviceSoftwareVersion();
  }

  public void abortConnection() {

    if (lastRequest != null) {

      connectionAborted = true;
      lastRequest.abort();
    }
  }

  public HttpResponse execute(HttpUriRequest request)
      throws ClientProtocolException, IOException {
    HttpResponse response = null;
    addProductSpecHeaders(request);
    lastRequest = request;
    log.debug("HTTP-" + request.getMethod() + " " + request.getURI());
    response = wrappedDefaultHttpClient.execute(request);
    log.debug("HTTP-STATUS: " + response.getStatusLine().toString());
    return this.response = response;
  }

  public HttpResponse executeGetQuery(boolean forceRefresh) throws ClientProtocolException,
      IOException {
    return executeGetQuery(forceRefresh, null);
  }

  public HttpResponse executeGetQuery(boolean forceRefresh, Map<String, String> headers)
      throws ClientProtocolException, IOException {
    get = new HttpGet(fullUrl);
    setHeaders(forceRefresh, headers, get);
    return execute(get);
  }

  public HttpResponse executePostQuery() throws ClientProtocolException,
      IOException {
    return execute(new HttpPost(fullUrl));
  }

  public HttpResponse executePostQuery(Map<String, String> parameters)
      throws ClientProtocolException, IOException {
    HttpPost post = new HttpPost(fullUrl);
    setPostParameters(parameters, post);
    return execute(post);
  }

  public HttpResponse executePostQuery(Map<String, String> parameters,
      Map<String, String> headers) throws ClientProtocolException, IOException {
    HttpPost post = new HttpPost(fullUrl);
    setPostHeaders(headers, post);
    setPostParameters(parameters, post);
    return execute(post);
  }

  public HttpResponse executePostQuery(byte[] postData,
      Map<String, String> headers) throws ClientProtocolException, IOException {
    HttpPost post = new HttpPost(fullUrl);
    post.setEntity(new ByteArrayEntity(postData));
    setPostHeaders(headers, post);
    return execute(post);
  }

  protected void addProductSpecHeaders(HttpUriRequest httpMessage) {
    httpMessage.addHeader("agentVersion", Configuration.getInstance()
        .getProductVersion());
    httpMessage.addHeader("agentName", Configuration.getInstance()
        .getProductName());
  }

  public boolean isConnectionAborted() {
    return connectionAborted;
  }

  public void setPostParameters(Map<String, String> parameters, HttpPost post)
      throws UnsupportedEncodingException {
    if (parameters != null && parameters.size() > 0) {
      Iterator<String> keysIter = parameters.keySet().iterator();
      List<NameValuePair> nameValuePairList = new LinkedList<NameValuePair>();
      while (keysIter.hasNext()) {

        String key = keysIter.next();
        String value = parameters.get(key);
        nameValuePairList.add(new BasicNameValuePair(key, value));
      }

      post.setEntity(new UrlEncodedFormEntity(nameValuePairList, "UTF-8"));
    }
  }

  public void setPostHeaders(Map<String, String> headers, HttpPost post)
      throws UnsupportedEncodingException {
    if (headers != null && headers.size() > 0) {
      Iterator<String> keysIter = headers.keySet().iterator();
      while (keysIter.hasNext()) {

        String key = keysIter.next();
        String value = headers.get(key);

        post.setHeader(key, value);
      }
    }

    post.setHeader("Content-Type", "application/x-www-form-urlencoded");
  }

  public void setHeaders(boolean forceRefresh, Map<String, String> headers, HttpRequestBase get)
      throws UnsupportedEncodingException, MalformedURLException {
	  


    if (headers == null) {
      headers = Configuration.getInstance().getRequestHeaders();
    }
    
    	headers.put("Cache-Control", "no-cache");
    
    if (headers != null && headers.size() > 0) {
      Iterator<String> keysIter = headers.keySet().iterator();
      while (keysIter.hasNext()) {

        String key = keysIter.next();
        String value = headers.get(key);

        get.setHeader(key, value);
      }
    }
  }

  public HttpPost getPost() {
    return new HttpPost(fullUrl);
  }

  public void releaseConnection() {
    if (response != null && response.getEntity() != null) {
      try {
        response.getEntity().consumeContent();
      } catch (IOException e) {
        log.error("Cannot consume response content to release HTTP connection",
            e);
      }
    }
  }
}
