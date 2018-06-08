package com.abin.lee.elasticjob.common;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.codecs.DefaultHttpRequestWriterFactory;
import org.apache.http.impl.nio.codecs.DefaultHttpResponseParser;
import org.apache.http.impl.nio.codecs.DefaultHttpResponseParserFactory;
import org.apache.http.impl.nio.conn.ManagedNHttpClientConnectionFactory;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.LineParser;
import org.apache.http.nio.NHttpMessageParser;
import org.apache.http.nio.NHttpMessageParserFactory;
import org.apache.http.nio.NHttpMessageWriterFactory;
import org.apache.http.nio.conn.ManagedNHttpClientConnection;
import org.apache.http.nio.conn.NHttpConnectionFactory;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.*;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: abin
 * Date: 16-4-18
 * Time: 上午10:24
 * To change this template use File | Settings | File Templates.
 * http://hc.apache.org/httpcomponents-asyncclient-dev/httpasyncclient/examples/org/apache/http/examples/nio/client/AsyncClientConfiguration.java
 */
public class AsyncHttpClientUtil {
    private static CloseableHttpAsyncClient httpAsyncClient = null;

    static {
        httpAsyncClient = getHttpAsyncClient();
    }

    private AsyncHttpClientUtil(){}

    private static class AsyncHttpClientUtilHolder{
        private static AsyncHttpClientUtil instance = new AsyncHttpClientUtil();
    }

    public static AsyncHttpClientUtil getInstance(){
        return AsyncHttpClientUtilHolder.instance;
    }


    public static CloseableHttpAsyncClient getHttpAsyncClient() {
        try {
            // Create an HttpClient with the given custom dependencies and configuration.
            httpAsyncClient = HttpAsyncClients.custom()
                    .setConnectionManager(getConnManager())
                    .setDefaultCookieStore(getCookieStore())
                    .setDefaultCredentialsProvider(getCredentialsProvider())
//                    .setProxy(new HttpHost("localhost", 8889))
                    .setDefaultRequestConfig(getDefaultRequestConfig())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return httpAsyncClient;
    }


    protected static PoolingNHttpClientConnectionManager getConnManager() throws IOReactorException {
        // Create a connection manager with custom configuration.
        PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(getIoReactor(), getConnFactory(), getSessionStrategyRegistry(), getDnsResolver());
        // Configure the connection manager to use connection configuration either
        // by default or for a specific host.
        connManager.setDefaultConnectionConfig(getConnectionConfig());
//        connManager.setConnectionConfig(new HttpHost("somehost", 80), ConnectionConfig.DEFAULT);

        // Configure total max or per route limits for persistent connections
        // that can be kept in the pool or leased by the connection manager.
        connManager.setMaxTotal(500);
        connManager.setDefaultMaxPerRoute(100);
//        connManager.setMaxPerRoute(new HttpRoute(new HttpHost("somehost", 80)), 20);

        return connManager;
    }

    protected static ConnectingIOReactor getIoReactor() throws IOReactorException {
        // Create a custom I/O reactort
        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(getIoReactorConfig());
        return ioReactor;
    }

    protected static IOReactorConfig getIoReactorConfig() throws IOReactorException {
        // Create I/O reactor configuration
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setIoThreadCount(Runtime.getRuntime().availableProcessors())
                .setConnectTimeout(30000)
                .setSoTimeout(30000)
                .build();
        return ioReactorConfig;
    }


    protected static NHttpConnectionFactory<ManagedNHttpClientConnection> getConnFactory() throws IOReactorException {
        // Use a custom connection factory to customize the process of
        // initialization of outgoing HTTP connections. Beside standard connection
        // configuration parameters HTTP connection factory can define message
        // parser / writer routines to be employed by individual connections.
        NHttpConnectionFactory<ManagedNHttpClientConnection> connFactory = new ManagedNHttpClientConnectionFactory(
                getRequestWriterFactory(), getResponseParserFactory(), HeapByteBufferAllocator.INSTANCE);
        return connFactory;
    }

    protected static CookieStore getCookieStore() throws IOReactorException {
        // Use custom cookie store if necessary.
        CookieStore cookieStore = new BasicCookieStore();
        return cookieStore;
    }

    protected static CredentialsProvider getCredentialsProvider() throws IOReactorException {
        // Use custom credentials provider if necessary.
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope("localhost", 8889), new UsernamePasswordCredentials("squid", "nopassword"));
        return credentialsProvider;
    }

    protected static RequestConfig getDefaultRequestConfig() throws IOReactorException {
        // Create global request configuration
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.DEFAULT)
                .setExpectContinueEnabled(true)
                .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
                .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
                .build();
        return defaultRequestConfig;
    }

    protected static Registry<SchemeIOSessionStrategy> getSessionStrategyRegistry() throws IOReactorException {

        // Create a registry of custom connection session strategies for supported
        // protocol schemes.
        Registry<SchemeIOSessionStrategy> sessionStrategyRegistry = RegistryBuilder.<SchemeIOSessionStrategy>create()
                .register("http", NoopIOSessionStrategy.INSTANCE)
                .register("https", new SSLIOSessionStrategy(getSslcontext(), getHostnameVerifier()))
                .build();
        return sessionStrategyRegistry;
    }

    protected static DnsResolver getDnsResolver() throws IOReactorException {

        // Use custom DNS resolver to override the system DNS resolution.
        DnsResolver dnsResolver = new SystemDefaultDnsResolver() {

            @Override
            public InetAddress[] resolve(final String host) throws UnknownHostException {
                if (host.equalsIgnoreCase("myhost")) {
                    return new InetAddress[] { InetAddress.getByAddress(new byte[] {127, 0, 0, 1}) };
                } else {
                    return super.resolve(host);
                }
            }

        };
        return dnsResolver;
    }


    protected static NHttpMessageWriterFactory<HttpRequest> getRequestWriterFactory() throws IOReactorException {
        NHttpMessageWriterFactory<HttpRequest> requestWriterFactory = new DefaultHttpRequestWriterFactory();
        return requestWriterFactory;
    }

    protected static NHttpMessageParserFactory<HttpResponse> getResponseParserFactory() throws IOReactorException {

        // Use custom message parser / writer to customize the way HTTP
        // messages are parsed from and written out to the data stream.
        NHttpMessageParserFactory<HttpResponse> responseParserFactory = new DefaultHttpResponseParserFactory() {

            @Override
            public NHttpMessageParser<HttpResponse> create(
                    final SessionInputBuffer buffer,
                    final MessageConstraints constraints) {
                LineParser lineParser = new BasicLineParser() {

                    @Override
                    public Header parseHeader(final CharArrayBuffer buffer) {
                        try {
                            return super.parseHeader(buffer);
                        } catch (ParseException ex) {
                            return new BasicHeader(buffer.toString(), null);
                        }
                    }

                };
                return new DefaultHttpResponseParser(
                        buffer, lineParser, DefaultHttpResponseFactory.INSTANCE, constraints);
            }

        };
        return responseParserFactory;
    }


    protected static SSLContext getSslcontext() throws IOReactorException {
        // SSL context for secure connections can be created either based on
        // system or application specific properties.
        SSLContext sslcontext = SSLContexts.createSystemDefault();
        return sslcontext;
    }


    protected static HostnameVerifier getHostnameVerifier() throws IOReactorException {

        // Use custom hostname verifier to customize SSL hostname verification.
        HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();
        return hostnameVerifier;
    }


    protected static ConnectionConfig getConnectionConfig() throws IOReactorException {
        // Create connection configuration
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setMalformedInputAction(CodingErrorAction.IGNORE)
                .setUnmappableInputAction(CodingErrorAction.IGNORE)
                .setCharset(Consts.UTF_8)
                .setMessageConstraints(getMessageConstraints())
                .build();
        return connectionConfig;
    }


    protected static MessageConstraints getMessageConstraints() throws IOReactorException {
        // Create message constraints
        MessageConstraints messageConstraints = MessageConstraints.custom()
                .setMaxHeaderCount(200)
                .setMaxLineLength(2000)
                .build();
        return messageConstraints;
    }


















    public static String httpPost(Map<String, String> request, String httpUrl){
        String result = "";
        CloseableHttpAsyncClient httpAsyncClient = getHttpAsyncClient();
        httpAsyncClient.start();
        try {
            if(MapUtils.isEmpty(request))
                throw new Exception("请求参数不能为空");
            HttpPost httpPost = new HttpPost(httpUrl);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for(Iterator<Map.Entry<String, String>> iterator = request.entrySet().iterator(); iterator.hasNext();){
                Map.Entry<String, String> entry = iterator.next();
                nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
            Future<HttpResponse> future = httpAsyncClient.execute(httpPost, null);
            HttpResponse response = future.get();
            result = EntityUtils.toString(response.getEntity());
            System.out.println("Executing response: "+ result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                httpAsyncClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String httpPost(String json, String httpUrl, Map<String, String> headers){
        String result = "";
        CloseableHttpAsyncClient httpAsyncClient = getHttpAsyncClient();
        httpAsyncClient.start();
        try {
            if(StringUtils.isBlank(json))
                throw new Exception("请求参数不能为空");
            HttpPost httpPost = new HttpPost(httpUrl);
            for(Iterator<Map.Entry<String, String>> iterator=headers.entrySet().iterator();iterator.hasNext();){
                Map.Entry<String, String> entry = iterator.next();
                Header header = new BasicHeader(entry.getKey(), entry.getValue());
                httpPost.setHeader(header);
            }
            httpPost.setEntity(new StringEntity(json, Charset.forName("UTF-8")));
            System.out.println("Executing request: " + httpPost.getRequestLine());
            Future<HttpResponse> future = httpAsyncClient.execute(httpPost, null);
            HttpResponse response = future.get();
            result = EntityUtils.toString(response.getEntity());
            System.out.println("Executing response: "+ result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                httpAsyncClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String httpGet(String httpUrl, Map<String, String> headers) {
        String result = "";
        CloseableHttpAsyncClient httpAsyncClient = getHttpAsyncClient();
        httpAsyncClient.start();
        try {
            HttpGet httpGet = new HttpGet(httpUrl);
            System.out.println("Executing request: " + httpGet.getRequestLine());
            for(Iterator<Map.Entry<String, String>> iterator=headers.entrySet().iterator();iterator.hasNext();){
                Map.Entry<String, String> entry = iterator.next();
                Header header = new BasicHeader(entry.getKey(), entry.getValue());
                httpGet.setHeader(header);
            }
            Future<HttpResponse> future = httpAsyncClient.execute(httpGet, null);
            HttpResponse response = future.get();
            result = EntityUtils.toString(response.getEntity());
            System.out.println("Executing response: "+ result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                httpAsyncClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    public static String httpGet(String httpUrl) {
        String result = "";
        CloseableHttpAsyncClient httpAsyncClient = getHttpAsyncClient();
        httpAsyncClient.start();
        try {
            HttpGet httpGet = new HttpGet(httpUrl);
            System.out.println("Executing request: " + httpGet.getRequestLine());
            Future<HttpResponse> future = httpAsyncClient.execute(httpGet, null);
            HttpResponse response = future.get();
            result = EntityUtils.toString(response.getEntity());
            System.out.println("Executing response: "+ result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                httpAsyncClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    public static void main(String[] args) {
//        String httpPostUrl = "http://172.16.2.145:9000/trend/find";
//        Map<String, String> request = new HashMap<String, String>();
//        request.put("id", "1");
//        String result = httpPost(request, httpPostUrl);
//        System.out.println("Executing result: "+ result);
//
//        String httpGetUrl = "http://172.16.2.145:9000/trend/find?id=1";
//        String httpGetUrl = "http://172.16.2.146:9500/sayHello";
//        String resultGet = httpGet(httpGetUrl);
//        System.out.println("Executing resultGet: "+ resultGet);

        String httpPostUrl1 = "http://python.loan.com/rules/risk_tip";
        Map<String, String> request1 = new HashMap<String, String>();
        request1.put("Cookie", "rules_session_id=64b713c52c7511e6a4519801a7928995");
        request1.put("RRDSource", "haohuan");
        String input = "{\"uid\":\"1\",\"bank_cards\":[\"622123412341234\"],\"mobiles\":[\"15088741234\"],\"name\":\"樊令爱\",\"id_card\":\"320322197308222517\",\"ips\":[\"222.222.222.222\"]}";
        String result1 = AsyncHttpClientUtil.getInstance().httpPost(input, httpPostUrl1,request1);
        System.out.println("Executing result1: "+ result1);

    }


}
