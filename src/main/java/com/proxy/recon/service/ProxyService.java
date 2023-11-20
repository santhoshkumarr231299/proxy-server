package com.proxy.recon.service;

import com.proxy.recon.data.LoginData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.brotli.dec.BrotliInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

@Service
@Slf4j
public class ProxyService {
    @Autowired
    LoginData loginData;
    @Value("${server.servlet.session.cookie.name}")
    String authCookieName;
    @Value("${target.url}")
    String targetUrl;
    public ResponseEntity proxyGateWay(HttpServletRequest request) {
        log.info(request.getRequestURI() + " " + request.getMethod());

        String domain = targetUrl;
        String urlString = getUrl(domain, request);
        return proxyConnection(urlString, request);
    }


    public ResponseEntity proxyConnection(String urlString, HttpServletRequest request) {
        HttpURLConnection webProxyConnection = null;
        try {

            URL weburl = new URL(urlString);

            webProxyConnection = (HttpURLConnection) weburl.openConnection();
            webProxyConnection.setConnectTimeout(20000);

            webProxyConnection.setRequestMethod(request.getMethod());

            webProxyConnection.setDoInput(true);
            webProxyConnection.setDoOutput(true);

            if("post".equalsIgnoreCase(request.getMethod())) {
                webProxyConnection.setDoOutput(true);

                OutputStream os = webProxyConnection.getOutputStream();
                String reqData = getjsonData(request);

                byte[] byteData = reqData.getBytes(StandardCharsets.UTF_8);
                os.write(byteData);
                os.close();
            } else if(!"get".equalsIgnoreCase(request.getMethod())) {
                String response = "<h1>" + HttpStatusCode.valueOf(405) + "</h1>";
                return new ResponseEntity<>(response, HttpStatus.valueOf(405));
            }

            Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String headerName = headers.nextElement();
                if(headerName != null) {
                    webProxyConnection.setRequestProperty(headerName, request.getHeader(headerName));
                }
            }


            int responseCode = webProxyConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK ||
                    responseCode == HttpURLConnection.HTTP_CREATED ||
                    responseCode == HttpURLConnection.HTTP_ACCEPTED ||
                    responseCode == HttpURLConnection.HTTP_NO_CONTENT ||
                    responseCode == HttpURLConnection.HTTP_PARTIAL) {

                HttpHeaders httpHeaders = new HttpHeaders();

                Map<String, List<String>> responseHeaders = webProxyConnection.getHeaderFields();

                for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                    String headerName = entry.getKey();
                    List<String> headerValues = entry.getValue();
                    for(String headerValue : headerValues) {
                        if(headerName != null) {
                            httpHeaders.add(headerName, headerValue);
                        }
                    }
                }

                String contentType = webProxyConnection.getContentType();

                if(contentType != null) {
                    httpHeaders.setContentType(MediaType.valueOf(contentType));
                } else {
                    httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                }

                if (contentType != null) {
                   if (contentType.contains("image") && !contentType.contains("svg")) {
                        byte[] imageData = processImageData(webProxyConnection);
                        return new ResponseEntity<>(imageData, httpHeaders, HttpStatus.valueOf(responseCode));
                    } else {
                       String response = processResponseData(webProxyConnection);
                        return new ResponseEntity<>(response, httpHeaders, HttpStatus.valueOf(responseCode));
                    }
                }
            } else if(responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                String newLocation = webProxyConnection.getHeaderField("Location");
                log.info("moved to new location : " + newLocation);
                webProxyConnection.disconnect();
                return proxyConnection(newLocation, request);
            }else if (responseCode == 422 || responseCode == 500) {
                // Handle the 422 Unprocessable Entity response.
                BufferedReader reader = new BufferedReader(new InputStreamReader(webProxyConnection.getErrorStream()));
                String line;
                StringBuilder errorResponse = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    errorResponse.append(line);
                }
                reader.close();
                return new ResponseEntity<>(errorResponse.toString(), HttpStatus.valueOf(responseCode));
            } else {
                log.info("HTTP Request Failed with Response Code: " + responseCode);
                String response = "<h1>" + HttpStatusCode.valueOf(responseCode) + "</h1>";
                return new ResponseEntity<>(response, HttpStatus.valueOf(responseCode));
            }
        } catch (SocketException e) {
            String response = "<h1>" + HttpStatusCode.valueOf(503) + "</h1>";
            return new ResponseEntity<>(response, HttpStatus.valueOf(503));
        } catch (Exception e) {
            log.error("Exception : ", e);
        } finally {
            try {
                if (webProxyConnection != null) {
                    webProxyConnection.disconnect();
                }
            } catch (Exception e) {
                //
            }
        }
        String response = "<h1>" + HttpStatusCode.valueOf(500) + "</h1>";
        return new ResponseEntity<>(response, HttpStatus.valueOf(500));
    }
    public String getUrl(String domain, HttpServletRequest request) {
        String path = request.getRequestURI();
        String params = request.getQueryString();
        if(path != null) {
            domain += path;
        }
        if(params != null) {
            domain += "?" + params;
        }
        return domain;
    }
    public byte[] processImageData(HttpURLConnection webProxyConnection) throws Exception {
        @Cleanup InputStream inputStream = webProxyConnection.getInputStream();
        return inputStream.readAllBytes();
    }
    public String processResponseData(HttpURLConnection webProxyConnection) throws Exception {
        @Cleanup BufferedReader reader = getReaderBasedOnEncoding(webProxyConnection);
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        return response.toString();
    }
    public String getjsonData(HttpServletRequest request) throws Exception {
        StringBuilder jsonRequest = new StringBuilder();
        String line;
        BufferedReader reader = request.getReader();
        while ((line = reader.readLine()) != null) {
            jsonRequest.append(line);
        }
        return jsonRequest.toString();
    }
    public BufferedReader getReaderBasedOnEncoding(HttpURLConnection webProxyConnection) throws Exception {
        String contentEncoding = webProxyConnection.getContentEncoding();
        if("gzip".equals(contentEncoding)) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(webProxyConnection.getInputStream())));
        } else if("br".equals(contentEncoding)) {
            return new BufferedReader(new InputStreamReader(new BrotliInputStream(webProxyConnection.getInputStream())));
        } else if("deflate".equals(contentEncoding)) {
            return new BufferedReader(new InputStreamReader(new InflaterInputStream(webProxyConnection.getInputStream())));
        } else {
            return new BufferedReader(new InputStreamReader(webProxyConnection.getInputStream()));
        }
    }
}
