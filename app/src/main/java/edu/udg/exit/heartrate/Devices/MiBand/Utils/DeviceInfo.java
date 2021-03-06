package edu.udg.exit.heartrate.Devices.MiBand.Utils;

/**
 * Class that stores device information.
 */
public class DeviceInfo {

    ////////////////
    // Attributes //
    ////////////////

    private String id;
    private Integer profileVersion;

    // Hardware and firmware
    private Integer firmwareVersion;
    private Integer hardwareVersion;
    private Integer feature;
    private Integer appearance;

    // Hearth rate monitoring
    private Integer firmwareVersion2;
    private boolean test1AHRMode = false;

    ////////////////////////
    // Life Cycle Methods //
    ////////////////////////

    /**
     * Constructor.
     * @param data - Device information stored as bytes
     */
    public DeviceInfo(byte[] data) {
        boolean checked = isChecksumCorrect(data);

        if((data.length == 16 || data.length == 20) && checked){
            id = String.format("%02X%02X%02X%02X%02X%02X%02X%02X", data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7]);
            profileVersion = Parse.BytesToInt(data,8,4);
            firmwareVersion = Parse.BytesToInt(data,12,4);
            hardwareVersion = data[6] & 255;
            appearance = data[5] & 255;
            feature = data[4] & 255;
        }else{
            id = null;
            profileVersion = null;
            firmwareVersion = null;
            hardwareVersion = null;
            feature = null;
            appearance = null;
        }

        if(data.length == 20 && checked){
            int s = 0;
            for (int i = 0; i < 4; ++i) {
                s |= (data[16 + i] & 255) << i * 8;
            }
            firmwareVersion2 = s;
        }else{
            firmwareVersion2 = null;
        }
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    /**
     * Checks if the information is correct.
     * @param data - Device information stored as bytes
     * @return True if the checksum is correct.
     */
    private boolean isChecksumCorrect(byte[] data) {
        int crc8 = CheckSums.getCRC8(new byte[]{data[0], data[1], data[2], data[3], data[4], data[5], data[6]});
        return (data[7] & 255) == (crc8 ^ data[3] & 255);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    /*---------*/
    /* Getters */
    /*---------*/

    /**
     * Gets the profile version of the device.
     * @return profileVersion
     */
    public Integer getProfileVersion() {
        return profileVersion;
    }

    /**
     * Gets the firmware version of the device.
     * @return firmwareVersion
     */
    public Integer getFirmwareVersion() {
        return firmwareVersion;
    }

    /**
     * Gets the firmware version for heart rate.
     * @return firmwareVersion or firmwareVersion2
     */
    public Integer getHeartRateFirmwareVersion() {
        if(test1AHRMode) return firmwareVersion;
        return firmwareVersion2;
    }

    /**
     * Gets the hardware version of the device.
     * @return hardwareVersion
     */
    public Integer getHardwareVersion() {
        return hardwareVersion;
    }

    /**
     * Gets the feature of the device.
     * @return feature
     */
    public Integer getFeature() {
        return feature;
    }

    /**
     * Gets the appearance of the device.
     * @return appearance
     */
    public Integer getAppearance() {
        return appearance;
    }

    /**
     * Checks device model.
     * @return True if the device is a Mi Band 1.
     */
    public boolean isMili1() {
        return hardwareVersion == 2 || (feature == 0 && appearance == 0 && hardwareVersion == 8 && firmwareVersion2 == -1);
    }

    /**
     * Checks device model.
     * @return True if the device is a Mi Band 1A.
     */
    public boolean isMili1A() {
        return feature == 5 && appearance == 0 || feature == 0 && hardwareVersion == 208;
    }

    /**
     * Checks device model.
     * @return True if the device is a Mi Band 1S
     * TODO - this is probably not quite correct, but sufficient for early 1S support
     */
    public boolean isMili1S() {
        return (feature == 4 && appearance == 0) || hardwareVersion == 4;
    }

    /**
     * Checks support for heart rate measurement.
     * @return True if device is 1S or 1A (in test mode).
     */
    public boolean supportsHeartRate() {
        return isMili1S() || (test1AHRMode && isMili1A());
    }

    @Override
    public String toString() {
        return "DeviceID: " + id + " | Feature: " + feature + " | Appearance: " + appearance +
        "\n Version: " + profileVersion + " / " + firmwareVersion + " / " + hardwareVersion;
    }
}
