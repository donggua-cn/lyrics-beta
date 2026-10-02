package com.applemusic.android.widevine

import android.util.Base64

/**
 * 内置 Widevine L3 设备凭证（WVD）。
 *
 * 来源与鸿蒙版一致：gamdl 项目内置（其源码注释：Dumped from Android Studio
 * Virtual Device running Android 9）。**仅用于个人自用，不要外传。**
 *
 * 作用：当系统 MediaDrm 这条路走不通时（比如某些设备不给 Widevine、或
 * Apple 的 key server 不接受本机 CDM 的 challenge），用这份凭证在本机
 * 直接构造 license challenge，不需要任何外部 CDM 二进制。
 */
object BuiltinWvd {

    /**
     * base64 解码器。默认用 Android 的实现；单元测试（JVM，没有 android.util.Base64）
     * 会把它换成 java.util.Base64 —— 否则 `BYTES` 会直接 NPE。
     */
    @JvmStatic
    var decoder: (String) -> ByteArray = { s -> Base64.decode(s, Base64.DEFAULT) }

    val BYTES: ByteArray by lazy { decoder(BASE64) }

    private const val BASE64 =
        "V1ZEAgIDAASoMIIEpAIBAAKCAQEAwnCFAPXy4U1J7p1NohAS+xl040f5FBaE/59bPp301bGz0UGFT9VoEtY3vaeakKh/d319xTNvCSWsEDRaMmp/wSnMiEZU" +
            "kkl04872jx2uHuR4k6KYuuJoqhsIo1TwUBueFZynHBUJzXQeW8Eb1tYAROGwp8W7r+b0RIjHC89RFnfVXpYlF5I6McktyzJNSOwlQbMqlVihfSUkv3WRd3HF" +
            "mA0Oxay51CEIkoTlNTHVlzVyhov5eHCDSp7QENRgaaQ03jC/CcgFOoQymhsBtRCM0CQmfuAHjA9e77R6m/GJPy75G9fqoZM1RMzVDHKbKZPd3sFd0c0+77gL" +
            "zW8cWEaaHwIDAQABAoIBAQCB2pN46MikHvHZIcTPDt0eRQoDH/YArGl2Lf7J+sOgU2U7wv49KtCug9IGHwDiyyUVsAFmycrF2RroV45FTUq0vi2SdSXV7Kjb" +
            "20Ren/vBNeQw9M37QWmU8Sj7q6YyWb9hv5T69DHvvDTqIjVtbM4RMojAAxYti5hmjNIh2PrWfVYWhXxCQ/WqAjWLtZBM6Oww1byfr5I/wFogAKkgHi8wYXZ4" +
            "LnIC8V7jLAhujlToOvMMC9qwcBiPKDP2FO+CPSXaqVhH+LPSEgLggnU3EirihgxovbLNAuDEeEbRTyR70B0lW19tLHixso4ZQa7KxlVUwOmrHSZf7nVuWqPp" +
            "xd+BAoGBAPQLyJ1IeRavmaU8XXxfMdYDoc8+xB7v2WaxkGXb6ToX1IWPkbMz4yyVGdB5PciIP3rLZ6s1+ruuRRV0IZ98i1OuN5TSR56ShCGg3zkd5C4L/xSM" +
            "Az+NDfYSDBdO8BVvBsw21KqSRUi1ctL7QiIvfedrtGb5XrE4zhH0gjXlU5qZAoGBAMv2segn0Jx6az4rqRa2Y7zRx4iZ77JUqYDBI8WMnFeR54uiioTQ+rOs" +
            "3zK2fGIWlrn4ohco/STHQSUTB8oCOFLMx1BkOqiR+UyebO28DJY7+V9ZmxB2Guyi7W8VScJcIdpSOPyJFOWZQKXdQFW3YICD2/toUx/pDAJh1sEVQsV3AoGB" +
            "ANyyp1rthmvoo5cVbymhYQ08vaERDwU3PLCtFXu4E0Ow90VNn6Ki4ueXcv/gFOp7pISk2/yuVTBTGjCblCiJ1en4HFWekJwrvgg3Vodtq8Okn6pyMCHRqvWE" +
            "PqD5hw6rGEensk0K+FMXnF6GULlfn4mgEkYpb+PvDhSYvQSGfkPJAoGAF/bAKFqlM/1eJEvU7go35bNwEiij9Pvlfm8y2L8Qj2lhHxLV240CJ6IkBz1Rl+S3" +
            "iNohkT8LnwqaKNT3kVB5daEBufxMuAmOlOX4PmZdxDj/r6hDg8ecmjj6VJbXt7JDd/c5ItKoVeGPqu035dpJyE+1xPAY9CLZel4scTsiQTkCgYBt3buRcZMw" +
            "nc4qqpOOQcXK+DWD6QvpkcJ55ygHYw97iP/lF4euwdHd+I5b+11pJBAao7G0fHX3eSjqOmzReSKboSe5L8ZLB2cAI8AsKTBfKHWmCa8kDtgQuI86fUfirCGd" +
            "hdA9AVP2QXN2eNCuPnFWi0WHm4fYuUB5be2c18ucxAb9CAESmgsK3QMIAhIQ071yBlsbLoO2CSB9Ds0cmRif6uevBiKOAjCCAQoCggEBAMJwhQD18uFNSe6d" +
            "TaIQEvsZdONH+RQWhP+fWz6d9NWxs9FBhU/VaBLWN72nmpCof3d9fcUzbwklrBA0WjJqf8EpzIhGVJJJdOPO9o8drh7keJOimLriaKobCKNU8FAbnhWcpxwV" +
            "Cc10HlvBG9bWAEThsKfFu6/m9ESIxwvPURZ31V6WJReSOjHJLcsyTUjsJUGzKpVYoX0lJL91kXdxxZgNDsWsudQhCJKE5TUx1Zc1coaL+Xhwg0qe0BDUYGmk" +
            "NN4wvwnIBTqEMpobAbUQjNAkJn7gB4wPXu+0epvxiT8u+RvX6qGTNUTM1QxymymT3d7BXdHNPu+4C81vHFhGmh8CAwEAASjwIkgBUqoBCAEQABqBAQQlRbfi" +
            "BNDb6eU6aKrsH5WJaYszTioXjPLrWN9dqyW0vwfT11kgF0BbCGkAXew2tLJJqIuD95cjJvyGUSN6VyhL6dp44fWEGDSBIPR0mvRq7bMP+m7Y/RLKf83+OyVJ" +
            "u/BpxivQGC5YDL9f1/A8eLhTDNKXs4Ia5DrmTWdPTPBL8SIgyfUtg3ofI+/I9Tf7it7xXpT0AbQBJfNkcNXGpO3JcBMSgAIL5xsXK5of1mMwAl6ygN1Gsj4a" +
            "Z052otnwN7kXk12SMsXheWTZ/PYh2KRzmt9RPS1T8hyFx/Kp5VkBV2vTAqqWrGw/dh4URqiHATZJUlhO7PN5m2Kq1LVFdXjWSzP5XBF2S83UMe+YruNHpE5G" +
            "QrSyZcBqHO0QrdPcU35GBT7S7+IJr2AAXvnjqnb8yrtpPWN2ZW/IWUJN2z4vZ7/HV4aj3OZhkxC1DIMNyvsusUKoQQuf8gwKiEe8cFwbwFSicywlFk9la2IP" +
            "e8oFShcxAzHLCCn/TIYUAvEL3/4LgaZvqWm80qCPYbgIP5HT8hPYkKWJ4WYknEWK+3InbnkzteFfGrQFCq4CCAESEGnj6Ji7LD+4o7MoHYT4jBQYjtW+kQUi" +
            "jgIwggEKAoIBAQDY9um1ifBRIOmkPtDZTqH+CZUBbb0eK0Cn3NHFf8MFUDzPEz+emK/OTub/hNxCJCao//pP5L8tRNUPFDrrvCBMo7Rn+iUb+mA/2yXiJ6iv" +
            "qcN9Cu9i5qOU1ygon9SWZRsujFFB8nxVreY5Lzeq0283zn1Cg1stcX4tOHT7utPzFG/ReDFQt0O/GLlzVwB0d1sn3SKMO4XLjhZdncrtF9jljpg7xjMIlnWJ" +
            "UqxDo7TQkTytJmUl0kcM7bndBLerAdJFGaXc6oSY4eNy/IGDluLCQR3KZEQsy/mLeV1ggQ44MFr7XOM+rd+4/314q/deQbjHqjWFuVr8iIaKbq+R63ShAgMB" +
            "AAEo8CISgAMii2Mw6z+Qs1bvvxGStie9tpcgoO2uAt5Zvv0CDXvrFlwnSbo+qR71Ru2IlZWVSbN5XYSIDwcwBzHjY8rNr3fgsXtSJty425djNQtF5+J2jrAh" +
            "f3Q2m7EI5aohZGpD2E0cr+dVj9o8x0uJR2NWR8FVoVQSXZpad3M/4QzBLNto/tz+UKyZwa7Sc/eTQc2+ZcDS3ZEO3lGRsH864Kf/cEGvJRBBqcpJXKfG+Itq" +
            "EW1AAPptjuggzmZEzRq5xTGf6or+bXrKjCpBS9G1SOyvCNF1k5z6lG8KsXhgQxL6ADHMoulxvUIihyPY5MpimdXfUdEQ5HA2EqNiNVNIO4qP007jW51yAeTh" +
            "Ory4J22xs8RdkIClOGAauLIl0lLA4flMzW+VfQl5xYxP0E5tuhn0h+844DslU8ZF7U1dU2QprIApffXD9wgAACk26Rggy8e96z8i86/+YYyZQkc9hIdCAERr" +
            "gEYCEbByzONrdRDs1MrS/ch1moV5pJv63BIKvQHGvLkaFwoMY29tcGFueV9uYW1lEgd1bmtub3duGioKCm1vZGVsX25hbWUSHEFuZHJvaWQgU0RLIGJ1aWx0" +
            "IGZvciB4ODZfNjQaGwoRYXJjaGl0ZWN0dXJlX25hbWUSBng4Nl82NBodCgtkZXZpY2VfbmFtZRIOZ2VuZXJpY194ODZfNjQaIAoMcHJvZHVjdF9uYW1lEhBz" +
            "ZGtfcGhvbmVfeDg2XzY0GmMKCmJ1aWxkX2luZm8SVUFuZHJvaWQvc2RrX3Bob25lX3g4Nl82NC9nZW5lcmljX3g4Nl82NDo5L1BTUjEuMTgwNzIwLjAxMi80" +
            "OTIzMjE0OnVzZXJkZWJ1Zy90ZXN0LWtleXMaHgoUd2lkZXZpbmVfY2RtX3ZlcnNpb24SBjE0LjAuMBokCh9vZW1fY3J5cHRvX3NlY3VyaXR5X3BhdGNoX2xl" +
            "dmVsEgEwMg4QASAAKA0wAEAASABQAA=="
}
