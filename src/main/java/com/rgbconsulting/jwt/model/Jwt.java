package com.rgbconsulting.jwt.model;

import java.util.Objects;

/**
 *
 * @author sergi
 */
public class Jwt {

    private String username;
    private String password;
    private String access_token;
    private Integer expires_in;
    private Long time_generated;
    private String role;

    public Jwt() {

    }

    public Jwt(Jwt jwt) {
        this.username = jwt.getUsername();
        this.password = jwt.getPassword();
        this.access_token = jwt.getAccess_token();
        this.expires_in = jwt.getExpires_in();
        this.time_generated = jwt.getTime_generated();
        this.role = role;
    }
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public Integer getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(Integer expires_in) {
        this.expires_in = expires_in;
    }

    public Long getTime_generated() {
        return this.time_generated;
    }

    public void setTime_generated(Long time_generated) {
        this.time_generated = time_generated;
    }
    
    public String getRole() {
        return this.role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Jwt jwt = (Jwt) o;
        return Objects.equals(username, jwt.username)
                && Objects.equals(password, jwt.password)
                && Objects.equals(access_token, jwt.access_token)
                && Objects.equals(expires_in, jwt.expires_in)
                && Objects.equals(role, jwt.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password, access_token, expires_in, role);
    }

    @Override
    public String toString() {
        return "jwt{"
                + ", username='" + username + '\''
                + ", password='" + password + '\''
                + ", access_token='" + access_token + '\''
                + ", expires_in=" + expires_in
                + ", role=" + role
                + '}';
    }
}
