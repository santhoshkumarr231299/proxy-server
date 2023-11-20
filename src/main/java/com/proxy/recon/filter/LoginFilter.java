package com.proxy.recon.filter;

import com.proxy.recon.data.LoginData;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;


@Component
@Order(1)
public class LoginFilter implements Filter {
    @Autowired
    LoginData loginData;
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        if(authentication == null || currentUser == null) {
            ((HttpServletResponse) response).sendRedirect("/logout");
            return;
        }

        String ip = request.getRemoteAddr();
        System.out.println("IP : " + ip);

        if(loginData.getGitlabLoginSuccessIpList().contains(ip)) {
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            response.getWriter().println("<h1>Congrats! You have completed the challenge</h1>");
            return;
        } else if(loginData.getGitlabLoginSuccessIpList().size() >= loginData.getGitlabLoginAllowed()) {
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            response.getWriter().println("<h1>You have been Eliminated</h1>");
            return;
        }
//        System.out.println(ip + " : " + currentUser);
        if(loginData.getProxyLoginSuccessIpList().contains(ip) || loginData.getProxyLoginSuccessUsernameList().contains(currentUser)) {
//            System.out.println(loginData.getIpUsernameMap().get(ip));
//            if(currentUser.equals(loginData.getIpUsernameMap().get(ip))) {
//                //
//            } else {
//                response.setContentType(MediaType.TEXT_HTML_VALUE);
//                String respContent = "<h1>These credentials have been taken by someone else.</h1>";
//                respContent += "<br/>";
//                respContent += "<a href='/logout'> Click here to logout</a>";
//                response.getWriter().println(respContent);
//                return;
//            }
        } else if(loginData.getProxyLoginSuccessIpList().size() >= loginData.getProxyLoginAllowed()) {
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            response.getWriter().println("<h1>Login Blocked</h1>");
            return;
        } else {
            loginData.addProxyLoginSuccessIpList(ip);
            loginData.addProxyLoginSuccessUsernameList(currentUser);
            loginData.addIpUsernameMap(currentUser, ip);
        }

        chain.doFilter(request,response);
    }
}
