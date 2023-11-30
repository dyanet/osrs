/*
 * Copyright 2012-2023, Dyanet Inc., Akber A. Choudhry,
 *   and other individual contributors identified by the
 *   @authors tag in each source artefact.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *   either express or implied.
 *   See the License for the specific language governing permissions
 *   and limitations under the License.
 */

package com.dyanet.osrs;

import static com.dyanet.osrs.Config.OSRSCONFIG;
import static com.dyanet.osrs.Config.OSRSDIR;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import com.dyanet.osrs.jaxb.Body;
import com.dyanet.osrs.jaxb.DataBlock;
import com.dyanet.osrs.jaxb.DtAssoc;
import com.dyanet.osrs.jaxb.Header;
import com.dyanet.osrs.jaxb.Item;
import com.dyanet.osrs.jaxb.OPSEnvelope;
import com.dyanet.osrs.jaxb.ObjectFactory;
import com.dyanet.osrs.req.OsrsRequest;
import com.dyanet.osrs.resp.OsrsResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

/**
 * Singleton client that does all the low-level stuff for
 * communications.
 * 
 * @author Akber Choudhry
 */
public class OsrsClient {
    
    /**
     * Singleton instance
     */
    private static OsrsClient instance;
    
    /**
     * Fields
     */
    private static Logger logger = LoggerFactory.getLogger(OsrsClient.class);
    private CloseableHttpClient httpClient;
    private String uri;
    private final String version = "0.9";
    private final String protocol = "XCP";
    private JAXBContext jc = null; // multi-threaded
    XMLReader xmlReader = null;
    private ObjectFactory oj = null; //just factory
    private OsrsResponseFactory orf = new OsrsResponseFactory();
    
    /**
     * Get the singleton instance
     */
    public static OsrsClient getInstance(boolean newHttpClient) {
        if (newHttpClient) {
            instance = null;
        }
        
        if (instance == null) {
            instance = new OsrsClient();
        }
        return instance;
    }

    
    /**
     * This should be the only constructor called
     * It will recreate the httpClient whenever it is called, allowing 
     */
    private OsrsClient() {
        logger.info("New OsrsClient instance being created");
        this.httpClient = HttpClientBuilder.create()
            .setMaxConnTotal(100)
            .setDefaultRequestConfig(getRequestConfig())
        .build();

        this.uri = "https://" + OsrsConfig.getValue("osrs.host") + ":" + OsrsConfig.getValue("osrs.sslPort") + "/";
        initJaxb();
    }

    /**
     * Create RequestConfig from configuration
     * @return RequestConfig
     */
    private RequestConfig getRequestConfig() {
      RequestConfig reqConfig = RequestConfig.custom()
          .setSocketTimeout(60000)
          .setConnectTimeout(60000)
      .build();
      return reqConfig;
    }

    private String bytesToHex(byte[] bytes) {
        String s = new String();
        BigInteger bi = new BigInteger(1, bytes);
        // Format to hexadecimal
        s = bi.toString(16); // 120ff0
        if (s.length() % 2 != 0) {
            // Pad with 0
            s = "0" + s;
        }
        return s;
    }

    private String getSignature(String xml) {
        String signature = new String();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            String concat = xml + OsrsConfig.getValue("osrs.key");
            concat = bytesToHex(md5.digest(concat.getBytes()));
            signature = bytesToHex(md5.digest(concat.concat(
                    OsrsConfig.getValue("osrs.key")).getBytes()));
        } catch (Exception ex) {
            logger.warn("Error generating signature: ", ex);
        }
        return signature;
    }

    /**
     * Raw send receive - one of two methods that executes network operations
     * 
     * @param HttpPost
     * @return String
     * @throws OsrsException
     */
    protected String sendReceive(HttpPost post) throws OsrsException {

        try {
            HttpResponse response = httpClient.execute(post);
            HttpEntity entity = response.getEntity();
            String ret =  EntityUtils.toString(entity);
            EntityUtils.consume(entity);
            return ret;
        } catch (Exception e) {
            throw new OsrsException("Error on base sendReceive", e);
        }

    }

    public OsrsResponse sendReceive(OsrsRequest request) throws OsrsException {

        String ret = sendReceive(getString(request));
        OPSEnvelope resp = createEnvelope(ret);
        OsrsResponse response = orf.createResponse(request, resp);
            return response;
    }
    
    public String sendReceive(String xml) throws OsrsException {

        String signature = getSignature(xml);

        HttpPost postRequest = new HttpPost(uri);
        postRequest.addHeader("Content-Type", "text/xml");
        postRequest.addHeader("X-Signature", signature);
        postRequest.addHeader("X-Username", OsrsConfig.getValue("osrs.userName"));

        try {
            postRequest.setEntity(new StringEntity(xml));
            String response = sendReceive(postRequest);
            return response;
        } catch (Exception ex) {
            throw new OsrsException("Sending post got exception ", ex);
        }
    }

    protected OPSEnvelope createEnvelope(String ret) throws OsrsException {
        OPSEnvelope opsEnvelope = null;
        try {

            InputSource inputSource = new InputSource(new StringReader(ret));
            SAXSource source = new SAXSource(xmlReader, inputSource);
            Unmarshaller u = jc.createUnmarshaller();
            opsEnvelope = (OPSEnvelope)u.unmarshal(source);
        } catch (Exception e) {
            throw new OsrsException("Error parsing response: " + ret, e);
        }
        return opsEnvelope;
    }

    protected OPSEnvelope getEmptyEnvelope() throws OsrsException {
        initJaxb();
        OPSEnvelope opsEnvelope = null;

            opsEnvelope = oj.createOPSEnvelope();
            Header header = oj.createHeader();
            header.setVersion(this.version);
            opsEnvelope.setHeader(header);
            
            DataBlock dBlock = oj.createDataBlock();
            
            Body body = oj.createBody();
            body.setDataBlock(dBlock);
            
            opsEnvelope.setBody(body);
        return opsEnvelope;
    }
    
    private void initJaxb() throws OsrsException {
        if (jc != null && oj != null) return;
        try {
            this.jc = JAXBContext.newInstance("com.dyanet.osrs.jaxb");
            this.oj = new ObjectFactory();
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            spf.setNamespaceAware(true);
            spf.setValidating(false);
            xmlReader = spf.newSAXParser().getXMLReader();
            xmlReader.setEntityResolver(new EntityResolver() {    
                @Override
                 public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    logger.debug("Ignoring DTD");
                    return new InputSource(new StringReader(""));
                }
            });            
        } catch (Exception e) {
            throw new OsrsException("JAXB Error", e);
        }
    }
    
    protected String getString(OsrsRequest request) throws OsrsException {
        OPSEnvelope envelope = getEmptyEnvelope();
        Map<String, Object> attrs = request.getAttributes();

        Item pro = oj.createItem(); 
        pro.setKey("protocol");
        pro.setStringValue(this.protocol);
        Item obj = oj.createItem(); 
        obj.setKey("object");
        obj.setStringValue(request.getObject());
        Item act = oj.createItem();
        act.setKey("action");
        act.setStringValue(request.getAction());
        Item att = oj.createItem();
        att.setKey("attributes");
        att.addDtAssoc(processAttrs(attrs));
        
        DtAssoc c = oj.createDtAssoc();
        c.addItem(pro);
        c.addItem(act);
        c.addItem(obj);
        c.addItem(att);
        ((List<Object>)envelope.getBody().getDataBlock()
            .getDtAass()).add(c);

        
        try {
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            m.setProperty("org.glassfish.jaxb.xmlHeaders", 
                "<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n" +
                "<!DOCTYPE OPS_envelope SYSTEM '" + OSRSDIR + OSRSCONFIG + "ops.dtd'>");
            //StringWriter writer = new StringWriter();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.marshal(envelope, baos);

            return baos.toString();
        } catch (Exception e) {
            throw new OsrsException("Error creating envelope from request", e);
        }
    }    
    

    private DtAssoc processAttrs(Map<String, Object> attrs) {
        DtAssoc dt = oj.createDtAssoc();
        for (Iterator<String> iter = attrs.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            if (attrs.get(key) instanceof String) {
                Item one = oj.createItem();
                one.setKey(key);
                one.setStringValue((String)attrs.get(key));
                dt.addItem(one);
            }
        }
        return dt;
    }
}