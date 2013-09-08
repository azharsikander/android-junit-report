package com.zutubi.android.junitreport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.util.Log;

public abstract class LogHandler implements Runnable
    {
    /**
     * @Author Tobias Gläser handles reading the log and notifies on when new line(s) added
     */

    public static final int LINES = 100;
    public static final int DELAY = 200;

    private ArrayList<String> bufferedlines = new ArrayList<String>();
    private String lastline = "null";
    private Thread t;
    private boolean exit = false;
    private static final String LOG_TAG = LogHandler.class.getSimpleName();

    public LogHandler()
        {
        t = new Thread(this);
        t.start();
        }

    public void stop()
        {
        exit = true;
        }

    public abstract void onLineAdd(String line);

    public void run()
        {
        while (!exit)
            {
            checkLogEvents();
            try
                {
                t.sleep(DELAY);
                }
            catch (InterruptedException e)
                {
                Log.e(LOG_TAG, safeMessage(e));
                }
            }
        }

    private void checkLogEvents()
        {
        try
            {
            ArrayList<String> chkl = new ArrayList<String>();
            // Executes the command.
            // -v time *:S $FilterForDeviceLog:V
            String[] cmd = { "logcat", "-d", "-vtime", "*:S", "SmokeTest:V", "-t", String.valueOf(LINES) };
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process
                            .getInputStream()), LINES * 2000);

            String output = "";
            while ((output = reader.readLine()) != null)
                {
                chkl.add(output);
                }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();

            int id = getLastSend(chkl);
            if (id == -1)
                {
                // resend complete
                if(chkl.size() < 1)
                    return ;
                bufferedlines = chkl;
                this.lastline = bufferedlines.get(bufferedlines.size() - 1);
                Log.i(LOG_TAG, "resend: "+this.lastline);
                for (String line : bufferedlines)
                    {
                    onLineAdd(line);
                    }
                }
            else
                {
                // compute resend part
                ArrayList<String> nl = new ArrayList<String>();
                if (id == 0)
                    return ;
                     
                for (int i = id+1; i < chkl.size(); i++)
                    {
                    nl.add(chkl.get(i));
                    }
                if (nl.size() < 1)
                    return;
                bufferedlines = nl;
                this.lastline = bufferedlines.get(bufferedlines.size() - 1);
                for (String line : bufferedlines)
                    {
                    onLineAdd(line);
                    }
                }

            }
        catch (IOException e)
            {
            Log.e(LOG_TAG, safeMessage(e));
            }
        catch (InterruptedException e)
            {
            Log.e(LOG_TAG, safeMessage(e));
            }
        }

    /**
     * gets index of last send line, -1 if not found
     * 
     * @return index of lastline
     */
    private int getLastSend(ArrayList<String> lines)
        {
        for (int i = 0; i < lines.size(); i++)
            {
            if (lines.get(i).equals(lastline))
                {
                Log.i(LOG_TAG, "found match: "+this.lastline);
                return i;
                }
            }
        return -1;
        }

    private String safeMessage(Throwable error)
        {
        String message = error.getMessage();
        return error.getClass().getName() + ": " + (message == null ? "<null>" : message);
        }
    }
