package com.frogdesign.akart.model;

import org.artoolkit.ar.base.ARToolKit

/**
 * In the a-kart game the car is identified by two marker glued on the rear
 * of the cars, one on the left and one on the right of the car
 *
 */
data class Car(val id: String, val lrMarkers: Pair<Int, Int>) {
    var leftAR: Int = -1;
    var rightAR: Int = -1;

    fun isDetected(arScene : ARToolKit) : Boolean = isLeftMarkerVisible(arScene) || isRightMarkerVisible(arScene)

    private fun isLeftMarkerVisible(arScene : ARToolKit) = leftAR >= 0 && arScene.queryMarkerVisible(leftAR)
    private fun isRightMarkerVisible(arScene : ARToolKit) = rightAR >= 0 && arScene.queryMarkerVisible(rightAR)

    fun estimatePosition(arScene : ARToolKit) : Pair<Float, Float> {
        var matrix : FloatArray = arScene.queryMarkerTransformation(leftAR);
        return Pair(matrix[12], matrix[13]);
    }
}

object Cars {
    @JvmField
    public val all = listOf(
            Car("taxiguerrilla", Pair(0, 1)),
            Car("gargamella", Pair(2, 3))
    );
}