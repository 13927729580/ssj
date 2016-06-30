/*
 * AudioProvider.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
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

package hcm.ssj.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SensorProvider;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Audio Sensor - get data from audio interface and forwards it
 * Created by Johnny on 05.03.2015.
 */
public class AudioProvider extends SensorProvider
{
    public class Options extends OptionList
    {
        public final Option<Integer> sampleRate = new Option<>("sampleRate", 8000, Integer.class, "");
        public final Option<Integer> channelConfig = new Option<>("channelConfig", AudioFormat.CHANNEL_IN_MONO, Integer.class, "");
        public final Option<Integer> audioFormat = new Option<>("audioFormat", AudioFormat.ENCODING_PCM_16BIT, Integer.class, "orientation of input picture");
        public final Option<Boolean> scale = new Option<>("scale", false, Boolean.class, "");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }
    public final Options options = new Options();

    protected AudioRecord _recorder;

    byte[] _data = null;

    public AudioProvider()
    {

        _name = "SSJ_sensor_Microphone_Audio";
    }

    @Override
    public void enter(Stream stream_out)
    {
        //setup android audio middleware
        _recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, options.sampleRate.getValue(), options.channelConfig.getValue(), options.audioFormat.getValue(), stream_out.tot*10);

        int state = _recorder.getState();
        if(state != 1)
            Log.w("unexpected AudioRecord state = " + state);

        if(options.scale.getValue())
        {
            if(options.audioFormat.getValue() != AudioFormat.ENCODING_PCM_8BIT && options.audioFormat.getValue() != AudioFormat.ENCODING_PCM_16BIT)
                Log.e("unsupported audio format for normalization");

            int numBytes = Microphone.audioFormatSampleBytes(options.audioFormat.getValue());
            _data = new byte[stream_out.num * stream_out.dim * numBytes];
        }

        //startRecording has to be called as close to the first read as possible.
        _recorder.startRecording();
        Log.i("Audio capturing started");
    }

    @Override
    protected boolean process(Stream stream_out)
    {
        if(!options.scale.getValue())
        {
            //read data
            // this is blocking and thus defines the update rate
            switch (options.audioFormat.getValue())
            {
                case AudioFormat.ENCODING_PCM_8BIT:
                    _recorder.read(stream_out.ptrB(), 0, stream_out.num * stream_out.dim);
                    break;
                case AudioFormat.ENCODING_PCM_16BIT:
                case AudioFormat.ENCODING_DEFAULT:
                    _recorder.read(stream_out.ptrS(), 0, stream_out.num * stream_out.dim);
                    break;
                default:
                    Log.w("unsupported audio format");
                    return false;
            }
        }
        else
        {
            //read data
            // this is blocking and thus defines the update rate
            _recorder.read(_data, 0, _data.length);

            //normalize it and convert it to floats
            float[] outf = stream_out.ptrF();
            int i = 0, j = 0;
            while (i < _data.length)
            {
                switch (options.audioFormat.getValue())
                {
                    case AudioFormat.ENCODING_PCM_8BIT:
                        outf[j++] = _data[i++] / 128.0f;
                        break;
                    case AudioFormat.ENCODING_PCM_16BIT:
                    case AudioFormat.ENCODING_DEFAULT:
                        outf[j++] = (short) ((_data[i+1] & 0xFF) << 8 | (_data[i+0] & 0xFF)) / 32768.0f;
                        i += 2;
                        break;
                    default:
                        Log.w("unsupported audio format");
                        return false;
                }
            }
        }

        return true;
    }

    @Override
    public void flush(Stream stream_out)
    {
        _recorder.stop();
        _recorder.release();
    }

    @Override
    public int getSampleDimension()
    {
        switch(options.channelConfig.getValue())
        {
            case AudioFormat.CHANNEL_IN_MONO:
                return 1;

            case AudioFormat.CHANNEL_IN_STEREO:
                return 2;
        }

        return 0;
    }

    @Override
    public double getSampleRate()
    {
        return options.sampleRate.getValue();
    }

    @Override
    public int getSampleNumber()
    {
        int minBufSize = AudioRecord.getMinBufferSize(options.sampleRate.getValue(), options.channelConfig.getValue(), options.audioFormat.getValue());
        int bytesPerSample = Microphone.audioFormatSampleBytes(options.audioFormat.getValue());
        int dim = getSampleDimension();

        return minBufSize / (bytesPerSample * dim);
    }

    @Override
    public int getSampleBytes()
    {
       if(options.scale.getValue())
            return 4;
        else
            return Microphone.audioFormatSampleBytes(options.audioFormat.getValue());
    }

    @Override
    public Cons.Type getSampleType()
    {
        if(options.scale.getValue())
            return Cons.Type.FLOAT;
        else
            return Microphone.audioFormatSampleType(options.audioFormat.getValue());
    }

    @Override
    public void defineOutputClasses(Stream stream_out)
    {
        stream_out.dataclass = new String[1];
        stream_out.dataclass[0] = "Audio";
    }
}
