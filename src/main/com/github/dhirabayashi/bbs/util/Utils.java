package com.github.dhirabayashi.bbs.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Utils {
    private Utils(){}

    public static byte[] inputStreamToBytes(InputStream inputStream) throws IOException {
        var bout = new ByteArrayOutputStream();
        var buffer = new byte[1024];
        while(true) {
            int len = inputStream.read(buffer);
            if(len < 0) {
                break;
            }
            bout.write(buffer, 0, len);
        }
        return bout.toByteArray();
    }
}
