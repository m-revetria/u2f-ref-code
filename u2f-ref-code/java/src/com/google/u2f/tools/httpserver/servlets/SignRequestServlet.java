package com.google.u2f.tools.httpserver.servlets;

import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;

import com.google.gson.JsonObject;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.U2fSignRequest;

public class SignRequestServlet extends JavascriptServlet {

  private final U2FServer u2fServer;

  public SignRequestServlet(U2FServer u2fServer) {
    this.u2fServer = u2fServer;
  }

  @Override
  public void generateJavascript(Request req, Response resp, PrintStream body) throws Exception {
    String userName = req.getParameter("user_name");
    String applicationId = req.getParameter("application_id");
    if (userName == null || applicationId == null) {
      resp.setStatus(Status.BAD_REQUEST);
      return;
    }
    
    U2fSignRequest signRequest = u2fServer.getSignRequest(userName, applicationId);

    JsonObject signData = new JsonObject();
    signData.addProperty("challenge", signRequest.getChallenge());
    signData.add("registered_keys", signRequest.getRegisteredKeysAsJson(applicationId));

    resp.setContentType("application/json");
    
    body.println(signData.toString());
  }
}