package com.proxy.recon.data;

import com.proxy.recon.model.AuthModel;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Getter
public class LoginData {
    final int proxyLoginAllowed = 2;
    final int gitlabLoginAllowed = 1;

    private List<String> proxyLoginSuccessUsernameList = new ArrayList<>();
//    private List<String> gitlabLoginSuccessUsernameList = new ArrayList<>();
    private Map<String, String> ipUsernameMap = new HashMap<>();

    private List<String> gitlabLoginSuccessIpList = new ArrayList<>();
    private List<String> proxyLoginSuccessIpList = new ArrayList<>();
    public synchronized void addProxyLoginSuccessIpList(String ip) {
        if(proxyLoginSuccessIpList.size() < proxyLoginAllowed) {
            proxyLoginSuccessIpList.add(ip);
        }
    }
    public synchronized void addGitlabLoginSuccessIpList(String ip) {
        if(gitlabLoginSuccessIpList.size() < gitlabLoginAllowed) {
            gitlabLoginSuccessIpList.add(ip);
        }
    }

    public synchronized void addProxyLoginSuccessUsernameList(String currentUser) {
        if(proxyLoginSuccessUsernameList.size() < proxyLoginAllowed) {
            this.proxyLoginSuccessUsernameList.add(currentUser);
        }
    }
    public synchronized void addIpUsernameMap(String currentUser, String ip) {
        if(proxyLoginSuccessUsernameList.size() < proxyLoginAllowed && gitlabLoginSuccessIpList.size() < gitlabLoginAllowed) {
            this.ipUsernameMap.put(currentUser, ip);
        }
    }
//    public synchronized void addProxyLoginSuccessUserAuth(String key, AuthModel value) {
//        this.proxyLoginSuccessUserAuth.put(key, value);
//    }

    public String getRootPasswordForCurrentUser(String currentUser) {
        return switch (currentUser) {
            case "temp" -> "temp";
            default -> null;
        };
    }
}
