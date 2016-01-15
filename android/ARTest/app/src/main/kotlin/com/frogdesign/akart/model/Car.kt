package com.frogdesign.akart.model;

import org.artoolkit.ar.base.ARToolKit

data class Position(var x: Float, var y: Float, var z: Float) {
    operator infix fun divAssign(d: Int) {
        x /= d;
        y /= d;
        z /= d;
    }
}

/**
 * In the a-kart game the car is identified by two marker glued on the rear
 * of the cars, one on the left and one on the right of the car
 *
 */
data class Car(val id: String, val lrMarkers: Pair<Int, Int>) {
    var leftAR: Int = -1;
    var rightAR: Int = -1;

    fun isDetected(arScene: ARToolKit): Boolean = isLeftMarkerVisible(arScene) || isRightMarkerVisible(arScene)

    private fun isLeftMarkerVisible(arScene: ARToolKit) = leftAR >= 0 && arScene.queryMarkerVisible(leftAR)
    private fun isRightMarkerVisible(arScene: ARToolKit) = rightAR >= 0 && arScene.queryMarkerVisible(rightAR)

    fun estimatePosition(arScene: ARToolKit): Position {
        var pos : Position = Position(0f,0f,0f);
        if (!isDetected(arScene)) return pos;
        var sides : Int = 0;
        if(isLeftMarkerVisible(arScene)) {
            var matrix : FloatArray = arScene.queryMarkerTransformation(leftAR)
            sides++;
            pos.x += matrix[12];
            pos.y += matrix[13];
            pos.z += matrix[14];
        }

        if(isRightMarkerVisible(arScene)) {
            var matrix : FloatArray = arScene.queryMarkerTransformation(rightAR)
            sides++;
            pos.x += matrix[12];
            pos.y += matrix[13];
            pos.z += matrix[14];
        }

        //scaled to nullify depth
        var depth : Float = pos.z / -500;
        pos.x /= depth;
        pos.y /= depth;

        pos /= sides;

        val FACTOR = 2;
        pos.x *= FACTOR;
        pos.y *= FACTOR;
        return pos;
    }
}

object Cars {
    @JvmField
    public val all = listOf(
            Car("taxiguerrilla", Pair(0, 1)),
            Car("gargamella", Pair(2, 3))
    );
}