/*
 * BluetoothReader.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura
 * *****************************************************
 * This file is part of the Social Signal Interpretation for Java (SSJ) framework
 * developed at the Lab for Human Centered Multimedia of the University of Augsburg.
 *
 * SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a
 * one-to-one port of SSI to Java, it is an approximation. Nor does SSJ pretend
 * to offer SSI's comprehensive functionality and performance (this is java after all).
 * Nevertheless, SSJ borrows a lot of programming patterns from SSI.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package hcm.ssj.ioput;

import android.bluetooth.BluetoothDevice;

import java.io.IOException;

import hcm.ssj.core.Log;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Johnny on 05.03.2015.
 */
public class BluetoothReader extends Sensor {

    public class Options extends OptionList
    {
        public final Option<String> connectionName = new Option<>("connectionName", "SSJ", String.class, "must match that of the peer");
        public final Option<String> serverName = new Option<>("serverName", "SSJ_BLServer", String.class, "");
        public final Option<String> serverAddr = new Option<>("serverAddr", null, String.class, "if this is a client");
        public final Option<BluetoothConnection.Type> connectionType = new Option<>("connectionType", BluetoothConnection.Type.SERVER, BluetoothConnection.Type.class, "");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }

    public final Options options = new Options();

    protected BluetoothConnection _conn;

    public BluetoothReader() {
        _name = "SSJ_consumer_BluetoothReader";
    }

    @Override
    public void init()
    {
        try {
            switch(options.connectionType.get())
            {
                case SERVER:
                    _conn = new BluetoothServer(options.connectionName.get(), options.serverName.get());
                    break;
                case CLIENT:
                    _conn = new BluetoothClient(options.connectionName.get(), options.serverName.get(), options.serverAddr.get());
                    break;
            }

        } catch (IOException e) {
            Log.e("error in setting up connection "+ options.connectionName, e);
        }
    }

    @Override
    public boolean connect()
    {
        try {
            _conn.connect();
        } catch (IOException e) {
            Log.e("error connecting over "+ options.connectionName, e);
            return false;
        }

        BluetoothDevice dev = _conn.getConnectedDevice();
        Log.i("connected to " + dev.getName() + " @ " + dev.getAddress());
        return true;
    }

    @Override
    public void disconnect()
    {
        try {
            _conn.disconnect();
        } catch (IOException e) {
            Log.e("failed closing connection", e);
        }
    }

    @Override
    public void forcekill()
    {
        try {
            _conn.disconnect();

        } catch (Exception e) {
            Log.e("error force killing thread", e);
        }

        super.forcekill();
    }

    public BluetoothConnection getConnection()
    {
        return _conn;
    }
}