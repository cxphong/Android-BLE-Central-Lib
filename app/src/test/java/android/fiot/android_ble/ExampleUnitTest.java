package android.fiot.android_ble;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        int loss = calcNumberLostPacket(0, new byte[]{0});
        System.out.println("0, 0, loss: " + loss);

        loss = calcNumberLostPacket(0, new byte[]{});
        System.out.println("0, empty, loss: " + loss);
        loss = calcNumberLostPacket(0xff, new byte[]{(byte) 0});
        System.out.println("0xff,0, loss: " + loss);
        loss = calcNumberLostPacket(0xfe, new byte[]{(byte) 1});
        System.out.println("0xfe, 1, loss: " + loss);
        loss = calcNumberLostPacket(0, new byte[]{(byte) 1});
        System.out.println("0, 1, loss: " + loss);
        loss = calcNumberLostPacket(0, new byte[]{(byte) 2});
        System.out.println("0, 2, loss: " + loss);
        loss = calcNumberLostPacket(0, new byte[]{(byte) 4});
        System.out.println("0, 4, loss: " + loss);
        loss = calcNumberLostPacket(0xfa, new byte[]{(byte) 0x1});
        System.out.println("0xfa, 1, loss: " + loss);
    }

    public int calcNumberLostPacket(int currentIndex, byte[] packet) {
        if (packet.length == 0) {
            return 0;
        }

        int numberOfLoss;
        int newIndex = (packet[0] & 0xff);

        if (newIndex < currentIndex) {
            numberOfLoss = newIndex - currentIndex + 255;
        } else if (newIndex > currentIndex) {
            numberOfLoss = newIndex - currentIndex - 1;
        } else {
            numberOfLoss = 0;
        }

        return numberOfLoss;
    }
}