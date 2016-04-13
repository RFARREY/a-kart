import json

from matplotlib import pyplot as plt

import cv2
import numpy as np


original = cv2.imread('gradient.png')
greyscaled = cv2.imread('gradient.png', 0)
origrows, origcols = greyscaled.shape
rows = origrows * 2
cols = origcols * 2

#img = cv2.medianBlur(img,5)
ret,thresholded = cv2.threshold(greyscaled,90,255,cv2.THRESH_BINARY_INV)

RADIUS = 10
DIAMETER = 2 * RADIUS
startrow = origrows / 2
startcolumn = origcols / 2
endrow = startrow + origrows
endcolumn = startcolumn + origcols
print startrow, startcolumn, endrow, endcolumn


ROTATE = 10.0 * np.pi / 180.0
the_sin = np.sin(ROTATE)
the_cos = np.cos(ROTATE)
def rotated_coords(x,y):
    return (int(the_cos * x - the_sin * y), int(the_sin * x + the_cos * y))


buffer = np.zeros((origrows, origcols))

def calculate_overlap_for(x,y, binary_image):
    global buffer
    buffer.fill(0)
    cv2.rectangle(buffer,(x - RADIUS,y - RADIUS),(x + RADIUS,y + RADIUS),WHITE, -1, 8)
    buffer = cv2.bitwise_and(buffer,buffer,mask=binary_image)
    return buffer.sum()

WHITE = (255,255,255)
overlaps = np.zeros((origrows, origcols))

i = 0
j = 0

dots = []
while True:
    x, y = rotated_coords(i, j)
    #print  x, y
    if x > startcolumn and y > startrow and x < endcolumn and y < endrow:
        real_x = x - startcolumn
        real_y = y - startrow
        overlaps[real_y,real_x] = calculate_overlap_for(real_x, real_y, thresholded)
        #print "Overlap ", x, " ", y, "=",overlaps[x / DIAMETER,y / DIAMETER]

    #advance in image
    i = i + DIAMETER
    if i >= rows:
        i = 0
        j = j + DIAMETER
        if j >= cols:
            break

print overlaps.max()
overlaps = overlaps / overlaps.max()
print overlaps.max(), overlaps.shape


#overlaps = overlaps[startrow / DIAMETER:endrow / DIAMETER, startcolumn / DIAMETER:endcolumn /DIAMETER]

x_p, y_p = overlaps.shape
final_image = np.zeros((origrows, origcols ,3), np.uint8)

dots = []
for (j, i) in np.ndindex(overlaps.shape):
    scaled_radius = (RADIUS + 1 )   * overlaps[j,i]
    #print i,j, overlaps[j, i] 
    if scaled_radius > 0:
        dot = { 'x' : i, 'y' : j, 'radius' : scaled_radius}
        dots.append(dot)
        cv2.circle(final_image,(i,j),int(scaled_radius),WHITE, -1, cv2.CV_AA)


images = [greyscaled, thresholded]

with open('data.txt', 'w') as outfile:
    json.dump(dots, outfile)
    print "%s points written" % len(dots)


cv2.imshow('original', original)
cv2.imshow('greyscale', greyscaled)
cv2.imshow('thresholded', thresholded)
cv2.imshow('final_image', final_image)
cv2.waitKey(0)
cv2.destroyAllWindows()