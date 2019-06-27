package a75f.io.logic.bo.util;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Created by ryanmattison isOn 7/25/17.
 */

public class ByteArrayUtils
{

    private static final String TAG = ByteArrayUtils.class.getSimpleName();

    private static final char[] HEX_NUMERALS = "0123456789ABCDEF".toCharArray();
    private static final int MASK_4 = 0xF;


    public static byte[] nullTerminateAndFillArrayToLengthFromString(String input, int size)
    {
        byte[] value = nullTerminateByteArray(input.getBytes(StandardCharsets.UTF_8));
        value = nullTerminateByteArray(value);
        return fillArrayToLength(value, size);
    }


    /**
     * Generates a null-terminated byte array
     *
     * @param inputBytes The byte array to have a null terminator appended to
     * @return The byte array with a null terminator appended
     */
    public static byte[] nullTerminateByteArray(byte[] inputBytes) {
        byte[] nullTerminator = {'\0'};
        byte[] value = new byte[inputBytes.length + nullTerminator.length];

        System.arraycopy(inputBytes, 0, value, 0, inputBytes.length);
        System.arraycopy(nullTerminator, 0, value, inputBytes.length, nullTerminator.length);

        return value;
    }

    public static byte[] fillArrayToLength(byte[] inputBytes, int size)
    {
        //Pad the name with null bytes
        if (inputBytes.length < size) {
            int newLen = size - inputBytes.length;
            byte[] emptyBuff = new byte[newLen];
            Arrays.fill(emptyBuff, (byte) 0x00);
            inputBytes = addBytes(inputBytes, emptyBuff);
        }

        return inputBytes;
    }

    /**
     * Concatenates the provided byte arrays
     *
     * @param bytes The byte arrays to be concatenated
     * @return The concatenated arrays
     */
    public static byte[] addBytes(byte[]... bytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (byte[] b : bytes) {
            try {
                outputStream.write(b);
            } catch (IOException e) {
                Log.e(TAG, "[addBytes] Failed to write to output stream.");
            }
        }
        return outputStream.toByteArray();
    }


    /**
     * Computes a CRC from the provided byte array
     *
     * @param message The array to generate a CRC from
     * @return The CRC, represented as a byte array
     */
    public static byte[] computeCrc(byte[] message) {
        CRC32 crcGen = new CRC32();
        crcGen.update(message);
        long val = crcGen.getValue();
        byte[] crc = ByteArrayUtils.toByteArray((int) val);
        return crc;
    }
    
    public static byte[] toByteArray(int value) {
        return new byte[] {
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value};
    }

    /**
     * Converts a big endian byte array to little endian
     *
     * @param bytesBe The byte array in big endian
     * @return The byte array in little endian form
     */
    public static byte[] bigToLittleEndian(byte[] bytesBe) {

        byte[] bytesLe = new byte[bytesBe.length];
        for (int i = 0; i < bytesBe.length; i++) {
            bytesLe[bytesBe.length - 1 - i] = bytesBe[i];
        }

        return bytesLe;
    }

    /**
     * Utility method to convert a hex string to a byte array
     *
     * @param s The string to be converted
     * @return A byte array of the converted string
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * A utility method to convert a byte array to a hex string
     * Based on the javax.xml implementation, with modifications
     *
     * @param data   The array to be converted
     * @param spaces A flag for whether the output should have spaces between each byte
     * @return A hex string of the converted byte array
     */
    public static String byteArrayToHexString(byte[] data, boolean spaces) {
        StringBuilder r = new StringBuilder();
        for (byte b : data) {
            r.append(HEX_NUMERALS[(b >> 4) & MASK_4]);
            r.append(HEX_NUMERALS[(b & MASK_4)]);
            if (spaces) {
                r.append(' ');
            }
        }
        return r.toString();
    }
}
