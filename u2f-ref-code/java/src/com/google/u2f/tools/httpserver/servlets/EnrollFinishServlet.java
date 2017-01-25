// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.tools.httpserver.servlets;

import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import com.google.gson.JsonObject;
import com.google.u2f.U2FException;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.messages.RegistrationResponse;

public class EnrollFinishServlet extends JavascriptServlet {

  private final U2FServer u2fServer;

  public EnrollFinishServlet(U2FServer u2fServer) {
    this.u2fServer = u2fServer;
  }

  @Override
  public void generateJavascript(Request req, Response resp, PrintStream body) {
    RegistrationResponse registrationResponse = new RegistrationResponse(
        req.getParameter("registration_data"), 
        req.getParameter("client_data"),
        req.getParameter("session_id")
    );

    // req.getParamter("challenge") is not being used by this server
    resp.setContentType("application/json");
    
    try {
      SecurityKeyData tokenData = u2fServer.processRegistrationResponse(
          registrationResponse,
          System.currentTimeMillis());
      System.out.println(tokenData.toString());

      JsonObject data = new JsonObject();
      data.addProperty("status", "success");
      body.println(data.toString());
    } catch (U2FException e) {
        body.println("{\"status\": \"failure\",\"token\":\""+ e.getMessage() + "\"}");
    }
  }
}
