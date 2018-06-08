package com.abin.lee.elasticjob.common;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: abin
 * Date: 16-4-18
 * Time: 上午10:24
 * To change this template use File | Settings | File Templates.
 */
public class HttpClientUtil {
    private static CloseableHttpClient httpsClient = null;
    private static CloseableHttpClient httpClient = null;

    static {
        httpClient = getHttpClient();
        httpsClient = getHttpsClient();
    }


    private HttpClientUtil(){}

    private static class HttpClientUtilHolder{
        private static HttpClientUtil instance = new HttpClientUtil();
    }

    public static HttpClientUtil getInstance(){
        return HttpClientUtilHolder.instance;
    }


    public static CloseableHttpClient getHttpClient() {
        try {
            httpClient = HttpClients.custom()
                    .setConnectionManager(PoolManager.getHttpPoolInstance())
                    .setConnectionManagerShared(true)
                    .setDefaultRequestConfig(requestConfig())
                    .setRetryHandler(retryHandler())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return httpClient;
    }


    public static CloseableHttpClient getHttpsClient() {
        try {
            //Secure Protocol implementation.
            SSLContext ctx = SSLContext.getInstance("SSL");
            //Implementation of a trust manager for X509 certificates
            TrustManager x509TrustManager = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs,
                                               String string) throws CertificateException {
                }
                public void checkServerTrusted(X509Certificate[] xcs,
                                               String string) throws CertificateException {
                }
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[]{x509TrustManager}, null);
            //首先设置全局的标准cookie策略
//            RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD_STRICT).build();
            ConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(ctx, hostnameVerifier);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", connectionSocketFactory).build();
            // 设置连接池
            httpsClient = HttpClients.custom()
                    .setConnectionManager(PoolsManager.getHttpsPoolInstance(socketFactoryRegistry))
                    .setConnectionManagerShared(true)
                    .setDefaultRequestConfig(requestConfig())
                    .setRetryHandler(retryHandler())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return httpsClient;
    }

    // 配置请求的超时设置
    //首先设置全局的标准cookie策略
    public static RequestConfig requestConfig(){
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD_STRICT)
                .setConnectionRequestTimeout(60000)
                .setConnectTimeout(60000)
                .setSocketTimeout(60000)
                .build();
        return requestConfig;
    }

    public static HttpRequestRetryHandler retryHandler(){
        //请求重试处理
        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception,int executionCount, HttpContext context) {
                if (executionCount >= 5) {// 如果已经重试了5次，就放弃
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                    return false;
                }
                if (exception instanceof InterruptedIOException) {// 超时
                    return false;
                }
                if (exception instanceof UnknownHostException) {// 目标服务器不可达
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
                    return false;
                }
                if (exception instanceof SSLException) {// ssl握手异常
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };
        return httpRequestRetryHandler;
    }



    //创建HostnameVerifier
    //用于解决javax.net.ssl.SSLException: hostname in certificate didn't match: <123.125.97.66> != <123.125.97.241>
    static HostnameVerifier hostnameVerifier = new NoopHostnameVerifier(){
        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return super.verify(s, sslSession);
        }
    };


    public static class PoolManager {
        public static PoolingHttpClientConnectionManager clientConnectionManager = null;
        private static int maxTotal = 200;
        private static int defaultMaxPerRoute = 100;

        private PoolManager(){
            clientConnectionManager.setMaxTotal(maxTotal);
            clientConnectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
        }

        private static class PoolManagerHolder{
            public static  PoolManager instance = new PoolManager();
        }

        public static PoolManager getInstance() {
            if(null == clientConnectionManager)
                clientConnectionManager = new PoolingHttpClientConnectionManager();
            return PoolManagerHolder.instance;
        }

        public static PoolingHttpClientConnectionManager getHttpPoolInstance() {
            PoolManager.getInstance();
//            System.out.println("getAvailable=" + clientConnectionManager.getTotalStats().getAvailable());
//            System.out.println("getLeased=" + clientConnectionManager.getTotalStats().getLeased());
//            System.out.println("getMax=" + clientConnectionManager.getTotalStats().getMax());
//            System.out.println("getPending="+clientConnectionManager.getTotalStats().getPending());
            return PoolManager.clientConnectionManager;
        }


    }

    public static class PoolsManager {
        public static PoolingHttpClientConnectionManager clientConnectionManager = null;
        private static int maxTotal = 200;
        private static int defaultMaxPerRoute = 100;

        private PoolsManager(){
            clientConnectionManager.setMaxTotal(maxTotal);
            clientConnectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
        }

        private static class PoolsManagerHolder{
            public static  PoolsManager instance = new PoolsManager();
        }

        public static PoolsManager getInstance(Registry<ConnectionSocketFactory> socketFactoryRegistry) {
            if(null == clientConnectionManager)
                clientConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            return PoolsManagerHolder.instance;
        }

        public static PoolingHttpClientConnectionManager getHttpsPoolInstance(Registry<ConnectionSocketFactory> socketFactoryRegistry) {
            PoolsManager.getInstance(socketFactoryRegistry);
//            System.out.println("getAvailable=" + clientConnectionManager.getTotalStats().getAvailable());
//            System.out.println("getLeased=" + clientConnectionManager.getTotalStats().getLeased());
//            System.out.println("getMax=" + clientConnectionManager.getTotalStats().getMax());
//            System.out.println("getPending="+clientConnectionManager.getTotalStats().getPending());
            return PoolsManager.clientConnectionManager;
        }

    }

    public static String httpPost(Map<String, String> request, String httpUrl){
        String result = "";
        CloseableHttpClient httpClient = getHttpClient();
        try {
            if(MapUtils.isEmpty(request))
                throw new Exception("请求参数不能为空");
            HttpPost httpPost = new HttpPost(httpUrl);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for(Iterator<Map.Entry<String, String>> iterator=request.entrySet().iterator(); iterator.hasNext();){
                Map.Entry<String, String> entry = iterator.next();
                nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
            System.out.println("Executing request: " + httpPost.getRequestLine());
            CloseableHttpResponse response = httpClient.execute(httpPost);
            result = EntityUtils.toString(response.getEntity());
            System.out.println("Executing response: "+ result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String httpPost(String json, String httpUrl, Map<String, String> headers){
        String result = "";
        CloseableHttpClient httpClient = getHttpClient();
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
            CloseableHttpResponse response = httpClient.execute(httpPost);
            result = EntityUtils.toString(response.getEntity());
            System.out.println("Executing response: "+ result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String httpGet(String httpUrl, Map<String, String> headers) {
        String result = "";
        CloseableHttpClient httpClient = getHttpClient();
        try {
            HttpGet httpGet = new HttpGet(httpUrl);
            System.out.println("Executing request: " + httpGet.getRequestLine());
            for(Iterator<Map.Entry<String, String>> iterator=headers.entrySet().iterator();iterator.hasNext();){
                Map.Entry<String, String> entry = iterator.next();
                Header header = new BasicHeader(entry.getKey(), entry.getValue());
                httpGet.setHeader(header);
            }
            CloseableHttpResponse response = httpClient.execute(httpGet);
            result = EntityUtils.toString(response.getEntity());
            System.out.println("Executing response: "+ result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    public static String httpGet(String httpUrl) {
        String result = "";
        CloseableHttpClient httpClient = getHttpClient();
        try {
            HttpGet httpGet = new HttpGet(httpUrl);
            System.out.println("Executing request: " + httpGet.getRequestLine());
            CloseableHttpResponse response = httpClient.execute(httpGet);
            result = EntityUtils.toString(response.getEntity());
            System.out.println("Executing response: "+ result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                httpClient.close();
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
//        String resultGet = httpGet(httpGetUrl);
//        System.out.println("Executing resultGet: "+ resultGet);

        String httpPostUrl1 = "http://python.loan.com/rules/risk_tip";
        Map<String, String> request1 = new HashMap<String, String>();
        request1.put("Cookie", "rules_session_id=64b713c52c7511e6a4519801a7928995");
        request1.put("RRDSource", "haohuan");
        String input = "{\"uid\":\"1\",\"bank_cards\":[\"622123412341234\"],\"mobiles\":[\"15088741234\"],\"name\":\"樊令爱\",\"id_card\":\"320322197308222517\",\"ips\":[\"222.222.222.222\"]}";
        String result1 = httpPost(input, httpPostUrl1,request1);
        System.out.println("Executing result1: "+ result1);

    }

}
