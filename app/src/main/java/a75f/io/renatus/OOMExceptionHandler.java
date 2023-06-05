package a75f.io.renatus;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OOMExceptionHandler {

    public static boolean isOOMCausedByFragmentation(Throwable throwable) {
        if (throwable instanceof OutOfMemoryError) {
            String message = throwable.getMessage();
            return message != null && isErrorCausedByFragmentation(message);
        }
        return false;
    }

    public static boolean isErrorCausedByFragmentation(String input) {
        try {
            Pattern pattern = Pattern.compile("(\\d+)\\sbyte allocation with (\\d+)\\sfree bytes");
            Matcher matcher = pattern.matcher(input);
            long[] numbers = null;

            if (matcher.find()) {
                long byteAllocation = Long.parseLong(Objects.requireNonNull(matcher.group(1)));
                long freeBytes = Long.parseLong(Objects.requireNonNull(matcher.group(2)));
                numbers = new long[]{byteAllocation, freeBytes};
            }
            if (numbers != null && numbers.length >= 2) {
                long firstNumber = numbers[0];
                long secondNumber = numbers[1];
                return isByteAllocationLessThanFreeBytes(firstNumber, secondNumber);
            }

        } catch (NumberFormatException | NullPointerException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean isByteAllocationLessThanFreeBytes(long firstNumber, long secondNumber) {
        return firstNumber < secondNumber;
    }

}
