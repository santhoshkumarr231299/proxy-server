package com.proxy.recon.filter;

import com.proxy.recon.data.LoginData;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
public class AuthFilter extends UsernamePasswordAuthenticationFilter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requestUrl = ((HttpServletRequest)request).getRequestURI();
        String currentUser = null;
        if(authentication != null) {
            currentUser = authentication.getName();
        }

        if(!"/login".equals(requestUrl) && currentUser == null) {
            ((HttpServletResponse) response).sendRedirect("/login");
            return;
        }

        if("/login".equals(requestUrl) && currentUser != null) {
            ((HttpServletResponse) response).sendRedirect("/");
            return;
        }

        chain.doFilter(request, response);
    }
}
