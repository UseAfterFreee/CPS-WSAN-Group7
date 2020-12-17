package no.nordicsemi.android.nrfthingy;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import android.util.Log;

import static no.nordicsemi.android.nrfthingy.FFT.fft;

public class Clap {
    private int count_until_two_seconds;
    private int count_until_signal_full;
    private final int n_times_looped = 128*2;
    Complex[] Signal = new Complex[256* n_times_looped];
    public Clap(){
        count_until_two_seconds = 0;
        count_until_signal_full = 0;
    }
    public double calculate_max(byte[] data) {


        StringBuilder sb = new StringBuilder();

        int count = 0;
        String temp = "";
        int temp_decimal = 0;
        for (byte b : data) {
            if (count == 0) {
                count = 1;
                temp_decimal = b;
            } else {
                count = 0;
                int x = b;
                x <<= 8;
                temp_decimal |= x;


                if (count_until_signal_full < 256 * n_times_looped) {
                    Signal[count_until_signal_full] = new Complex(Double.valueOf(temp_decimal), 0);
                }
                count_until_signal_full = count_until_signal_full + 1;
            }
        }

        count_until_two_seconds = count_until_two_seconds + 1;

        if (count_until_two_seconds > n_times_looped) {

            Complex[] fft_Signal = fft(Signal);
            Complex[] fft_relevant = Arrays.copyOfRange(fft_Signal, 0, fft_Signal.length / 2);
            double[] fft_real = new double[256 * fft_relevant.length];
            for (int i = 0; i < fft_relevant.length; i++) {
                double temp_complex = fft_relevant[i].re();
                fft_real[i] = temp_complex;
                String temp_decimal_string = Double.toString(temp_complex);
                sb.append("," + temp_decimal_string);
            }


            count_until_two_seconds = 0;
            count_until_signal_full = 0;


            double max = 0;
            double sum = 0;
            double total = 0;
            int max_i = 0;
            for (int i = 100; i < fft_real.length; i++) {
                if (max < fft_real[i]) {
                    max = fft_real[i];
                    max_i = i;
                }
                sum = sum + fft_real[i];
                total = total + 1;

            }
            Log.d("Banter", Integer.toString(max_i));
            double mean = (sum / total);
            for (int i = 4000; i < 8000; i++) {
                if (fft_real[i] == max) {
                    Log.d("Banter", "you clapped");
                }
                else {
                    max = 0;
                }
            }


            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, "Signal_data.csv");
            FileOutputStream stream = null;
            Log.d("Banter", "start writing");


            try {
                stream = new FileOutputStream(file);
                stream.write(sb.toString().getBytes());
                stream.write("\n".getBytes());
                stream.write("\n".getBytes());
                stream.close();
                Log.d("Banter", "writing completed");

                //stream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                stream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return max;


        }

            return 0.0;

    }

}

