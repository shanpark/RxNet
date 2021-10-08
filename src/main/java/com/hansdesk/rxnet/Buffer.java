package com.hansdesk.rxnet;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Buffer {
    private List<byte[]> bytes = new ArrayList<>();
    private int rIndex = 0;
    private int rPosition = 0;
    private int wIndex = 0;
    private int wPosition = 0;
    private int mIndex = -1;
    private int mPosition = 0;

    private InputStream inputStream;
    private OutputStream outputStream;

    static class _InputStream extends InputStream {
        private final Buffer buffer;

        _InputStream(Buffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public int read() {
            return buffer.read();
        }
    }

    static class _OutputStream extends OutputStream {
        private final Buffer buffer;

        _OutputStream(Buffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public void write(int b) {
            buffer.write(b);
        }
    }

    public Buffer(int initialCapacity) {
        bytes.add(new byte[initialCapacity]);
    }

    public InputStream inputStream() {
        if (inputStream == null)
            inputStream = new _InputStream(this);
        return inputStream;
    }

    public OutputStream outputStream() {
        if (outputStream == null)
            outputStream = new _OutputStream(this);
        return outputStream;
    }

    /**
     * Buffer에 읽을 수 있는 데이터가 있는 지 여부를 반환한다.
     *
     * @return true if any readable byte exists, other false.
     */
    boolean readable() {
        return (rIndex < wIndex) || (rPosition < wPosition);
    }

    /**
     * 이 버퍼에 쓰기 작업을 하기위한 ByteBuffer 객체를 반환한다.
     *
     * @return 현재 상태에서 write buffer의 남은 공간을 ByteBuffer로 wrap해서 반환한다.
     */
    java.nio.ByteBuffer byteBufferForRead() {
        return ByteBuffer.wrap(bytes.get(rIndex), rPosition, readableBytes());
    }

    /**
     * 이 버퍼에 쓰기 작업을 하기위한 ByteBuffer 객체를 반환한다.
     *
     * @return 현재 상태에서 write buffer의 남은 공간을 ByteBuffer로 wrap해서 반환한다.
     */
    java.nio.ByteBuffer byteBufferForWrite() {
        return ByteBuffer.wrap(bytes.get(wIndex), wPosition, writableBytes());
    }

    /**
     * reset() 메소드를 호출했을 때 돌아갈 현재 read buffer의 position을 기억해 놓는다.
     * reset()을 호출할 필요가 없어진다면 반드시 unmark() 메소드를 호출해야 한다.
     */
    public void mark() {
        mIndex = rIndex;
        mPosition = rPosition;
    }

    /**
     * mark() 메소드가 기록해놓은 위치를 해제한다. mark()를 호출한 후에 reset()을 호출할 필요가 없는 것으로 판단되면
     * 반드시 unmark()를 호출해줘야 한다.
     */
    public void unmark() {
        mIndex = -1;
        compact(); // mark 상태에서는 compact가 되지 않는다. unmark될 때 한 번 해줘야 한다.
    }

    /**
     * read buffer의 position을 mark()를 호출한 시점으로 되돌린다.
     * reset()을 호출한 후에는 unmark()를 호출할 필요는 없다.
     */
    public void reset() {
        if (mIndex > 0) {
            rIndex = mIndex;
            rPosition = mPosition;
            unmark();
        }
    }

    /**
     * 현재 mark가 된 상태인지 조회.
     *
     * @return mark가 된 상태이면 true, 아니면 false.
     */
    public boolean marked() {
        return (mIndex >= 0);
    }

    /**
     * 버퍼에서 한 byte를 읽는다.
     *
     * @return 읽은 바이트를 int형으로 반환.
     */
    public int read() {
        if (readable()) {
            byte data = bytes.get(rIndex)[rPosition];
            postRead(1);
            return data;
        }
        return -1;
    }

    /**
     * 버퍼에 한 byte를 write한다.
     *
     * @param b 버퍼에 기록할 값. 하위 8 bit만 write된다.
     */
    public void write(int b) {
        bytes.get(wIndex)[wPosition] = (byte)b;
        postWrite(1);
    }

    /**
     * parameter로 받은 buffer의 내용을 나의 buffer에 write한다..
     *
     * @param buffer 이 buffer의 내용을 나의 buffer에 write한다.
     */
    public void write(Buffer buffer) {
        int count = Math.min(buffer.readableBytes(), writableBytes());

        while (count > 0) {
            System.arraycopy(buffer.bytes.get(buffer.rIndex), buffer.rPosition, bytes.get(wIndex), wPosition, count);
            buffer.postRead(count); // read buffer position 이동.
            postWrite(count); // write buffer position 이동.

            count = Math.min(buffer.readableBytes(), writableBytes());
        }
    }

    /**
     * 한 번에 읽을 수 있는 데이터의 크기이다.
     *
     * @return 현재 read buffer에서 읽을 수 있는 데이터의 byte 수.
     */
    public int readableBytes() {
        return (wIndex == rIndex) ?
                (wPosition - rPosition) :
                (bytes.get(rIndex).length - rPosition);
    }

    /**
     * 한 번에 write가능한 공간의 크기이다.
     *
     * @return 현재 write buffer에서 write가능한 공간의 byte 수.
     */
    public int writableBytes() {
        return (bytes.get(wIndex).length - wPosition);
    }

    /**
     * 버퍼를 완전히 비우고 내부 버퍼의 크기도 초기 상태로 되돌린다.
     */
    public void clear() {
        rIndex = rPosition = 0;
        wIndex = wPosition = 0;
        if (bytes.size() > 1) {
            byte[] temp = bytes.get(0);
            bytes.clear();
            bytes.add(temp);
        }
    }

    /**
     * 내부 버퍼로 사용되는 array에서 직접 데이터를 read 했다면 다음 read 작업을 위해서 read position을 옮겨야 한다.
     * 그런 경우에 호출해서 이동시켜준다.
     *
     * @param count read position을 전진시킬 byte 수.
     */
    void advanceReadPosition(int count) {
        postRead(count);
    }

    /**
     * 내부 버퍼로 사용되는 array에 직접 데이터를 write 했다면 다음 write 작업을 위해서 write position을 옮겨야 한다.
     * 그런 경우에 호출해서 이동시켜준다.
     *
     * @param count write position을 전진시킬 byte 수.
     */
    boolean advanceWritePosition(int count) {
        return postWrite(count);
    }

    /**
     * 현재 read buffer에서 count만큼 데이틀 읽어들였을 때 position을 count만큼 전진시키는 메소드이다.
     * commit이 되기 전에는 buffer가 삭제되는 경우는 없다.
     *
     * @param count 전진시킬 byte 수.
     */
    private void postRead(int count) {
        while (count > 0) {
            int move = Math.min((bytes.get(rIndex).length - rPosition), count);
            count -= move;
            rPosition += move;
            if (rPosition >= bytes.get(rIndex).length) {
                rIndex++;
                rPosition = 0;
            }
        }

        compact();
    }

    /**
     * 현재 write buffer에 count만큼의 데이터가 write되었을 때 position을 count만큼 전진시키는 메소드이다.
     *
     * @param count 전진시킬 byte 수. remaining()이 반환하는 수 이하의 값만 올 수 있다.
     * @return 버퍼가 추가로 할당되었으면 true, 아니면 false
     */
    private boolean postWrite(int count) {
        boolean bufferChanged = false;

        wPosition += count;
        if (wPosition >= bytes.get(wIndex).length) {
            wIndex++;
            if (wIndex >= bytes.size())
                bytes.add(new byte[bytes.get(0).length * (1 << bytes.size())]);
            wPosition = 0;
            bufferChanged = true;
        }

        return bufferChanged;
    }

    /**
     * 버퍼의 내부 공간을 정리하여 메모리를 잡고 있지 않도록 정리한다.
     * 사용중인 내부 버퍼만 남기고 garbage collection 될 수 있도록 한다.
     */
    private void compact() {
        if (!marked()) {
            if (!readable()) {
                clear();
            } else if (rIndex > 0) {
                for (int inx = rIndex; inx <= wIndex; inx++)
                    bytes.set(inx - rIndex, bytes.get(inx));
                bytes = bytes.stream().limit(wIndex - rIndex + 1).collect(Collectors.toList());

                wIndex -= rIndex;
                rIndex = 0;
            }
        }
    }
}
