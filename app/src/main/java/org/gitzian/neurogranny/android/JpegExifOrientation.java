package org.gitzian.neurogranny.android;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads the JPEG EXIF orientation tag without copying the original photo.
 *
 * <p>This is the API 23 fallback. Android's ExifInterface(InputStream) constructor
 * is available on API 24 and later. Only tag 0x0112 is interpreted; no other
 * image metadata is retained or exported.
 */
public final class JpegExifOrientation {
    public static final int NORMAL = 1;

    private static final int EXIF_MARKER = 0xe1;
    private static final int ORIENTATION_TAG = 0x0112;
    private static final int SHORT_TYPE = 3;

    private JpegExifOrientation() {
    }

    public static int read(InputStream stream) throws IOException {
        if (stream.read() != 0xff || stream.read() != 0xd8) {
            return NORMAL;
        }

        while (true) {
            int markerPrefix;
            do {
                markerPrefix = stream.read();
            } while (markerPrefix == 0xff);

            if (markerPrefix < 0 || markerPrefix == 0xda || markerPrefix == 0xd9) {
                return NORMAL;
            }
            if (markerPrefix == 0x00 || markerPrefix == 0xd8 || markerPrefix == 0x01
                    || markerPrefix >= 0xd0 && markerPrefix <= 0xd7) {
                continue;
            }

            int length = readWord(stream);
            if (length < 2) {
                return NORMAL;
            }
            int payloadLength = length - 2;
            if (markerPrefix != EXIF_MARKER) {
                skipFully(stream, payloadLength);
                continue;
            }

            byte[] payload = new byte[payloadLength];
            if (!readFully(stream, payload)) {
                return NORMAL;
            }
            int orientation = fromExifPayload(payload);
            if (orientation != NORMAL) {
                return orientation;
            }
        }
    }

    static int fromExifPayload(byte[] bytes) {
        if (bytes.length < 14 || bytes[0] != 'E' || bytes[1] != 'x'
                || bytes[2] != 'i' || bytes[3] != 'f'
                || bytes[4] != 0 || bytes[5] != 0) {
            return NORMAL;
        }

        int offset = 6;
        boolean littleEndian;
        if (bytes[offset] == 'I' && bytes[offset + 1] == 'I') {
            littleEndian = true;
        } else if (bytes[offset] == 'M' && bytes[offset + 1] == 'M') {
            littleEndian = false;
        } else {
            return NORMAL;
        }

        if (word(bytes, offset + 2, littleEndian) != 42) {
            return NORMAL;
        }
        long ifdOffset = unsignedInt(bytes, offset + 4, littleEndian);
        if (ifdOffset > bytes.length - offset - 2) {
            return NORMAL;
        }
        int entriesOffset = offset + (int) ifdOffset;
        int entries = word(bytes, entriesOffset, littleEndian);
        for (int i = 0; i < entries; i++) {
            long index = (long) entriesOffset + 2 + (long) i * 12;
            if (index > bytes.length - 12) {
                break;
            }
            int entry = (int) index;
            if (word(bytes, entry, littleEndian) == ORIENTATION_TAG
                    && word(bytes, entry + 2, littleEndian) == SHORT_TYPE
                    && unsignedInt(bytes, entry + 4, littleEndian) == 1) {
                int orientation = word(bytes, entry + 8, littleEndian);
                return orientation >= 1 && orientation <= 8 ? orientation : NORMAL;
            }
        }
        return NORMAL;
    }

    private static int word(byte[] bytes, int index, boolean littleEndian) {
        if (littleEndian) {
            return (bytes[index] & 0xff) | ((bytes[index + 1] & 0xff) << 8);
        }
        return ((bytes[index] & 0xff) << 8) | (bytes[index + 1] & 0xff);
    }

    private static long unsignedInt(byte[] bytes, int index, boolean littleEndian) {
        long low = word(bytes, index + (littleEndian ? 0 : 2), littleEndian);
        long high = word(bytes, index + (littleEndian ? 2 : 0), littleEndian);
        return low | (high << 16);
    }

    private static int readWord(InputStream stream) throws IOException {
        int high = stream.read();
        int low = stream.read();
        return high < 0 || low < 0 ? -1 : high << 8 | low;
    }

    private static boolean readFully(InputStream stream, byte[] target) throws IOException {
        int read = 0;
        while (read < target.length) {
            int next = stream.read(target, read, target.length - read);
            if (next < 0) {
                return false;
            }
            if (next == 0) {
                int single = stream.read();
                if (single < 0) {
                    return false;
                }
                target[read++] = (byte) single;
            } else {
                read += next;
            }
        }
        return true;
    }

    private static void skipFully(InputStream stream, int count) throws IOException {
        while (count > 0) {
            long skipped = stream.skip(count);
            if (skipped > 0) {
                count -= (int) skipped;
            } else if (stream.read() != -1) {
                count--;
            } else {
                return;
            }
        }
    }
}
