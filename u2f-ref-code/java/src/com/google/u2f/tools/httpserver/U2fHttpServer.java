// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.tools.httpserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import com.google.common.collect.ImmutableSet;
import com.google.u2f.server.ChallengeGenerator;
import com.google.u2f.server.DataStore;
import com.google.u2f.server.SessionIdGenerator;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.impl.BouncyCastleCrypto;
import com.google.u2f.server.impl.MemoryDataStore;
import com.google.u2f.server.impl.U2FServerReferenceImpl;
import com.google.u2f.tools.httpserver.servlets.EnrollDataServlet;
import com.google.u2f.tools.httpserver.servlets.EnrollFinishServlet;
import com.google.u2f.tools.httpserver.servlets.EnrollRequestServlet;
import com.google.u2f.tools.httpserver.servlets.RequestDispatcher;
import com.google.u2f.tools.httpserver.servlets.SignDataServlet;
import com.google.u2f.tools.httpserver.servlets.SignFinishServlet;
import com.google.u2f.tools.httpserver.servlets.SignRequestServlet;
import com.google.u2f.tools.httpserver.servlets.StaticHandler;

public class U2fHttpServer {
  private final static Logger Log = Logger.getLogger(U2fHttpServer.class.getSimpleName());

  private final Object lock = new Object();
  private final U2FServer u2fServer;

  private long sessionIdCounter = 0;

  public static void main(String[] args) throws InterruptedException {
    new U2fHttpServer();
  }

  public U2fHttpServer() {
    ChallengeGenerator challengeGenerator = new ChallengeGenerator() {
      @Override
      public byte[] generateChallenge(String accountName) {
        try {
          return Hex.decodeHex("1234".toCharArray());
        } catch (DecoderException e) {
          throw new RuntimeException(e);
        }
      }
    };

    SessionIdGenerator sessionIdGenerator = new SessionIdGenerator() {
      @Override
      public String generateSessionId(String accountName) {
        return new StringBuilder()
          .append("sessionId_")
          .append(sessionIdCounter++)
          .append("_")
          .append(accountName)
          .toString();
      }
    };

    X509Certificate trustedCertificate;
    try {
      trustedCertificate = (X509Certificate) CertificateFactory.getInstance("X.509")
          .generateCertificate(new ByteArrayInputStream(Hex.decodeHex((
        		  "308201D230820177A00302010202090088A59EE4B363E329300A06082A8648CE"
        				  + "3D0403023045310B30090603550406130241553113301106035504080C0A536F"
        				  + "6D652D53746174653121301F060355040A0C18496E7465726E65742057696467"
        				  + "69747320507479204C7464301E170D3137303132343133343135365A170D3237"
        				  + "303132353133343135365A3045310B3009060355040613024155311330110603"
        				  + "5504080C0A536F6D652D53746174653121301F060355040A0C18496E7465726E"
        				  + "6574205769646769747320507479204C74643059301306072A8648CE3D020106"
        				  + "082A8648CE3D0301070342000465DFB127BCDC2D8E323263DF34817D4BD9F9B3"
        				  + "09E63149432F6374917B66A721C3FD3E728924F88764D02CCAF21E5D72631372"
        				  + "A55F04BB6BA9D2F7402F494589A350304E301D0603551D0E04160414ADE7F364"
        				  + "E16B13A3CF2814256EAB47CAD1227B10301F0603551D23041830168014ADE7F3"
        				  + "64E16B13A3CF2814256EAB47CAD1227B10300C0603551D13040530030101FF30"
        				  + "0A06082A8648CE3D0403020349003046022100B77BF208A8E94BEF652763BB48"
        				  + "1C658A3E1F14D76D234B57E5A36DF6600E5FCE022100CF7B37BEB97B466877D3"
        				  + "82289B2EF143C8D27855FE9098F7511C4ED687C52657").toCharArray())));
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    } catch (DecoderException e) {
      throw new RuntimeException(e);
    }
    DataStore dataStore = new MemoryDataStore(sessionIdGenerator);
    dataStore.addTrustedCertificate(trustedCertificate);
    
    // this implementation will only accept signatures from http://localhost:8080
    u2fServer = new U2FServerReferenceImpl(challengeGenerator, dataStore,
        new BouncyCastleCrypto(), ImmutableSet.of("http://localhost:9901", "ios:bundle-id:me.id.wallet.sandbox"));
    Container dispatchContainer = new RequestDispatcher()
        .registerContainer("/", new StaticHandler("text/html","html/index.html"))
        .registerContainer("/enroll", new StaticHandler("text/html","html/enroll.html"))
        .registerContainer("/sign", new StaticHandler("text/html","html/sign.html"))
        
        .registerContainer("/enrollData.js", new EnrollDataServlet(u2fServer))
        .registerContainer("/enrollFinish", new EnrollFinishServlet(u2fServer))
        .registerContainer("/enrollRequest", new EnrollRequestServlet(u2fServer))

        .registerContainer("/signData.js", new SignDataServlet(u2fServer))
        .registerContainer("/signFinish", new SignFinishServlet(u2fServer))
    	.registerContainer("/signRequest", new SignRequestServlet(u2fServer));

    try {
      Connection connection = new SocketConnection(new ContainerServer(dispatchContainer));

      try {
        connection.connect(new InetSocketAddress("0.0.0.0", 9901));

        synchronized (lock) {
          lock.wait();
        }
      } finally {
        connection.close();
      }
    } catch (IOException e) {
      Log.severe("Error with HTTP server: " + e);
      return;
    } catch (InterruptedException e) {
      Log.info("Interrupted");
      return;
    }
  }
}
