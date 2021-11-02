package com.robin.opencvusage.app.network

class ApiException(val msg: String, val status: Int) :
    Throwable()