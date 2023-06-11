package com.lightimageloaderdownloader.lild.exceptions

class IllegalDataException(type: Any?) : Exception("Data type not supported ${type?.javaClass}")