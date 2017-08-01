package com.ble.ionidea.sampleble;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 5/31/2017.
 */
public class ScannedDevicesAdapter extends RecyclerView.Adapter<ScannedDevicesAdapter.ViewHolder> {
    private final String TAG = "ScannedDevicesAdapter";
    private List<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    private Listener listener;
    private BluetoothDevice currentBluetoothDevice;
    private BleState currentState;

    public ScannedDevicesAdapter(Listener listener) {
        this.listener = listener;
    }

    public void clear() {
        bluetoothDevices.clear();
        currentBluetoothDevice = null;
        notifyDataSetChanged();
    }

    public void addBluetoothDevice(BluetoothDevice bluetoothDevice) {
        for (BluetoothDevice device : bluetoothDevices) {
            if (device.getAddress().equals(bluetoothDevice.getAddress())) {
                return;
            }
        }
        bluetoothDevices.add(bluetoothDevice);
        notifyDataSetChanged();
    }

    public void updateConnectionState(BluetoothDevice bluetoothDevice, BleState state) {
        if (bluetoothDevice != null) {
            Log.d(TAG, "updateConnectionState(). " + bluetoothDevice.getAddress() + " state: " + state.name());
        } else {
            Log.d(TAG, "updateConnectionState(). state: " + state.name());
        }
        this.currentBluetoothDevice = bluetoothDevice;
        this.currentState = state;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bluetooth_device, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final BluetoothDevice device = bluetoothDevices.get(position);
        holder.tvName.setText(device.getName());
        holder.tvAddress.setText(device.getAddress());
        holder.btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onConnectClicked(device);
            }
        });

        if (currentBluetoothDevice != null) {
            if (currentBluetoothDevice.getAddress().equals(device.getAddress())) {
                if (currentState.equals(BleState.CONNECTING)) {
                    holder.btnConnect.setEnabled(false);
                    holder.btnConnect.setText("Connecting...");
                } else if (currentState.equals(BleState.CONNECTED)) {
                    holder.btnConnect.setEnabled(true);
                    holder.btnConnect.setText("Disconnect");
                } else if (currentState.equals(BleState.DISCONNECTED)) {
                    holder.btnConnect.setEnabled(true);
                    holder.btnConnect.setText("Connect");
                }
            } else if (currentState == BleState.CONNECTING ||
                    currentState == BleState.CONNECTED) {
                holder.btnConnect.setEnabled(false);
            } else {
                holder.btnConnect.setEnabled(true);
            }
        } else {
            holder.btnConnect.setText("Connect");
            holder.btnConnect.setEnabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return bluetoothDevices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress;
        Button btnConnect;

        public ViewHolder(View view) {
            super(view);
            tvName = (TextView) view.findViewById(R.id.tv_name);
            tvAddress = (TextView) view.findViewById(R.id.tv_address);
            btnConnect = (Button) view.findViewById(R.id.btn_connect);
        }
    }

    public interface Listener {
        void onConnectClicked(BluetoothDevice bluetoothDeviceInfo);
    }
}
