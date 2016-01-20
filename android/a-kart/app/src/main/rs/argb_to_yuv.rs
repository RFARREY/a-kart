#pragma version(1)
#pragma rs java_package_name(com.frogdesign.akart)

rs_allocation gInImage;
uchar *outBytes;

int width;
int height;
int frameSize;

void filter() {
    int yIndex = 0;
    int uvIndex = frameSize;
    for (int j = 0; j < height; j++) {
        for (int i = 0; i < width; i++) {
            const uchar4 *in_pixel = rsGetElementAt(gInImage, i, j);

            int R = in_pixel->r;
            int G = in_pixel->g;
            int B = in_pixel->b;

            int Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;

            outBytes[yIndex++] = (uchar) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
            if (j % 2 == 0 && i % 2 == 0) {
                int U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                int V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;
                outBytes[uvIndex++] = (uchar) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                outBytes[uvIndex++] = (uchar) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
            }
        }
    }
}