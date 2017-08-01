package com.ble.ionidea.sampleble;

/**
 * Created by user on 7/31/2017.
 */
public class BleStateEvent {
    private BleState state;
    private String data;

    public BleStateEvent(BleState state) {
        this.state = state;
    }

    public BleStateEvent(BleState state, String data) {
        this.state = state;
        this.data = data;
    }

    public BleState getState() {
        return state;
    }

    public void setState(BleState state) {
        this.state = state;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
