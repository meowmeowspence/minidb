package minidb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageTest {

    @Test
    void pageHasCorrectSize() {

        Page page = new Page(0);

        assertEquals(
                4096,
                page.toByteArray().length
        );
    }

    @Test
    void bytesCanBeWrittenAndRead() {

        Page page = new Page(0);

        byte[] bytes = {
                10,
                20,
                30,
                40
        };

        page.write(100, bytes);

        byte[] result =
                page.read(100, 4);

        assertArrayEquals(
                bytes,
                result
        );
    }

    @Test
    void writingPastPageBoundaryFails() {

        Page page = new Page(0);

        byte[] bytes =
                new byte[10];

        assertThrows(
                IllegalArgumentException.class,
                () -> page.write(
                        4090,
                        bytes
                )
        );
    }

}