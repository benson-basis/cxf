/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.sts.token.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.jwt.JWTTokenProvider;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

/**
 * Some unit tests for creating JWT Tokens via the JWTTokenProvider in different realms
 */
public class JWTTokenProviderRealmTest extends org.junit.Assert {
    
    @org.junit.Test
    public void testRealms() throws Exception {
        TokenProvider jwtTokenProvider = new JWTTokenProvider();
        TokenProviderParameters providerParameters = createProviderParameters(JWTTokenProvider.JWT_TOKEN_TYPE);
        providerParameters.setRealm("A");
        
        // Create Realms
        Map<String, RealmProperties> jwtRealms = new HashMap<String, RealmProperties>();
        RealmProperties jwtRealm = new RealmProperties();
        jwtRealm.setIssuer("A-Issuer");
        jwtRealms.put("A", jwtRealm);
        jwtRealm = new RealmProperties();
        jwtRealm.setIssuer("B-Issuer");
        jwtRealms.put("B", jwtRealm);
        ((JWTTokenProvider)jwtTokenProvider).setRealmMap(jwtRealms);
        
        // Realm "A"
        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE, "A"));
        TokenProviderResponse providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        
        String token = (String)providerResponse.getToken();
        assertNotNull(token);
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        
        assertEquals(jwt.getClaim(JwtConstants.CLAIM_ISSUER), "A-Issuer");
        
        // Realm "B"
        providerParameters.setRealm("B");
        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE, "B"));
        providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        
        token = (String)providerResponse.getToken();
        assertNotNull(token);
        jwtConsumer = new JwsJwtCompactConsumer(token);
        jwt = jwtConsumer.getJwtToken();
        
        assertEquals(jwt.getClaim(JwtConstants.CLAIM_ISSUER), "B-Issuer");
        
        // Default Realm
        providerParameters.setRealm(null);
        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE, null));
        providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        
        token = (String)providerResponse.getToken();
        assertNotNull(token);
        jwtConsumer = new JwsJwtCompactConsumer(token);
        jwt = jwtConsumer.getJwtToken();
        
        assertEquals(jwt.getClaim(JwtConstants.CLAIM_ISSUER), "STS");
    }
    
    private TokenProviderParameters createProviderParameters(
        String tokenType
    ) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        parameters.setKeyRequirements(keyRequirements);

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        parameters.setWebServiceContext(webServiceContext);

        parameters.setAppliesToAddress("http://dummy-service.com/dummy");

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);
        
        parameters.setEncryptionProperties(new EncryptionProperties());

        return parameters;
    }
    
    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "stsstore.jks");
        
        return properties;
    }
    
}
