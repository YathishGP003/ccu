package a75f.io.renatus;

import junit.framework.TestCase;

public class OOMExceptionHandlerTest extends TestCase {

    public void testIsOOMCausedByFragmentation_WithValidInput_ShouldReturnTrue() {
        Throwable throwable = new OutOfMemoryError("10 byte allocation with 100 free bytes");
        boolean result = OOMExceptionHandler.isOOMCausedByFragmentation(throwable);
        assertTrue(result);
    }


    public void testIsOOMCausedByFragmentation_WithInvalidInput_ShouldReturnFalse() {
        Throwable throwable = new OutOfMemoryError("Out of memory");
        boolean result = OOMExceptionHandler.isOOMCausedByFragmentation(throwable);
        assertFalse(result);
    }


    public void testIsErrorCausedByFragmentation_WithValidInput_ShouldReturnTrue() {
        String input = "10 byte allocation with 100 free bytes";
        boolean result = OOMExceptionHandler.isErrorCausedByFragmentation(input);
        assertTrue(result);
    }


    public void testIsErrorCausedByFragmentation_WithInvalidInput_ShouldReturnFalse() {
        String input = "Out of memory";
        boolean result = OOMExceptionHandler.isErrorCausedByFragmentation(input);
        assertFalse(result);
    }


    public void testIsByteAllocationLessThanFreeBytes_WithLessThanComparison_ShouldReturnTrue() {
        boolean result = OOMExceptionHandler.isByteAllocationLessThanFreeBytes(10, 100);
        assertTrue(result);
    }


    public void testIsByteAllocationLessThanFreeBytes_WithGreaterThanComparison_ShouldReturnFalse() {
        boolean result = OOMExceptionHandler.isByteAllocationLessThanFreeBytes(100, 10);
        assertFalse(result);
    }


    public void testIsByteAllocationLessThanFreeBytes_WithEqualToComparison_ShouldReturnFalse() {
        boolean result = OOMExceptionHandler.isByteAllocationLessThanFreeBytes(10, 10);
        assertFalse(result);
    }
}