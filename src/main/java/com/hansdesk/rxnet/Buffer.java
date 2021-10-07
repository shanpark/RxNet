package com.hansdesk.rxnet;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class Buffer {
    private LinkedList<byte[]> bytes = new LinkedList<>();
    private int rIndex = 0;
    private int rPosition = 0;
    private int wIndex = 0;
    private int wPosition = 0;
    private byte[] wBytes;
    private byte[] rBytes;

    public Buffer(int initialCapacity) {
        bytes.add(new byte[initialCapacity * (int)Math.pow(2, bytes.size())]);
        rBytes = wBytes = bytes.getFirst();
    }

    boolean readable() {
        return (rIndex < wIndex) || (rPosition < wPosition);
    }

    int readableBytes() {
        return wPosition - rPosition;
    }

    int remaining() {
        return (wBytes.length - wPosition);
    }

    java.nio.ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(wBytes, wPosition, wBytes.length - wPosition);
    }

    public int read() {
        if (readable()) {
            byte data = rBytes[rPosition];
            postRead();
            return data;
        }
        return -1;
    }

    public void write(int b) {
        wBytes[wPosition] = (byte)b;
        postWrite();
    }

    /**
     * 버퍼에 count만큼의 데이터가 write되었을 때 position을 전진시키는 메소드이다.
     *
     * @param count 전진시킬 byte 수.
     * @return 새로운 버퍼할당 없이 전진시켰으면 true, 전진시키고 새로운 버퍼가 할당되었으면 false.
     */
    public boolean written(int count) {
        wPosition += count;
        if (wPosition >= wBytes.length) {
            wIndex++;
            if (wIndex >= bytes.size())
                bytes.add(new byte[bytes.getFirst().length * (int)Math.pow(2, bytes.size())]);
            wBytes = bytes.get(wIndex);
            wPosition = 0;
            return false;
        }
        return true;
    }

    private void postRead() {
        rPosition++;
        if (rPosition >= rBytes.length) {
            rIndex++;
            rBytes = bytes.get(rIndex);
            rPosition = 0;
        }
    }

    private void postWrite() {
        wPosition++;
        if (wPosition >= wBytes.length) {
            wIndex++;
            if (wIndex >= bytes.size())
                bytes.add(new byte[bytes.getFirst().length * (int)Math.pow(2, bytes.size())]);
            wBytes = bytes.get(wIndex);
            wPosition = 0;
        }
    }
}
