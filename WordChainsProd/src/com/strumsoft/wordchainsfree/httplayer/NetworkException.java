/**
 * NetworkException.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.strumsoft.wordchainsfree.httplayer;

/**
 * This class/interface
 * 
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * @Since May 14, 2012
 */
public class NetworkException extends Exception {

    private static final long serialVersionUID = -7022001752841718372L;

    public NetworkException(String message) {
        super(message);
    }
}
