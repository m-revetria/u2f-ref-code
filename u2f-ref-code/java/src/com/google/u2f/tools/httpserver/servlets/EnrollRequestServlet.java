package com.google.u2f.tools.httpserver.servlets;

import java.io.PrintStream;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;

import com.google.gson.JsonObject;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.RegistrationRequest;

public class EnrollRequestServlet extends JavascriptServlet {

  private final U2FServer u2fServer;

  public EnrollRequestServlet(U2FServer u2fServer) {
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
    RegistrationRequest registrationRequest = u2fServer.getRegistrationRequest(userName, applicationId);

    JsonObject enrollServerData = new JsonObject();
    enrollServerData.addProperty("challenge", registrationRequest.getChallenge());
    enrollServerData.addProperty("version", registrationRequest.getVersion());
    enrollServerData.addProperty("session_id", registrationRequest.getSessionId());

    resp.setContentType("application/json");
    
    body.println(enrollServerData.toString());
  }
}