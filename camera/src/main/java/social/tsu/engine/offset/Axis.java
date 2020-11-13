package social.tsu.engine.offset;

import social.tsu.control.Facing;

/**
 * The axis around which offsets are computed. We have two possibilities:
 * - an axis going out of the device screen towards the user's face
 * - an axis going out of the device screen towards the back
 * <p>
 * We are mostly interested in the first one, but some APIs will require
 * angles in the sensor reference, in which case, for front cameras, we are
 * required to use {@link #RELATIVE_TO_SENSOR}.
 */
public enum Axis {

    /**
     * This rotation axis is the one going out of the device screen
     * towards the user's face.
     */
    ABSOLUTE,

    /**
     * This rotation axis takes into account the current
     * {@link com.otaliastudios.cameraview.controls.Facing} value.
     * <p>
     * - for {@link Facing#BACK}, this equals {@link #ABSOLUTE}
     * - for {@link Facing#FRONT}, this is inverted
     */
    RELATIVE_TO_SENSOR
}