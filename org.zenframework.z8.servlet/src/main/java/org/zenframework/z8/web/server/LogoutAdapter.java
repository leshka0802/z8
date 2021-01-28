package org.zenframework.z8.web.server;

import org.zenframework.z8.web.servlet.Servlet;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class LogoutAdapter extends Adapter {

	static private final String AdapterPath = "/logout";

	public LogoutAdapter(Servlet servlet) {
		super(servlet);
	}

	@Override
	public boolean canHandleRequest(HttpServletRequest request) {
		return request.getServletPath().endsWith(AdapterPath);
	}
	
	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		HttpSession session = request.getSession();
		if (session != null)
			session.invalidate();

		RequestDispatcher requestDispatcher = request.getRequestDispatcher("/");
		requestDispatcher.forward(request, response);
	}

}
