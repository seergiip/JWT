package com.rgbconsulting.jwt.controller;

/**
 *
 * @author sergi
 */
class JwtNotFoundException extends RuntimeException {
    JwtNotFoundException(Long id) {
    super("Could not find jwt " + id);
  }
}
