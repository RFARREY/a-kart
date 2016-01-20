/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#pragma version(1)
#pragma rs java_package_name(com.frogdesign.akart)

rs_allocation gInImage;

uchar *outBytes;

int width;
int height;
int frameSize;

void filter() {
        int yIndex = 0;
        int uvIndex;
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