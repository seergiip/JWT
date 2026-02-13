package com.rgbconsulting.jwt.controller;

import com.rgbconsulting.jwt.model.Jwt;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 *
 * @author sergi
 */
interface JWTRepository extends JpaRepository<Jwt, Long> {
    
}
